/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.core.cluster.ClusterMessage;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.core.cluster.AckDirectiveMessage;
import org.cougaar.core.cluster.DirectiveMessage;
import org.cougaar.util.StringUtility;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Iterator;

public class MessageManagerImpl implements MessageManager, Serializable {

  public static final long serialVersionUID = -8662117243114391926L;

  private static final boolean debug =
    Boolean.getBoolean("org.cougaar.core.cluster.MessageManager.debug");

  private static final int MAXTRIES = 5;

  private static final long KEEP_ALIVE_INTERVAL = 55000L;

  private boolean USE_MESSAGE_MANAGER = false;

  /** The provider of our cluster services **/
  private transient ClusterServesLogicProvider myCluster;

  private transient String clusterNameForLog;

  /** Messages we need to send at the end of this epoch. **/
  private transient ArrayList stuffToSend = new ArrayList();

  /** Tracks the sequence numbers of other clusters **/
  private HashMap clusterInfo = new HashMap(13);

  /** Something has happened during this epoch. **/
  private transient boolean needAdvanceEpoch = false;

  /** The retransmitter thread **/
  private transient Retransmitter retransmitter;

  /** The acknowledgement sender thread **/
  private transient AcknowledgementSender ackSender;

  /** The keep alive sender thread **/
  private transient KeepAliveSender keepAliveSender;

  /** Debug logging **/
  private transient PrintWriter logWriter = null;

  /** The format of timestamps in the log **/
  private static DateFormat logTimeFormat =
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

  /**
   * Inner static class to track the state of communication with
   * another cluster.
   **/
  private class ClusterInfo implements java.io.Serializable {
    /** The ClusterIdentifier of the remote cluster **/
    private ClusterIdentifier clusterIdentifier;

    private long remoteIncarnationNumber = 0L;

    private long localIncarnationNumber = System.currentTimeMillis();

    private transient boolean restarted = false;

    public ClusterInfo(ClusterIdentifier cid) {
      clusterIdentifier = cid;
    }

    public ClusterIdentifier getClusterIdentifier() {
      return clusterIdentifier;
    }

    /**
     * The last sequence number we transmited to the cluster described
     * by this ClusterInfo
     **/
    private int currentTransmitSequenceNumber = 0;

    /**
     * The next sequence number we expect to receive from the cluster
     * described by this ClusterInfo
     **/
    private int currentReceiveSequenceNumber = 0;

    /**
     * The record of messages that have been acknowledged. We acknowledge the highest

    /**
     * The queue of messages that are outstanding.
     **/
    private TreeSet outstandingMessages = new TreeSet();

    public void addOutstandingMessage(TimestampedMessage tsm) {
      outstandingMessages.add(tsm);
      needAdvanceEpoch = true;
    }

    public TimestampedMessage[] getOutstandingMessages() {
      return (TimestampedMessage[]) outstandingMessages.toArray(new TimestampedMessage[outstandingMessages.size()]);
    }

    public synchronized TimestampedMessage getFirstOutstandingMessage() {
      if (outstandingMessages.isEmpty()) return null;
      return (TimestampedMessage) outstandingMessages.first();
    }

    /** Which messages have we actually processed (need acks) **/
    private AckSet ackSet = new AckSet(1);

    private boolean needSendAcknowledgement = false;

    private transient long transmissionTime = 0;

    public synchronized long getTransmissionTime() {
      return transmissionTime;
    }

    public synchronized void setTransmissionTime(long now) {
      transmissionTime = now;
    }

    public void acknowledgeMessage(ClusterMessage aMessage) {
      ackSet.set(aMessage.getContentsId());
      needAdvanceEpoch = true;
    }

    public boolean needSendAcknowledgement() {
      return needSendAcknowledgement;
    }

    public void setNeedSendAcknowledgment() {
      needSendAcknowledgement = true;
      ackSender.poke();
    }

    public boolean getRestarted() {
      return restarted;
    }

    public void setRestarted(boolean newRestarted) {
      restarted = newRestarted;
    }

    public void advance() {
      int oldMin = ackSet.getMinSequence();
      if (ackSet.advance() > oldMin) {
        needAdvanceEpoch = true; // State has change, need to persist
        setNeedSendAcknowledgment(); // Also need to send an ack
      }
    }

    /**
     * Create an acknowledgement for the current min sequence of the
     * ackset.
     **/
    public AckDirectiveMessage getAcknowledgement() {
      int firstZero = ackSet.getMinSequence();
      AckDirectiveMessage ack = new AckDirectiveMessage(getClusterIdentifier(),
                                                        myCluster.getClusterIdentifier(),
                                                        firstZero - 1,
                                                        remoteIncarnationNumber);
      needSendAcknowledgement = false;
      return ack;
    }

    public void receiveAck(MessageManagerImpl mm, int sequence, boolean isRestart) {
      long now = System.currentTimeMillis();
      for (Iterator messages = outstandingMessages.iterator(); messages.hasNext(); ) {
        TimestampedMessage tsm = (TimestampedMessage) messages.next();
        if (tsm.getSequenceNumber() <= sequence) {
          mm.printMessage("Remv", tsm);
          messages.remove();
        } else if (isRestart) {
          tsm.setTimestamp(now); // Retransmit this ASAP
        } else {
          break;                // Nothing left to do
        }
      }
    }

    /**
     * Check that the given message has the right sequence number.
     * @return a code indicating whether the message is old, current,
     * or future.
     **/
    public int checkReceiveSequenceNumber(DirectiveMessage aMessage) {
      int seq = aMessage.getContentsId();
      if (remoteIncarnationNumber == 0L) return FUTURE;
      if (seq <= currentReceiveSequenceNumber) return DUPLICATE;
      if (seq > currentReceiveSequenceNumber + 1) return FUTURE;
      return PRESENT;
    }

    public long getLocalIncarnationNumber() {
      return localIncarnationNumber;
    }

    public long getRemoteIncarnationNumber() {
      return remoteIncarnationNumber;
    }

    public void setRemoteIncarnationNumber(long incarnationNumber) {
      remoteIncarnationNumber = incarnationNumber;
    }

    public int getCurrentTransmitSequenceNumber() {
      return currentTransmitSequenceNumber;
    }

    public int getNextTransmitSequenceNumber() {
      return ++currentTransmitSequenceNumber;
    }

    public int getCurrentReceiveSequenceNumber() {
      return currentReceiveSequenceNumber;
    }

    /**
     * Update the current receive sequence number of this other cluster.
     * @param seqno the new current sequence number of this other
     * cluster.
     **/
    public void updateReceiveSequenceNumber(int seqno) {
      currentReceiveSequenceNumber = seqno;
      needAdvanceEpoch = true;  // Our state changed need to persist
    }

    public String toString() {
      return "ClusterInfo " + clusterIdentifier + " " +
        incarnationToString(localIncarnationNumber) + "->" +
        incarnationToString(remoteIncarnationNumber);
    }
  }

  /**
   * Tag a message on a retransmit queue with the time at which it
   * should next be sent.
   **/
  private class TimestampedMessage implements Comparable, java.io.Serializable {
    private transient long timestamp = System.currentTimeMillis();
    private transient int nTries = 0;

    protected transient DirectiveMessage theMessage;

    private ClusterIdentifier theDestination;
    private int theSequenceNumber;
    private long theIncarnationNumber;
    private Directive[] theDirectives;
    private ClusterInfo info;

    TimestampedMessage(ClusterInfo info, DirectiveMessage aMsg) {
      this.info = info;
      theMessage = aMsg;
      theDestination = aMsg.getDestination();
      theSequenceNumber = aMsg.getContentsId();
      theIncarnationNumber = aMsg.getIncarnationNumber();
      theDirectives = aMsg.getDirectives();
    }

    public void send(long now) {
      myCluster.sendMessage(getMessage());
      long nextRetransmission =
        now + retransmitSchedule[Math.min(nTries++, retransmitSchedule.length - 1)];
      setTimestamp(nextRetransmission);
    }

    /**
     * Get the DirectiveMessage. If theMessage is null, create a new
     * one. theMessage will be null only after rehydration of the
     * message manager.
     **/
    public DirectiveMessage getMessage() {
      if (theMessage == null) {
        theMessage = new DirectiveMessage(getSource(),
                                          getDestination(),
                                          theIncarnationNumber,
                                          getDirectives());
        theMessage.setContentsId(theSequenceNumber);
      }
      theMessage.setAllMessagesAcknowledged(info.getFirstOutstandingMessage() == this);
      return theMessage;
    }

    public ClusterIdentifier getDestination() {
      return theDestination;
    }

    public ClusterIdentifier getSource() {
      return myCluster.getClusterIdentifier();
    }

    public int getSequenceNumber() {
      return theSequenceNumber;
    }

    public long getIncarnationNumber() {
      return theIncarnationNumber;
    }

    public Directive[] getDirectives() {
      return theDirectives;
    }

    public void setTimestamp(long ts) {
      timestamp = ts;
    }

    public int compareTo(Object other) {
      TimestampedMessage otherMsg = (TimestampedMessage) other;
      return this.theSequenceNumber - otherMsg.theSequenceNumber;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("seq(");
      buf.append(theIncarnationNumber);
      buf.append("/");
      buf.append(theSequenceNumber);
      buf.append(") ");
      StringUtility.appendArray(buf, theDirectives);
      return buf.substring(0);
    }
  }

  private static long[] retransmitSchedule = {
    20000L, 20000L, 60000L, 120000L, 300000L
  };

  public MessageManagerImpl(boolean enable) {
    USE_MESSAGE_MANAGER = enable;
  }

  public void start(ClusterServesLogicProvider aCluster, boolean didRehydrate) {
    myCluster = aCluster;
    String clusterName = aCluster.getClusterIdentifier().getAddress();
    clusterNameForLog = "               ".substring(Math.min(14, clusterName.length())) + clusterName + " ";
    if (debug) {
      try {
        logWriter = new PrintWriter(new FileWriter("MessageManager_" +
                                                   aCluster.getClusterIdentifier().getAddress() +
                                                   ".log",
                                                   true || didRehydrate));
        printLog("MessageManager Started");
      }
      catch (IOException e) {
        System.err.println("Can't open MessageManager log file: " + e);
      }
    }

    if (USE_MESSAGE_MANAGER) {
      retransmitter = new Retransmitter(clusterName);
      retransmitter.start();
      ackSender = new AcknowledgementSender(clusterName);
      ackSender.start();
    }
    keepAliveSender = new KeepAliveSender(clusterName);
    keepAliveSender.start();
  }

  private synchronized void sendKeepAlive() {
    ArrayList messages = new ArrayList(clusterInfo.size());
    Directive[] directives = new Directive[0];
    long now = System.currentTimeMillis();
    for (Iterator clusters = clusterInfo.values().iterator(); clusters.hasNext(); ) {
      ClusterInfo info = (ClusterInfo) clusters.next();
      if (info.getFirstOutstandingMessage() == null) {
        if (now > info.getTransmissionTime() + KEEP_ALIVE_INTERVAL) {
          DirectiveMessage ndm =
            new DirectiveMessage(myCluster.getClusterIdentifier(),
                                 info.getClusterIdentifier(),
                                 info.getLocalIncarnationNumber(),
                                 directives);
          messages.add(ndm);
        }
      }
    }
    sendMessages(messages.iterator());
  }

  private void printMessage(String prefix, ClusterMessage aMessage) {
    if (aMessage instanceof DirectiveMessage) {
      printMessage(prefix, (DirectiveMessage) aMessage);
    } else if (aMessage instanceof AckDirectiveMessage) {
      printMessage(prefix, (AckDirectiveMessage) aMessage);
    }
  }

  private void printMessage(String prefix, DirectiveMessage aMessage) {
    printMessage(prefix,
                 aMessage.getIncarnationNumber(),
		 aMessage.getContentsId(),
		 aMessage.getSource().getAddress(),
		 aMessage.getDestination().getAddress(),
                 (aMessage.areAllMessagesAcknowledged() ? " yes" : " no") +
		 StringUtility.arrayToString(aMessage.getDirectives()));
  }

  private void printMessage(String prefix, AckDirectiveMessage aMessage) {
    printMessage(prefix,
                 aMessage.getIncarnationNumber(),
		 aMessage.getContentsId(),
		 aMessage.getSource().getAddress(),
		 aMessage.getDestination().getAddress(),
		 "");
  }

  private void printMessage(String prefix, TimestampedMessage tsm) {
    printMessage(prefix,
		 tsm.getIncarnationNumber(),
                 tsm.getSequenceNumber(),
		 tsm.getSource().getAddress(),
		 tsm.getDestination().getAddress(),
                 " ???" + StringUtility.arrayToString(tsm.getDirectives()));
  }

  private Date tDate = new Date();
  private SimpleDateFormat incarnationFormat =
    new SimpleDateFormat("yyyy/MM/dd/hh:mm:ss.SSS");

  private String incarnationToString(long l) {
    if (l == 0L) return "<none>";
    tDate.setTime(l);
    return incarnationFormat.format(tDate);
  }

  private void printMessage(String prefix, long incarnationNumber, int sequence,
                            String from, String to, String contents)
  {
    tDate.setTime(incarnationNumber);
    String msg = prefix + " " + sequence + " " + from + "->" + to + " (" + incarnationFormat.format(tDate) + "): " + contents;
//      System.out.println(msg);
    if (logWriter != null) {
      printLog(msg);
    }
  }

  private void printLog(String msg) {
    logWriter.print(logTimeFormat.format(new Date(System.currentTimeMillis())));
    logWriter.print(clusterNameForLog);
    logWriter.println(msg);
    logWriter.flush();
  }

  /**
   * Submit a DirectiveMessage for transmission from this cluster. The
   * message is added to the set of message to be transmitted at the
   * end of the current epoch.
   **/
  public void sendMessages(Iterator messages) {
    if (USE_MESSAGE_MANAGER) {
      synchronized (this) {
        while (messages.hasNext()) {
          DirectiveMessage aMessage = (DirectiveMessage) messages.next();
          ClusterInfo info = getClusterInfo(aMessage.getDestination());
          if (info == null) {
            if (debug) printLog("sendMessage createNewConnection");
            info = createNewConnection(aMessage.getDestination(), 0L);
          }
          aMessage.setIncarnationNumber(info.getLocalIncarnationNumber());
          aMessage.setContentsId(info.getNextTransmitSequenceNumber());
          stuffToSend.add(new TimestampedMessage(info, aMessage));
          if (debug) printMessage("QSnd", aMessage);
        }
        needAdvanceEpoch = true;
      }
    } else {
      while (messages.hasNext()) {
        myCluster.sendMessage((DirectiveMessage) messages.next());
      }
    }
  }

  private Directive[] emptyDirectives = new Directive[0];

  private void sendInitializeMessage(ClusterInfo info) {
    DirectiveMessage msg = new DirectiveMessage(myCluster.getClusterIdentifier(),
                                                info.getClusterIdentifier(),
                                                info.getLocalIncarnationNumber(),
                                                emptyDirectives);
    msg.setContentsId(info.getNextTransmitSequenceNumber());
    stuffToSend.add(new TimestampedMessage(info, msg));
    if (debug) printMessage("QSnd", msg);
    needAdvanceEpoch = true;
  }

  private ClusterInfo getClusterInfo(ClusterIdentifier clusterIdentifier) {
    return (ClusterInfo) clusterInfo.get(clusterIdentifier);
  }

  private ClusterInfo createClusterInfo(ClusterIdentifier clusterIdentifier) {
    ClusterInfo info = new ClusterInfo(clusterIdentifier);
    clusterInfo.put(clusterIdentifier, info);
    return info;
  }

  /**
   * Check a received DirectiveMessage for being a duplicate.
   * @param aMessage The received DirectiveMessage
   * @return DUPLICATE, FUTURE, RESTART, IGNORE, or OK
   **/
  public int receiveMessage(DirectiveMessage directiveMessage) {
    if (!USE_MESSAGE_MANAGER) return OK;
    synchronized (this) {
      boolean restarted = false;
      ClusterIdentifier sourceIdentifier = directiveMessage.getSource();
      ClusterInfo info = getClusterInfo(sourceIdentifier);
      boolean isFirst = directiveMessage.getContentsId() == 1;
      if (info != null) {
        if (info.getRestarted()) {
          restarted = true;
          info.setRestarted(false);
        }
        long infoIncarnation = info.getRemoteIncarnationNumber();
        long messageIncarnation = directiveMessage.getIncarnationNumber();
        if (infoIncarnation != messageIncarnation) {
          if (infoIncarnation == 0L) {
            if (isFirst) {
              info.setRemoteIncarnationNumber(messageIncarnation);
            } else {
              if (debug) printMessage("Nnz1", directiveMessage);
              info.setNeedSendAcknowledgment();
              return restarted ? (IGNORE | RESTART) : IGNORE;      // Stray message
            }
          } else if (messageIncarnation < infoIncarnation) {
            if (debug) printMessage("Prev", directiveMessage);
            info.setNeedSendAcknowledgment();
            return restarted ? (IGNORE | RESTART) : IGNORE; // Message from previous incarnation of remote cluster
          } else if (messageIncarnation > infoIncarnation) {
                                // Message from new incarnation
            if (isFirst) {      // Synchronize to new incarnation
              if (debug) printLog("receiveMessage messageIncarnation > infoIncarnation");
              info = createNewConnection(sourceIdentifier, directiveMessage.getIncarnationNumber());
              restarted = true;
            } else {
              if (debug) printMessage("Nnz2", directiveMessage);
              info.setNeedSendAcknowledgment();
              return restarted ? (IGNORE | RESTART) : IGNORE; // Apparently new incarnation, but not sequence 0
            }
          }
        }
      } else {
        if (isFirst) {
          if (debug) printLog("receiveMessage null info is first");
          info = createNewConnection(sourceIdentifier, directiveMessage.getIncarnationNumber());
        } else {
          if (debug) printMessage("receiveMessage null info not first", directiveMessage);
          info = createNewConnection(sourceIdentifier, 0L);
          return IGNORE; // Must have sequence zero to synchronize
        }
      }
      switch (info.checkReceiveSequenceNumber(directiveMessage)) {
      case DUPLICATE:
        if (debug) printMessage("Dupl", directiveMessage);
        info.setNeedSendAcknowledgment();
        return IGNORE;
      default:
      case FUTURE:
        if (directiveMessage.areAllMessagesAcknowledged()) {
          // We are out of sync
          if (debug) printLog("receiveMessage from future all acked");
          info = createNewConnection(sourceIdentifier, 0L);
          return IGNORE | RESTART;
        }
        if (debug) printMessage("Futr", directiveMessage);
        return IGNORE;        // Message out of order; ignore it
      case OK:
        if (debug) printMessage("Rcvd", directiveMessage);
        info.updateReceiveSequenceNumber(directiveMessage.getContentsId());
        needAdvanceEpoch = true;
        return restarted ? (RESTART | OK) : OK;
      }
    }
  }

  private ClusterInfo createNewConnection(ClusterIdentifier sourceIdentifier, long remoteIncarnationNumber) {
    ClusterInfo info = createClusterInfo(sourceIdentifier); // New connection
    info.setRemoteIncarnationNumber(remoteIncarnationNumber);
    if (debug) printLog("New Connection: " + info.toString());
    sendInitializeMessage(info);
    return info;
  }

  public void acknowledgeMessages(Iterator messages) {
    if (!USE_MESSAGE_MANAGER) return;
    synchronized (this) {
      while (messages.hasNext()) {
        DirectiveMessage aMessage = (DirectiveMessage) messages.next();
        if (aMessage.getContentsId() == 0) return; // Not reliably sent
        ClusterInfo info = getClusterInfo(aMessage.getSource());
        info.acknowledgeMessage(aMessage);
        needAdvanceEpoch = true;
        if (debug) printMessage("QAck", aMessage);
      }
    }
  }

  private void resetAllConnections() {
    clusterInfo.clear();
    printLog("MessageManager connections clearer");
  }

  /**
   * Process a directive acknowledgement. The acknowledged messages
   * are removed from the retransmission queues. If the ack is marked
   * as having been sent during a cluster restart, we speed up the
   * retransmission process to hasten the recovery process.
   **/
  public int receiveAck(AckDirectiveMessage theAck) {
    synchronized (this) {
      if (debug) printMessage("RAck", theAck);
      ClusterInfo info = getClusterInfo(theAck.getSource());
      if (info != null) {
        boolean restarted = false;;
        if (info.getRestarted()) {
          info.setRestarted(false);
          restarted = true;
        }
        long localIncarnationNumber = info.getLocalIncarnationNumber();
        long ackIncarnationNumber = theAck.getIncarnationNumber();
        if (localIncarnationNumber == ackIncarnationNumber) {
          int seq = theAck.getContentsId();
          if (info.getCurrentTransmitSequenceNumber() < seq) {
            if (debug) printLog("receiveAck from future same incarnation");
            createNewConnection(info.getClusterIdentifier(), 0L);
            return RESTART;
          }
          info.receiveAck(this, seq, false);
          return restarted ? (RESTART | OK) : OK;
        } else if (localIncarnationNumber < ackIncarnationNumber) {
          // We are living in the past. We must have rehydrated with
          // an old set of connections.
          if (debug) printLog("receiveAck from future incarnation");
          createNewConnection(info.getClusterIdentifier(), 0L);
          return RESTART;
        } else {
          // The other end is living in the past. Hopefully, he will
          // eventually get with the program.
          if (debug) printLog("receiveAck from past incarnation");
          return restarted ? (IGNORE | RESTART) : IGNORE;
        }
      } else {
        return IGNORE;
      }
    }
  }

  /**
   * Determine if anything has happened during this epoch.
   * @return true if anything has changed.
   **/
  public boolean needAdvanceEpoch() {
    return needAdvanceEpoch;
  }

  /**
   * Wrap up the current epoch and get into the correct state to be
   * persisted. Every message that has been queued for transmission is
   * sent. Acknowledgement numbers are advanced so we begin
   * acknowledging messages we have received and processed. This
   * method must be called while this MessageManager is
   * synchronized. We purposely omit the "synchronized" here because
   * proper operation is precluded unless the synchronization is
   * performed externally.
   **/
  public void advanceEpoch() {
    // Advance the information about every other cluster
    for (Iterator clusters = clusterInfo.values().iterator(); clusters.hasNext(); ) {
      ClusterInfo info = (ClusterInfo) clusters.next();
      info.advance();
    }
    needAdvanceEpoch = false;
    long now = System.currentTimeMillis();
    for (Iterator iter = stuffToSend.iterator(); iter.hasNext(); ) {
      TimestampedMessage tsm = (TimestampedMessage) iter.next();
      getClusterInfo(tsm.getDestination()).addOutstandingMessage(tsm);
      retransmitter.poke();
    }
    stuffToSend.clear();
    if (logWriter != null) {
      printLog("Advanced epoch");
    }
  }

  private class KeepAliveSender extends Thread {
    public KeepAliveSender(String clusterName) {
      super("Keep Alive Sender/" + clusterName);
    }
    public void run() {
      while (true) {
        sendKeepAlive();
        try {
          sleep(KEEP_ALIVE_INTERVAL);
        }
        catch (InterruptedException ie) {
        }
      }
    }
  }

  private class AcknowledgementSender extends Thread {
    private boolean poked = false;
    ArrayList acksToSend = new ArrayList();

    public AcknowledgementSender(String clusterName) {
      super("Ack Sender/" + clusterName);
    }

    public synchronized void poke() {
      poked = true;
      AcknowledgementSender.this.notify();
    }

    public void run() {
      while (true) {
        synchronized (AcknowledgementSender.this) {
          while (!poked) {
            try {
              AcknowledgementSender.this.wait();
            }
            catch (InterruptedException ie) {
            }
          }
          poked = false;
        }
        synchronized (MessageManagerImpl.this) {
          for (Iterator clusters = clusterInfo.values().iterator(); clusters.hasNext(); ) {
            ClusterInfo info = (ClusterInfo) clusters.next();
            if (info.needSendAcknowledgement()) {
              acksToSend.add(info.getAcknowledgement());
            }
          }
        }
        for (Iterator iter = acksToSend.iterator(); iter.hasNext(); ) {
          AckDirectiveMessage ack = (AckDirectiveMessage) iter.next();
          if (debug) printMessage("SAck", ack);
          myCluster.sendMessage(ack);
        }
        acksToSend.clear();
      }
    }
  }

  private class Retransmitter extends Thread {
    private boolean poked = false;
    private ArrayList messagesToRetransmit = new ArrayList();

    public Retransmitter(String clusterName) {
      super(clusterName + "/Message Manager");
    }

    public synchronized void poke() {
      poked = true;
      Retransmitter.this.notify();
    }

    /**
     * Retransmit messages that have not been acknowledged. Iterate
     * through all the clusters for which we have ClusterInfo and
     * interate through all the outstandmessage that have been sent to
     * that cluster. Check the time to retransmit of the message and if
     * the current time has passed that time, then retransmit the
     * message. Keep the earliest time of any message that is not ready
     * to be retransmitted and sleep long enough so that there could be
     * at least one message to retransmit when we awaken.
     **/
    public void run() {
      while (true) {
        try {
          long now = System.currentTimeMillis();
          long earliestTime = now + retransmitSchedule[0];
          synchronized (MessageManagerImpl.this) {
            for (Iterator clusters = clusterInfo.values().iterator();
                 clusters.hasNext(); )
              {
                ClusterInfo info = (ClusterInfo) clusters.next();
                TimestampedMessage tsm = info.getFirstOutstandingMessage();
                if (tsm == null) continue;
                if (tsm.timestamp <= now) {
                  TimestampedMessage[] messages = info.getOutstandingMessages();
                  info.setTransmissionTime(now);
                  messagesToRetransmit.addAll(java.util.Arrays.asList(messages));
                } else if (tsm.timestamp < earliestTime) {
                  earliestTime = tsm.timestamp;
                }
              }
          }
          if (!messagesToRetransmit.isEmpty()) {
            for (Iterator iter = messagesToRetransmit.iterator(); iter.hasNext(); ) {
              TimestampedMessage tsm = (TimestampedMessage) iter.next();
              tsm.send(now);
              if (tsm.timestamp < earliestTime) {
                earliestTime = tsm.timestamp;
              }
              if (debug) printMessage(tsm.nTries == 1 ? "Send" : ("Rxm" + tsm.nTries), tsm);
            }
            messagesToRetransmit.clear();
          }
          synchronized (Retransmitter.this) {
            if (!poked) {
              long sleepTime = 5000L + earliestTime - now;
              if (sleepTime > 30000L) sleepTime = 30000L;
              Retransmitter.this.wait(sleepTime);
            }
            poked = false;
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /** Serialize ourselves. Used for persistence. **/
  private void writeObject(ObjectOutputStream os) throws IOException {
    synchronized (this) {
      if (stuffToSend.size() > 0) {
        throw new IOException("Non-empty stuffToSend");
      }
      os.defaultWriteObject();
    }
  }

  private void readObject(ObjectInputStream is)
    throws IOException, ClassNotFoundException
  {
    is.defaultReadObject();
    stuffToSend = new ArrayList();
    needAdvanceEpoch = false;
//      for (Iterator clusters = clusterInfo.values().iterator(); clusters.hasNext(); ) {
//        ClusterInfo info = (ClusterInfo) clusters.next();
//        info.setRestarted(true);
//      }
  }
}

/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import java.util.*;
import org.cougaar.util.CircularQueue;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageStatistics;
import org.cougaar.core.society.MulticastMessageAddress;
import org.cougaar.core.society.MessageTransportServer;
import org.cougaar.core.society.MessageTransportClient;
import org.cougaar.core.society.MessageTransportWatcher;
import org.cougaar.core.society.MessageSecurityManager;
import org.cougaar.core.society.SecureMessage;

import java.beans.Beans;        // used to instantiate MSM

import org.cougaar.core.cluster.ClusterServesClusterManagement;

import org.cougaar.core.cluster.ClusterContext;
import java.io.*;

/**
 * Base class for implementations of Message Transport systems.
 * All members of a society must be using the same MessageTransport class.
 *
 * Logging points are:
 *  outQ+	message added to outbound queue
 *  outQ-	message removed from outbound queue
 *  inQ+	message added to incoming queue
 *  inQ-	message removed from incoming queue
 *  deliv	message (successfully) delivered to cluster
 *  delivFail	message delivered to cluster but generated exception
 *
 * subclasses may add additional logging points
 *
 * all Q lines include "(queuelength)" at end.
 *
 *
 * System Properties:
 * org.cougaar.message.isLogging=false : turn on Message logging detail (cml files).
 * org.cougaar.message.security=null : if non-null, specifies the MessageSecurityManager
 *   to use for incoming and outgoing messages.  Only one may be specified, and
 *   it had better match the MSMs of other MessageTransports.  Note that
 *   different MessageTransports may or may not make use of the MSM
 *   depending on policy.
 * 
 **/

public abstract class MessageTransport implements MessageTransportServer
{
  /** are we logging? **/
  public static boolean isLogging = false;

  /** are we collecting statistics on msms? **/
  protected static boolean statMSM = false;

  protected static String msmName = null;

  /** holder for our MSM instance, if specified **/ 
  private static MessageSecurityManager msm = null; 

  // initialize static vars from system properties.
  static {
    Properties props = System.getProperties();
    isLogging = (Boolean.valueOf(props.getProperty("org.cougaar.message.isLogging", "false"))).booleanValue();
    msmName = props.getProperty("org.cougaar.message.security");
    // "org.cougaar.core.society.SignedMessageSecurityManager
    statMSM = (Boolean.valueOf(props.getProperty("org.cougaar.message.msmStats", "false"))).booleanValue();
  }

  static double secureTotal = 0;
  static double secureIter = 0;
  static double unsecTotal = 0;
  static double unsecIter = 0;

  /** reference to the (local) cluster(s) we are serving.
   * Type is MessageTransportClient
   **/
  protected HashMap myClients = new HashMap(89);

  protected void addLocalClient(MessageTransportClient client) {
    synchronized (myClients) {
      try {
        myClients.put(client.getMessageAddress(), client);
      } catch(Exception e) {}
    }
  }
  protected void removeLocalClient(MessageTransportClient client) {
    synchronized (myClients) {
      try {
        myClients.remove(client.getMessageAddress());
      } catch (Exception e) {}
    }
  }

  protected MessageTransportClient findLocalClient(MessageAddress id) {
    synchronized (myClients) {
      return (MessageTransportClient) myClients.get(id);
    }
  }

  // this is a slow implementation, as it conses a new set each time.
  // Better alternatives surely exist.
  protected Iterator findLocalMulticastClients(MulticastMessageAddress addr) {
    synchronized (myClients) {
      return new ArrayList(myClients.values()).iterator();
    }
  }

  /** outbound message queue */
  private CircularQueue outQ = new CircularQueue();

  /** secure a message iff we have a MSM **/
  protected Message secure(Message m) {
    if (msm != null) {
      long t = 0;
      if (statMSM) t = System.currentTimeMillis();
      Message sm = msm.secureMessage(m);
      if (statMSM) {
        t = System.currentTimeMillis()-t;
        if (secureTotal==0.0) System.err.println("First secure = "+t);
        secureTotal = secureTotal+t;
        secureIter++;
      }
      return sm;
    } else {
      return m;
    }
  }
  /** unsecure a message if it needs it **/
  protected Message unsecure(Message m) {
    if (m instanceof SecureMessage) {
      if (msm == null) {
        System.err.println("MessageTransport "+this+" received SecureMessage "+m+
                           " but has no MessageSecurityManager.");
        m = null;
      } else {
        long t =0;
        if (statMSM) t = System.currentTimeMillis();

        Message n = msm.unsecureMessage((SecureMessage) m);
        if (n == null) {
          System.err.println("MessageTransport "+this+" received an unverifiable SecureMessage "+m);
        }
        m = n;

        if (statMSM) {
          t = System.currentTimeMillis()-t;
          if (unsecTotal==0.0) System.err.println("First unsecure = "+t);
          unsecTotal += t;
          unsecIter++;
        }
      }
    }
    return m;
  }

  /** send a message somewhere - message has destination as an ivar 
   * Implements MessageTransportServer
   **/
  public void sendMessage(Message m) {
    int osize = 0;

    if (checkMessage(m)) {
      synchronized (outQ) {
        outQ.add(m);
        outQ.notify();
        osize = outQ.size();
      }

      if (isLogging) log("outQ+", m.toString()+" ("+osize+")");

      watchingOutgoing(m);
    } else {
      System.err.println("Warning: MessageTransport.sendMessage of malformed message: "+m);
      Thread.dumpStack();
      return;
    }
  }

  /** @return true IFF the message appears to be well-formed. **/
  protected boolean checkMessage(Message m) {
    MessageAddress t = m.getTarget();
    // reasonable target?
    if (t == null || t.toString().equals(""))
      return false;

    // looks ok to me
    return true;
  }

  // (OutQ dispatch thread)

  /** deal with any outgoing messages as needed */
  private void dispatchOutgoingMessages() {
    while (true) {
      Message m;
      int osize;
      // wait for a message to handle
      synchronized (outQ) {
        while (outQ.isEmpty()) {
          try {
            outQ.wait();
          } catch (InterruptedException e) {} // dont care
        }
        
	m = (Message) outQ.next(); // from top
        osize = outQ.size();
      }

      if (m != null) {
        if (isLogging) log("outQ-", m.toString()+" ("+osize+")");
        sendMessageToSociety(m);
      }
    }
  }

  /** entry point for subclasses **/
  protected abstract void sendMessageToSociety(Message m);

  /** Incoming message queue */
  private CircularQueue inQ = new CircularQueue();

  // runs in rpc/dispatch thread

  /** Add incoming messages to the incoming queue.
   * called by network transport in its own thread.
   * This is the one method which needs to be exposed to the network transport.
   * @return the current length of our (incoming) message queue.
   */
  public void receiveMessage(Message m) {
    if (isLogging) log("inQ0", (m.toString()));
    int isize;
    
    synchronized (inQ) {
      inQ.add(m);
      inQ.notify();
      isize = inQ.size(); 
    }
    
    if (isLogging) log("inQ+", (m.toString())+"("+isize+")");
    
    watchingIncoming(m);
  }

  // (InQ message dispatch thread)

  /** Deal with (queued) incoming messages, delivering them to 
   * our cluster mechanisms serially (but without keeping the
   * queue locked.)
   **/
  private void dispatchIncomingMessages() {
    while (true) {
      Message m;
      int isize;
      synchronized (inQ) {
	// wait for a message to handle
	while (inQ.isEmpty()) {
	  try {
	    inQ.wait();
	    //Thread.sleep(100);	// sleep instead
	  } catch (InterruptedException e) {
	    // we don't care if we are interrupted - just continue
	    // to cycle.
	  } 
	}
      
	m = (Message) inQ.next();
        isize = inQ.size();
      }
      if (isLogging) log("inQ-", (m.toString())+"("+isize+")");
      // deliver outside synchronization so that we don't block the queue
      sendMessageToClient(m);
    }
  }

  /** stub method to deliver a received message to our cluster.
   **/
  private void sendMessageToClient(Message m) {
    if (m != null) {
      try {
        MessageAddress addr = m.getTarget();
        if (addr instanceof MulticastMessageAddress) {
          Iterator i = findLocalMulticastClients((MulticastMessageAddress)addr); 
          while (i.hasNext()) {
            MessageTransportClient client = (MessageTransportClient) i.next();
            client.receiveMessage(m);
            if (isLogging) log("mdeliv", m.toString());
          }
        } else {
          MessageTransportClient client = findLocalClient(addr);
          if (client != null) {
            client.receiveMessage(m);
            if (isLogging) log("deliv", "to "+client+": "+m);
          } else {
            throw new RuntimeException("Misdelivered message "+m+" sent to "+this);
          }
        }
      } catch (Exception e) {
	e.printStackTrace();
	if (isLogging) log("delivFail", (m.toString())+" Exception:"+e);
      }
    } else {
      System.err.println("MessageTransport Received non-Message "+m);
    }
  }


  /** the thread running dispatchIncomingMessages() */
  private Thread inQThread;

  /** the thread running dispatchOutgoingMessages() */
  private Thread outQThread;

  private String myId;
  public MessageTransport(String id) {
    myId = id;
    
    if (msmName != null && (! msmName.equals("")) && (! msmName.equals("none"))) {
      try {
        msm = (MessageSecurityManager) Beans.instantiate(null,msmName);
        msm.setMessageTransport(this);
      } catch (Exception e) {
        System.err.println("Problem instantiating MessageSecurityManager \""+
                           msmName+"\":\n\t"+e);
        e.printStackTrace();
      }
    }

    if (statMSM)
      startHeartbeat();
  }

    
  /** Start the dispatch threads and connect to the FDS registry
   **/
  public void start() {
    if (isLogging)
      getLog();

    inQThread = new Thread(new Runnable() {
      public void run() {
	dispatchIncomingMessages();
      }
    }, myId+"/InQ");
    inQThread.start();

    outQThread = new Thread(new Runnable() {
      public void run() {
	dispatchOutgoingMessages();
      }
    }, myId+"/OutQ");
    outQThread.start();


    //System.err.println("Started MessageTransport for "+c);
  }

  // must be started
  public void registerClient(MessageTransportClient client) {
    addLocalClient(client);

    registerClientWithSociety(client);
  }

  /** @return true IFF the MessageAddress is known to the nameserver **/
  public abstract boolean addressKnown(MessageAddress a);

  /** Register a Client with the society.
   * the default implementation does nothing, but MessageTransports to be used
   * by real, distributed societies will require some sort of name service.
   **/
  protected void registerClientWithSociety(MessageTransportClient client) {
  }

  // logging support
  private PrintWriter logStream = null;

  private PrintWriter getLog() {
    if (logStream == null) {
      try {
	logStream = new PrintWriter(new FileWriter(getIdentifier()+".cml"), true);
      } catch (Exception e) {
	e.printStackTrace();
	System.err.println("Logging required but not possible - exiting");
	System.exit(1);
      }
    }
    return logStream;
  }

  protected void log(String key, String info) {
    getLog().println(myId+"\t"+System.currentTimeMillis()+"\t"+key+"\t"+info);
  }

  //
  public String getIdentifier() {
    return myId;
  }


  private Vector watchers = new Vector();

  public void addMessageTransportWatcher(MessageTransportWatcher watcher) {
    watchers.addElement(watcher);
  }

  protected void watchingIncoming(Message m) {
    if (watchers.size() == 0) return;
    for (Enumeration e = watchers.elements() ; e.hasMoreElements(); ) {
      ((MessageTransportWatcher)e.nextElement()).messageReceived(m);
    }
  }
  protected void watchingOutgoing(Message m) {
    if (watchers.size() == 0) return;
    for (Enumeration e = watchers.elements() ; e.hasMoreElements(); ) {
      ((MessageTransportWatcher)e.nextElement()).messageSent(m);
    }
  }

  private boolean disableRetransmission = false;

  public void setDisableRetransmission(boolean disableRetransmission) {
    this.disableRetransmission = disableRetransmission;
  }

  public boolean isDisableRetransmission() {
    return disableRetransmission;
  }

  public abstract NameServer getNameServer();

  //
  // specialized message transport serialization layer
  //

  /**
   * Accumulate total bytes sent using ObjectOutputStream. This
   * variable should be accessed only while synchronized on this
   * MessageTransport object.
   **/
  protected long statisticsTotalBytes = 0L;
  protected long[] messageLengthHistogram = null;

  /** Keep statistics if true **/
  public static boolean keepStatistics =
    (Boolean.valueOf(System.getProperty("org.cougaar.message.keepStatistics", "false"))).booleanValue();

  public class StatisticsStreamWrapper extends OutputStream {
    OutputStream wrapped;
    int byteCount = 0;
    public StatisticsStreamWrapper(OutputStream wrapped) {
      this.wrapped = wrapped;
      if (messageLengthHistogram == null) {
        messageLengthHistogram = new long[MessageStatistics.NBINS];
      }
    }
    public void close() throws IOException {
      wrapped.close();
    }
    public void flush() throws IOException {
      wrapped.flush();
      int bin = 0;
      int maxBin = MessageStatistics.NBINS - 1;
      while (bin < maxBin && byteCount >= MessageStatistics.BIN_SIZES[bin]) {
        bin++;
      }
      messageLengthHistogram[bin]++;
      statisticsTotalBytes += byteCount;
      byteCount = 0;
    }
    public void write(int b) throws IOException {
      wrapped.write(b);
      synchronized (MessageTransport.this) {
        byteCount += 1;
      }
    }
    public void write(byte[] b) throws IOException {
      wrapped.write(b);
      synchronized (MessageTransport.this) {
        byteCount += b.length;
      }
    }
    public void write(byte[] b, int o, int len) throws IOException {
      wrapped.write(b, o, len);
      synchronized (MessageTransport.this) {
        byteCount += len;
      }
    }
  }

  /** Heartbeat to print statistics on 
   **/
  private class Heartbeat implements Runnable {
    public void run() {
      while (true) {
        try {
          Thread.sleep(5*1000); // sleep for (at least) 5s
        } catch (InterruptedException ie) {}

        double secMean = secureTotal;
        if (secureIter>0) secMean/=secureIter;

        double unsecMean = unsecTotal;
        if (unsecIter>0) unsecMean/=unsecIter;

        System.err.println("{"+secMean+" "+unsecMean+"}");
      }
    }
  }

  private static Thread heartbeat = null;
  private static Object heartbeatLock = new Object();

  private void startHeartbeat() {
    synchronized (heartbeatLock) {
      if (heartbeat == null) {
        heartbeat = new Thread(new Heartbeat(), "Heartbeat");
        heartbeat.setPriority(Thread.MIN_PRIORITY);
        heartbeat.start();
      }
    }
  }
}

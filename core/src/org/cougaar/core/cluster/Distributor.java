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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.cougaar.core.cluster.persist.Persistence;
import org.cougaar.core.cluster.persist.PersistenceSubscriberState;

public class Distributor {
  /*
   * Synchronization methodology. The distributor distributes three
   * kinds of things: envelopes, messages, and timers. All three are
   * synchronized on the distributor object (this) so only one of
   * these can be in progress at a time. The distributor must have
   * unfettered access to the subscribers meaning that the subscribers
   * cannot themselves by locked while awaiting access to the
   * distributor. Each distribution may generate a persistence
   * delta. Persistence deltas are not generated unless there are no
   * open transactions. Normally, subscribers are allowed to open
   * transactions except if sufficient time has elapsed since the
   * previous persistence delta requiring that a persistence delta
   * must be generated. A persistence delta must also be generated if
   * there are no open transactions and nothing has been distributed
   * to any subscriber.
   **/

  /** The maximum interval between persistence deltas. **/
  private static final long MAX_PERSIST_INTERVAL = 37000L;

  /** The object we use to persist ourselves. */
  private Persistence persistence = null;

  /** The time that we last persisted **/
  private long lastPersist = System.currentTimeMillis();

  /** Do we need to persist sometime; changed state has not been persisted **/
  private boolean needToPersist = false;

  /** All objects published prior to the last rehydrated delta **/
  private PersistenceEnvelope rehydrationEnvelope = null;

  /** Envelopes distributed since the last rehydrated delta **/
  private List postRehydrationEnvelopes = null;

  /** True if rehydration occurred at startup **/
  private boolean didRehydrate = false;

  /** Envelopes that have been distributed during a persistence epoch. **/
  private List epochEnvelopes = new ArrayList();

  /** The message manager for this cluster **/
  private MessageManager myMessageManager = null;

  /** Debug logging **/
  private transient PrintWriter logWriter = null;

  public PublishHistory history =
    System.getProperty("org.cougaar.core.cluster.keepPublishHistory", "false").equals("true")
      ? new PublishHistory()
      : null;

  /** The format of timestamps in the log **/
  private static DateFormat logTimeFormat =
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS ");

  /** Our set of (all) registered Subscribers **/
  private static class Subscribers {
    private List subscribers = new ArrayList();
    ReferenceQueue refQ = new ReferenceQueue();

    public void register(Subscriber subscriber) {
      checkRefQ();
      subscribers.add(new WeakReference(subscriber, refQ));
    }
    public void unregister(Subscriber subscriber) {
      for (Iterator iter = subscribers.iterator(); iter.hasNext(); ) {
        WeakReference ref = (WeakReference) iter.next();
        if (ref.get() == subscriber) {
          iter.remove();
        }
      }
    }

    public Iterator iterator() {
      checkRefQ();
      final Iterator iter = subscribers.iterator();
      return new Iterator() {
        public boolean hasNext() {
          return iter.hasNext();
        }
        public Object next() {
          WeakReference ref = (WeakReference) iter.next();
          return ref.get();
        }
        public void remove() {
          iter.remove();
        }
      };
    }

    private void checkRefQ() {
      Reference ref;
      while ((ref = refQ.poll()) != null) {
        subscribers.remove(ref);
      }
    }
  }

  private Subscribers subscribers = new Subscribers();

  /** Our ALPPlan **/
  private ALPPlan alpPlan;

  private String myName;

  /** Isolated constructor **/
  public Distributor(String name) {
    if (name == null) name = "Anonymous";
    myName = name;

    if (Boolean.getBoolean("org.cougaar.distributor.debug")) {
      try {
        logWriter = new PrintWriter(new FileWriter("Distributor_" + name + ".log", true));
        printLog("Distributor Started");
      }
      catch (IOException e) {
        System.err.println("Can't open Distributor log file: " + e);
      }
    }
    // create the message distribution thread (but don't start it),
    // requiring our client to call this.start().
  }

  public void setPersistence(Persistence newPersistence) {
    persistence = newPersistence;
  }

  public Persistence getPersistence() {
    return persistence;
  }

  public void setALPPlan(ALPPlan alpPlan) {
    this.alpPlan = alpPlan;
  }

  private void printEnvelope(Envelope envelope, SubscriptionClient client) {
    boolean first = true;
    for (Iterator tuples = envelope.getAllTuples(); tuples.hasNext(); ) {
      if (first) {
        printLog("Outbox of " + client.getSubscriptionClientName());
        first = false;
      }
      EnvelopeTuple tuple = (EnvelopeTuple) tuples.next();
      if (tuple.isBulk()) {
        for (Iterator objects = ((BulkEnvelopeTuple) tuple).getCollection().iterator();
             objects.hasNext(); ) {
          printLog("BULK   " + objects.next());
        }
      } else {
        String kind = ""; 
        if (tuple.isAdd()) {
          kind = "ADD    ";
        } else if (tuple.isChange()) {
          kind = "CHANGE ";
        } else {
          kind = "REMOVE ";
        }
        printLog(kind + tuple.getObject());
      }
    }
  }

  private void printLog(String msg) {
    logWriter.print(logTimeFormat.format(new Date(System.currentTimeMillis())));
    logWriter.println(msg);
    logWriter.flush();
  }

  public String toString() {
    return "<Distributor " + myName + ">";
  }

  public boolean didRehydrate(Subscriber subscriber) {
    if (!didRehydrate) return false;
    return (persistence.getSubscriberState(subscriber) != null);
  }

  public MessageManager getMessageManager() {
    if (persistence == null) {
      if (myMessageManager == null) {
	myMessageManager = new MessageManagerImpl(false);
      }
      return myMessageManager;
    } else {
      return persistence.getMessageManager();
    }
  }

  private void rehydrate() {
    if (persistence != null) {
      rehydrationEnvelope = new PersistenceEnvelope();
      List undistributedEnvelopes = persistence.rehydrate(rehydrationEnvelope);
      if (undistributedEnvelopes != null) {
        didRehydrate = true;
	postRehydrationEnvelopes = new ArrayList();
        postRehydrationEnvelopes.addAll(undistributedEnvelopes);
        epochEnvelopes.addAll(undistributedEnvelopes);
      }
    }
  }

  /**
   * Rehydrate a new subscription. New subscriptions that correspond
   * to persisted subscriptions are quietly fed the
   * rehydrationEnvelope which has all the objects that had been
   * distributed prior to the last rehydrated delta. Objects in the
   * rehydrationEnvelope do not waken the subscriber; they are simply
   * added to the subscription container. Next, the envelopes that
   * were pending in the inbox of the subscriber at the last delta are
   * fed to the subscription. The subscriber _will_ be notified of
   * these. Finally, all envelopes in postRehydrationEnvelopes are fed
   * to the subscription. The subscriber will be notified of these as
   * well.
   **/
  private void rehydrateNewSubscription(Subscription s,
                                        List persistedTransactionEnvelopes,
                                        List persistedPendingEnvelopes)
  {
    s.fill(rehydrationEnvelope);
    if (persistedTransactionEnvelopes != null) {
      for (Iterator iter = persistedTransactionEnvelopes.iterator(); iter.hasNext(); ) {
        s.fill((Envelope) iter.next());
      }
    }
    if (persistedPendingEnvelopes != null) {
      for (Iterator iter = persistedPendingEnvelopes.iterator(); iter.hasNext(); ) {
        s.fill((Envelope) iter.next());
      }
    }
    for (Iterator iter = postRehydrationEnvelopes.iterator(); iter.hasNext(); ) {
      s.fill((Envelope) iter.next());
    }
  }

  /**
   * Discard rehydration info for a given subscriber
   **/
  void discardRehydrationInfo(Subscriber subscriber) {
    if (rehydrationEnvelope != null) {
      persistence.discardSubscriberState(subscriber);
      if (!persistence.hasSubscriberStates()) {
        discardRehydrationInfo();
      }
    }
  }

  private void discardRehydrationInfo() {
    rehydrationEnvelope = null;
    postRehydrationEnvelopes = null;
  }

  /** provide a hook to start the distribution thread.
   * Note that although Distributor is Runnable, it does not extend Thread,
   * rather, it maintains it's own thread state privately.
   **/
  public synchronized void start(Cluster theCluster) {
    rehydrate();
    getMessageManager().start(theCluster, didRehydrate);
  }

  //
  // Subscriber Services
  //

  /**
   * Register a Subscriber with the Distributor.  Future envelopes are
   * distributed to all registered subscribers.
   **/
  public synchronized void registerSubscriber(Subscriber subscriber) {
    subscribers.register(subscriber);
  }

  /**
   * Unregister subscriber with the Distributor. Future envelopes are
   * not distributed to unregistered subscribers.
   **/
  public synchronized void unregisterSubscriber(Subscriber subscriber) {
    subscribers.unregister(subscriber);
  }

  /**
   * Provide a new subscription with its initial fill. If the
   * subscriber of the subscription was persisted, we fill from the
   * persisted information (see rehydrateNewSubscription) otherwise we
   * fill from the ALPPlan (alpPlan.fillSubscription).
   **/
  public synchronized void fillSubscription(Subscription subscription) {
    Subscriber subscriber = subscription.getSubscriber();
    PersistenceSubscriberState subscriberState = null;
    if (didRehydrate) {
      subscriberState = persistence.getSubscriberState(subscriber);
    }
    if (subscriberState != null &&
        subscriberState.pendingEnvelopes != null) {
      rehydrateNewSubscription(subscription,
                               subscriberState.transactionEnvelopes,
                               subscriberState.pendingEnvelopes);
    } else {
      alpPlan.fillSubscription(subscription);
    }
  }

  public synchronized void fillQuery(Subscription subscription) {
    alpPlan.fillQuery(subscription);
  }

  // These are used as locals, but allocated here to reduce consing.
  private final List outboxes = new ArrayList();
  private static final List emptyList = new ArrayList(0);
  private final List subscriberStates = new ArrayList();
  private final List messagesToSend = new ArrayList();
  private final List directiveMessages = new ArrayList();

  /**
   * The main workhorse of the distributor. Distributes the contents
   * of an outbox envelope to everybody.
   *
   * If needToPersist is true and it is time to persist, we set the
   * persistPending flag to prevent any further openTransactions from
   * happening. Then we distribute the outbox and consequent
   * envelopes. If anything is distributed, we set the needToPersist
   * flag. Any messages generated by the ALPPlan are gathered and
   * given to the message manager for eventual transmission. Finally,
   * the generation of a persistence delta is considered.
   **/
  private void distribute(Envelope outbox, SubscriptionClient client) {
    if (outbox != null && logWriter != null) {
      printEnvelope(outbox, client);
    }
    if (persistence != null) {
      if (needToPersist) {
        if (timeToPersist()) {
          setPersistPending(true);  // Lock out new transactions
        }
      }
    }
    boolean haveSomethingToDistribute = false;
    while (outbox != null && outbox.size() > 0) {
      outboxes.add(outbox);
      outbox = alpPlan.receiveEnvelope(outbox);
      haveSomethingToDistribute = true;
    }
    boolean busy = haveSomethingToDistribute;
    if (persistence != null) {
      if (!needToPersist && haveSomethingToDistribute) {
        needToPersist = true;
      }
    }
    for (Iterator iter = subscribers.iterator(); iter.hasNext(); ) {
      Subscriber subscriber = (Subscriber) iter.next();
      if (subscriber == alpPlan) continue;
      if (haveSomethingToDistribute) {
        subscriber.receiveEnvelopes(outboxes);
      } else if (!busy && subscriber.isBusy()) {
        busy = true;
      }
    }
    alpPlan.appendMessagesToSend(messagesToSend); // Fill messagesToSend
    if (messagesToSend.size() > 0) {
      getMessageManager().sendMessages(messagesToSend.iterator());
    }
    messagesToSend.clear();
    if (persistence != null) {
      if (postRehydrationEnvelopes != null) {
        postRehydrationEnvelopes.addAll(outboxes);
      }
      epochEnvelopes.addAll(outboxes);
      if (!needToPersist && getMessageManager().needAdvanceEpoch()) {
        needToPersist = true;
      }
      if (!busy && transactionCount > 0) busy = true;
      if (needToPersist) {
        if (!busy) {
          setPersistPending(true);  // Lock out new transactions
        }
      }
      if (persistPending) {
        if (transactionCount == 0) {
          doPersistence();
        }
      }
    }
    outboxes.clear();
  }

  /**
   * Process directive and ack messages from other clusters. Acks are
   * given to the message manager. Directive messages are passed
   * through the message manager for validation and then given to the
   * ALPPlan for processing. Envelopes resulting from that processing
   * are distributed.
   **/
  public synchronized void receiveMessages(List messages) {
    for (Iterator msgs = messages.iterator(); msgs.hasNext(); ) {
      Object m = msgs.next();
      if (m instanceof DirectiveMessage) {
        DirectiveMessage msg = (DirectiveMessage) m;
        int code = getMessageManager().receiveMessage(msg);
        if ((code & MessageManager.RESTART) != 0) {
          alpPlan.restart(msg.getSource());
        }
        if ((code & MessageManager.IGNORE) == 0) {
          directiveMessages.add(msg);
        }
      } else if (m instanceof AckDirectiveMessage) {
        AckDirectiveMessage msg = (AckDirectiveMessage) m;
        int code = getMessageManager().receiveAck(msg);
        if ((code & MessageManager.RESTART) != 0) {
          alpPlan.restart(msg.getSource()); // Remote cluster has restarted
        }
      }
    }
    // We nominally ack the messages here so the persisted state will
    // include the acks. The acks are not actually sent until the
    // persistence delta is concluded.
    getMessageManager().acknowledgeMessages(directiveMessages.iterator());

    // The following must be unconditional to insure persistence
    // happens.
    distribute(alpPlan.receiveMessages(directiveMessages), alpPlan.getClient());
    directiveMessages.clear();
  }

  /**
   * Generate a persistence delta if possible and necessary. It is
   * possible if the transaction count is zero and necessary if either
   * persistPending is true or needToPersist is true and we are not
   * busy. This second clause is needed so we don't end up being idle
   * with needToPersist being true.
   **/
  private void doPersistence() {
    for (Iterator iter = subscribers.iterator(); iter.hasNext(); ) {
      Subscriber subscriber = (Subscriber) iter.next();
      if (subscriber.isReadyToPersist()) {
        subscriberStates.add(new PersistenceSubscriberState(subscriber));
      }
    }
    persistence.persist(epochEnvelopes, emptyList, subscriberStates);
    epochEnvelopes.clear();
    subscriberStates.clear();
    setPersistPending(false);
    needToPersist = false;
    lastPersist = System.currentTimeMillis();
  }

  private boolean timeToPersist() {
    return (System.currentTimeMillis() - lastPersist > MAX_PERSIST_INTERVAL);
  }

  /**
   * Transaction control
   **/
  private Object transactionLock = new Object();
  private boolean persistPending = false;
  private int transactionCount = 0;

  public void startTransaction() {
    synchronized (transactionLock) {
      while (persistPending) {
        try {
          transactionLock.wait();
        }
        catch (InterruptedException ie) {
        }
      }
      ++transactionCount;
    }
  }

  public void finishTransaction(Envelope outbox, SubscriptionClient client) {
    synchronized (transactionLock) {
      --transactionCount;
    }
    synchronized (this) {
      distribute(outbox, client);
    }
  }

  private void setPersistPending(boolean on) {
    synchronized (transactionLock) {
      persistPending = on;
      if (!persistPending) {
        transactionLock.notifyAll();
      }
    }
  }
}

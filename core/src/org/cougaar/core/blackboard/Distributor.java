/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.blackboard;

import org.cougaar.core.agent.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.cougaar.core.blackboard.*;
import org.cougaar.core.persist.Persistence;
import org.cougaar.core.persist.PersistenceSubscriberState;
import org.cougaar.core.persist.RehydrationResult;
import org.cougaar.core.persist.PersistenceNotEnabledException;
import org.cougaar.core.service.NamingService;
import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.util.UnaryPredicate;

/**  The Distributor
 * 
 * @property org.cougaar.core.agent.keepPublishHistory When set to <em>true</em>, enables tracking of 
 * all publishes.  Extremely expensive.
 **/
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
  private static final long TIMER_PERSIST_INTERVAL = 33000L;

  /** True if using lazy persistence **/
  private boolean lazyPersistence = true;

  /** The object we use to persist ourselves. */
  private Persistence persistence = null;

  /** The time that we last persisted **/
  protected long lastPersist = System.currentTimeMillis();

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

  /** Timer thread for periodic lazy-persistence. **/
  private Timer distributorTimer = null;

  /** Debug logging **/
  private transient PrintWriter logWriter = null;

  public PublishHistory history =
    System.getProperty("org.cougaar.core.agent.keepPublishHistory", "false").equals("true")
      ? new PublishHistory()
      : null;

  /** The format of timestamps in the log **/
  private DateFormat logTimeFormat =
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

  /** Our Blackboard **/
  private Blackboard blackboard;

  private String myName;

  /** Isolated constructor **/
  public Distributor(Blackboard blackboard, String name) {
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
    this.blackboard = blackboard;
    // create the message distribution thread (but don't start it),
    // requiring our client to call this.start().
  }

  public void setPersistence(Persistence newPersistence, boolean lazy) {
    persistence = newPersistence;
    lazyPersistence = lazy;
  }

  /** called by Subscriber to link into Blackboard persistence mechanism **/
  Persistence getPersistence() {
    return persistence;
  }

  private void printEnvelope(Envelope envelope, BlackboardClient client) {
    boolean first = true;
    for (Iterator tuples = envelope.getAllTuples(); tuples.hasNext(); ) {
      if (first) {
        printLog("Outbox of " + client.getBlackboardClientName());
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
    return myMessageManager;
  }

  /** Pass thru to blackboard to safely return blackboard object counts.
   * Used by BlackboardMetricsService
   * @param predicate The objects to count in the blackboard
   * @return int The count of objects that match the predicate currently in the blackboard
   **/
  public synchronized int getBlackboardCount(UnaryPredicate predicate) {
    return blackboard.countBlackboard(predicate);
  }

  /** Pass thru to blackboard to safely return the size of the blackboard
   *  collection.
   **/
  public synchronized int getBlackboardSize() {
    return blackboard.getBlackboardSize();
  }

  /**
   * Rehydrate this blackboard. If persistence is off, just create a
   * MessageManager that does nothing. If persistence is on, try to
   * rehydrate from existing persistence deltas. The result of this is
   * a List of undistributed envelopes and a MessageManager. There
   * might be no MessageManager in the result signifying that either
   * there were no persistence deltas or that lazyPersistence was on
   * so the message manager did not need to be saved. In either case
   * the existence of an appropriate message manager is assured. The
   * undistributed envelopes might be null signifying that there was
   * no persistence deltas in existence. This is reflected in the
   * value of the didRehydrate flag.
   **/
  private void rehydrate(Object state) {
    if (persistence != null) {
      rehydrationEnvelope = new PersistenceEnvelope();
      RehydrationResult rr = persistence.rehydrate(rehydrationEnvelope, state);
      if (lazyPersistence) {    // Ignore any rehydrated message manager
        myMessageManager = new MessageManagerImpl(false);
      } else {
        myMessageManager = rr.messageManager;
        if (myMessageManager == null) {
          myMessageManager = new MessageManagerImpl(true);
        }
      }
      if (rr.undistributedEnvelopes != null) {
        didRehydrate = true;
	postRehydrationEnvelopes = new ArrayList();
        postRehydrationEnvelopes.addAll(rr.undistributedEnvelopes);
        epochEnvelopes.addAll(rr.undistributedEnvelopes);
      }
    } else {
      myMessageManager = new MessageManagerImpl(false);
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
  public synchronized void start(ClusterServesLogicProvider theCluster, Object state) {
    rehydrate(state);
    getMessageManager().start(theCluster, didRehydrate);

    if (lazyPersistence && distributorTimer == null) {
      distributorTimer = new Timer();
      distributorTimer.schedule(new TimerTask() {
        public void run() {
          timerPersist();
        }
      }, 
        TIMER_PERSIST_INTERVAL, 
        TIMER_PERSIST_INTERVAL);
    }
  }

  /** provide a hook to stop the distribution thread.
   * @see #start
   **/
  public synchronized void stop() {
    getMessageManager().stop();

    if (lazyPersistence && distributorTimer != null) {
      distributorTimer.cancel();
      distributorTimer = null;
    }
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
   * fill from the Blackboard (blackboard.fillSubscription).
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
      blackboard.fillSubscription(subscription);
    }
  }

  public synchronized void fillQuery(Subscription subscription) {
    blackboard.fillQuery(subscription);
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
   * flag. Any messages generated by the Blackboard are gathered and
   * given to the message manager for eventual transmission. Finally,
   * the generation of a persistence delta is considered.
   **/
  private void distribute(Envelope outbox, BlackboardClient client) {
    if (outbox != null && logWriter != null) {
      printEnvelope(outbox, client);
    }
    if (persistence != null) {
      if (needToPersist) {
        if (timeToPersist()) {
          maybeSetPersistPending();  // Lock out new transactions
        }
      }
    }
    boolean haveSomethingToDistribute = false;
    // nest loops in case delayed actions cascade into more
    // lp actions.
    while (outbox != null && outbox.size() > 0) {
      while (outbox != null && outbox.size() > 0) {
        outboxes.add(outbox);
        outbox = blackboard.receiveEnvelope(outbox);
        haveSomethingToDistribute = true;
      }

      // outbox should be empty at this point.
      // execute any pending DelayedLPActions
      outbox = blackboard.executeDelayedLPActions();
    }

    //      while (outbox != null && outbox.size() > 0) {
    //        outboxes.add(outbox);
    //        outbox = blackboard.receiveEnvelope(outbox);
    //        haveSomethingToDistribute = true;
    //      }

    /**
     * busy indicates that we have found evidence that things are
     * still happening or are about to happen in this agent.
     **/
    boolean busy = haveSomethingToDistribute;
    if (persistence != null) {
      if (!needToPersist && haveSomethingToDistribute) {
        needToPersist = true;
      }
    }
    for (Iterator iter = subscribers.iterator(); iter.hasNext(); ) {
      Subscriber subscriber = (Subscriber) iter.next();
      if (subscriber == blackboard) continue;
      if (haveSomethingToDistribute) {
        subscriber.receiveEnvelopes(outboxes);
      } else if (!busy && subscriber.isBusy()) {
        busy = true;
      }
    }
    blackboard.appendMessagesToSend(messagesToSend); // Fill messagesToSend
    if (messagesToSend.size() > 0) {
      if (logWriter != null) {
        for (Iterator i = messagesToSend.iterator(); i.hasNext(); ) {
          DirectiveMessage msg = (DirectiveMessage) i.next();
          Directive[] dirs = msg.getDirectives();
          for (int j = 0; j < dirs.length; j++) {
            printLog("SEND   " + dirs[j]);
          }
        }
      }
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
          maybeSetPersistPending();  // Lock out new transactions
        }
      }
      if (persistPending) {
        if (transactionCount == 0) {
          doPersistence();
        } else {
          System.out.println("Persist deferred, "
                             + transactionCount
                             + " transactions open");
        }
      }
    }
    outboxes.clear();
  }

  public synchronized void restartAgent(ClusterIdentifier cid) {
    try {
      blackboard.startTransaction();
      blackboard.restart(cid);
      Envelope envelope = blackboard.receiveMessages(Collections.EMPTY_LIST);
      distribute(envelope, blackboard.getClient());
    } finally {
      blackboard.stopTransaction();
    }
  }

  /**
   * Process directive and ack messages from other clusters. Acks are
   * given to the message manager. Directive messages are passed
   * through the message manager for validation and then given to the
   * Blackboard for processing. Envelopes resulting from that processing
   * are distributed.
   **/
  public synchronized void receiveMessages(List messages) {
    for (Iterator msgs = messages.iterator(); msgs.hasNext(); ) {
      Object m = msgs.next();
      if (m instanceof DirectiveMessage) {
        DirectiveMessage msg = (DirectiveMessage) m;
        int code = getMessageManager().receiveMessage(msg);
        if ((code & MessageManager.RESTART) != 0) {
          try {
            blackboard.startTransaction();
            blackboard.restart(msg.getSource());
          } finally {
            blackboard.stopTransaction();
          }
        }
        if ((code & MessageManager.IGNORE) == 0) {
          if (logWriter != null) {
            Directive[] dirs = msg.getDirectives();
            for (int i = 0; i < dirs.length; i++) {
              printLog("RECV   " + dirs[i]);
            }
          }
          directiveMessages.add(msg);
        }
      } else if (m instanceof AckDirectiveMessage) {
        AckDirectiveMessage msg = (AckDirectiveMessage) m;
        int code = getMessageManager().receiveAck(msg);
        if ((code & MessageManager.RESTART) != 0) {
          blackboard.restart(msg.getSource()); // Remote cluster has restarted
        }
      }
    }
    // We nominally ack the messages here so the persisted state will
    // include the acks. The acks are not actually sent until the
    // persistence delta is concluded.
    getMessageManager().acknowledgeMessages(directiveMessages.iterator());

    // The following must be unconditional to insure persistence
    // happens.
    try {
      blackboard.startTransaction();
      Envelope envelope = blackboard.receiveMessages(directiveMessages);
      distribute(envelope, blackboard.getClient());
    } finally {
      blackboard.stopTransaction();
    }

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
    doPersistence(false, false);
  }

  private Object doPersistence(boolean persistedStateNeeded, boolean full) {
    for (Iterator iter = subscribers.iterator(); iter.hasNext(); ) {
      Subscriber subscriber = (Subscriber) iter.next();
      if (subscriber.isReadyToPersist()) {
        subscriberStates.add(new PersistenceSubscriberState(subscriber));
      }
    }
    Object result;
    synchronized (getMessageManager()) {
      getMessageManager().advanceEpoch();
      result = persistence.persist(epochEnvelopes, emptyList, subscriberStates,
                                   persistedStateNeeded, full,
                                   lazyPersistence ? null : getMessageManager());
    }
    epochEnvelopes.clear();
    subscriberStates.clear();
    setPersistPending(false);
    needToPersist = false;
    lastPersist = System.currentTimeMillis();
    return result;
  }

  protected boolean timeToLazilyPersist() {
    long overdue = System.currentTimeMillis() - persistence.getPersistenceTime();
    if (overdue > 0L) {
      System.out.println("Lazy persistence overdue by " + overdue);
    } 
    return overdue > 0L;
  }

  private boolean timeToPersist() {
    long nextPersistTime = Math.min(lastPersist + MAX_PERSIST_INTERVAL,
                                    persistence.getPersistenceTime());
    return (System.currentTimeMillis() >= nextPersistTime);
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

  private void finishTransaction() {
    synchronized (transactionLock) {
      --transactionCount;
      transactionLock.notifyAll();
    }
  }

  public void finishTransaction(Envelope outbox, BlackboardClient client) {
    finishTransaction();
    synchronized (this) {
      distribute(outbox, client);
    }
  }

  /**
   * Force a persistence delta to be generated.
   **/
  public void persistNow() throws PersistenceNotEnabledException {
    persist(false, false);
    System.gc();
    persist(false, true);
  }

  /**
   * Force a (full) persistence delta to be generated and return result
   **/
  public Object getState() throws PersistenceNotEnabledException {
    persist(false, false);
    System.gc();
    return persist(true, true);
  }

  /**
   * Generate a persistence delta and (maybe) return the data of that
   * delta. We call startTransaction to insure that persistence is
   * neither in progress nor can begin. Then we wait until there are
   * no other transactions in progress and then call doPersistence.
   * @param isStateWanted true if the data of a full persistence delta is wanted
   * @return a state Object including all the data from a full
   * persistence delta if isStateWanted is true, null if isStateWanted
   * is false.
   **/
  private Object persist(boolean isStateWanted, boolean full)
    throws PersistenceNotEnabledException
  {
    if (persistence == null)
      throw new PersistenceNotEnabledException();
    synchronized (transactionLock) {
      startTransaction();
      try {
        // Invariant: persistPending==false transactionCount >= 1
        while (transactionCount > 1) {
          try {
            transactionLock.wait();
          }
          catch (InterruptedException ie) {
          }
        }
        // We are the only one left in the pool
        return doPersistence(isStateWanted, full);
      } finally {
        finishTransaction();
      }
    }
  }

  private void maybeSetPersistPending() {
    if (!lazyPersistence || timeToLazilyPersist()) {
      setPersistPending(true);
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

  protected void timerPersist() {
    if (needToPersist && (!lazyPersistence || timeToLazilyPersist())) {
      try {
        persist(false, false);
      } catch (PersistenceNotEnabledException pnee) {
        pnee.printStackTrace();
      }
    }
  }
}

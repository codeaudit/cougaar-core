/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.agent.ClusterServesLogicProvider;
import org.cougaar.core.persist.Persistence;
import org.cougaar.core.persist.PersistenceNotEnabledException;
import org.cougaar.core.persist.PersistenceSubscriberState;
import org.cougaar.core.persist.RehydrationResult;
import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

/**
 * The Distributor coordinates blackboard transactions, subscriber
 * updates, and persistence.
 * <p>
 * An agent has one blackboard, one distributor, and zero or more
 * subscribers.  Each subscriber has a set of subscriptions that
 * are known only to that subscriber.  A subscriber registers with
 * the distributor to receive blackboard add/change/remove
 * notification.  When a subscriber wishes to modify the
 * blackboard contents, it must start a distributor transaction,
 * fill an envelope with add/change/remove tuples, and finish
 * the distributor transaction.  The blackboard is a special
 * subscriber that maintains a view of all blackboard objects and
 * manages the logic providers.  The distributor also coordinates
 * periodic persistence and ensures that blackboard updates are
 * thread-safe.
 *
 * @property org.cougaar.core.agent.keepPublishHistory
 *   if set to <em>true</em>, enables tracking of
 *   all publishes.  Extremely expensive.
 **/
public final class Distributor {

  /*
   * Design summary:
   *
   * The distributor uses two locks:
   *   distributorLock
   *   transactionLock
   * The distributorLock guards against "distribute()" and
   * blackboard modification.  The transactionLock guards against
   * start/finish transaction and persistence.  Only one distribute
   * can occur at a time, and it can't occur at the same time as a
   * persist.  Only one persist can occur at a time, and there must
   * be no active transactions (to guard against object
   * modifications during serialization).  When a persist is not
   * taking place it's okay for transactions to start/finish while
   * a distribute is taking place, to provide parallelism.
   *
   * Persistence runs in either a lazy or non-lazy mode, where the
   * best-supported mode is lazy.  Lazy persistence occurs
   * periodically (33 seconds) if the blackboard has been modified
   * during that time, and only if the persistence implementation
   * indicates that it is "getPersistenceTime()".
   *
   * Old notes, partially accurate:
   *
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
   */

  /** The maximum interval between persistence deltas. **/
  private static final long MAX_PERSIST_INTERVAL = 37000L;
  private static final long TIMER_PERSIST_INTERVAL = 33000L;

  //
  // these are set in the constructor and are final:
  //

  /** The publish history is available for subscriber use. */
  public final PublishHistory history =
    (Boolean.getBoolean("org.cougaar.core.agent.keepPublishHistory") ?
     new PublishHistory() :
     null);

  /** the name of this distributor */
  private final String name;

  // blackboard, noted below.

  /** the logger, which is thread safe */
  private final Logger logger =
    LoggerFactory.getInstance().createLogger(getClass());

  //
  // these are set immediately following the constructor, and
  // are effectively final:
  //

  /** True if using lazy persistence **/
  private boolean lazyPersistence = true;

  /** The object we use to persist ourselves. */
  private Persistence persistence = null;

  //
  // lock for open/close transaction and persistence:
  //

  private final Object transactionLock = new Object();

  // the following are locked under the transactionLock:
  private boolean persistPending = false;
  private boolean persistActive = false;
  private int transactionCount = 0;

  // temporary list for use within "doPersist":
  private final List subscriberStates = new ArrayList();

  //
  // lock for distribute and blackboard access:
  //

  private final Object distributorLock = new Object();

  // the following are locked under the distributorLock:

  /** our blackboard */
  private final Blackboard blackboard;

  /** True if rehydration occurred at startup **/
  private boolean didRehydrate = false;

  /** Envelopes that have been distributed during a
   * persistence epoch. **/
  private final List epochEnvelopes = new ArrayList();

  /** The message manager for this cluster **/
  private MessageManager myMessageManager = null;

  /** Timer thread for periodic lazy-persistence. **/
  private Timer distributorTimer = null;

  private final Subscribers subscribers = new Subscribers();

  // temporary lists, for use within "distribute()":
  private final List outboxes = new ArrayList();
  private final List messagesToSend = new ArrayList();

  // temporary list, for use within "receiveMessages()":
  private final List directiveMessages = new ArrayList();

  //
  // These are partially locked, and may cause bugs in
  // the future.  In practice they seem to be fine:
  //

  /** Envelopes distributed since the last rehydrated delta **/
  private List postRehydrationEnvelopes = null;

  /** All objects published prior to the last rehydrated delta **/
  private PersistenceEnvelope rehydrationEnvelope = null;

  /** The time that we last persisted **/
  private long lastPersist = System.currentTimeMillis();

  /** Do we need to persist sometime; changed state has not
   * been persisted **/
  private boolean needToPersist = false;


  /** Isolated constructor **/
  public Distributor(Blackboard blackboard, String name) {
    this.blackboard = blackboard;
    this.name = (name != null ? name : "Anonymous");
    if (logger.isInfoEnabled()) {
      logger.info(name + ": Distributor started");
    }
  }

  /**
   * Called by the blackboard immediately after the constructor,
   * and only once.
   */
  void setPersistence(Persistence newPersistence, boolean lazy) {
    assert persistence == null : "persistence already set";
    persistence = newPersistence;
    lazyPersistence = lazy;
  }

  /**
   * Called by Subscriber to link into Blackboard persistence
   * mechanism
   */
  Persistence getPersistence() {
    return persistence;
  }

  /**
   * Called by subscriber to discard rehydration info.
   */
  void discardRehydrationInfo(Subscriber subscriber) {
    // FIXME this isn't locked!
    if (rehydrationEnvelope != null) {
      persistence.discardSubscriberState(subscriber);
      if (!persistence.hasSubscriberStates()) {
        // discard rehydration info:
        rehydrationEnvelope = null;
        postRehydrationEnvelopes = null;
      }
    }
  }

  public boolean didRehydrate(Subscriber subscriber) {
    if (!didRehydrate) return false;
    return (persistence.getSubscriberState(subscriber) != null);
  }

  /**
   * Pass thru to blackboard to safely return blackboard object
   * counts.
   * Used by BlackboardMetricsService
   * @param predicate The objects to count in the blackboard
   * @return int The count of objects that match the predicate
   *   currently in the blackboard
   **/
  public int getBlackboardCount(UnaryPredicate predicate) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      return blackboard.countBlackboard(predicate);
    }
  }

  /**
   * Pass thru to blackboard to safely return the size of the
   * blackboard collection.
   **/
  public int getBlackboardSize() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      return blackboard.getBlackboardSize();
    }
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
    assert  Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (persistence != null) {
      rehydrationEnvelope = new PersistenceEnvelope();
      RehydrationResult rr =
        persistence.rehydrate(rehydrationEnvelope, state);
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

  private MessageManager getMessageManager() {
    return myMessageManager;
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
  private void rehydrateNewSubscription(
      Subscription s,
      List persistedTransactionEnvelopes,
      List persistedPendingEnvelopes) {
    assert  Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    s.fill(rehydrationEnvelope);
    if (persistedTransactionEnvelopes != null) {
      for (Iterator iter = persistedTransactionEnvelopes.iterator();
          iter.hasNext(); ) {
        s.fill((Envelope) iter.next());
      }
    }
    if (persistedPendingEnvelopes != null) {
      for (Iterator iter = persistedPendingEnvelopes.iterator();
          iter.hasNext(); ) {
        s.fill((Envelope) iter.next());
      }
    }
    for (Iterator iter = postRehydrationEnvelopes.iterator();
        iter.hasNext(); ) {
      s.fill((Envelope) iter.next());
    }
  }

  /** provide a hook to start the distribution thread.
   * Note that although Distributor is Runnable, it does not
   * extend Thread, rather, it maintains it's own thread state
   * privately.
   **/
  public void start(
      ClusterServesLogicProvider theCluster, Object state) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      rehydrate(state);
      getMessageManager().start(theCluster, didRehydrate);

      if (lazyPersistence && distributorTimer == null) {
        distributorTimer = new Timer();
        TimerTask tt =
          new TimerTask() {
            public void run() {
              timerPersist();
            }
          };
        distributorTimer.schedule(
            tt,
            TIMER_PERSIST_INTERVAL,
            TIMER_PERSIST_INTERVAL);
      }
    }
  }

  /** provide a hook to stop the distribution thread.
   * @see #start
   **/
  public void stop() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      getMessageManager().stop();

      if (lazyPersistence && distributorTimer != null) {
        distributorTimer.cancel();
        distributorTimer = null;
      }
    }
  }

  //
  // Subscriber Services
  //

  /**
   * Register a Subscriber with the Distributor.  Future envelopes are
   * distributed to all registered subscribers.
   **/
  public void registerSubscriber(Subscriber subscriber) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      subscribers.register(subscriber);
    }
  }

  /**
   * Unregister subscriber with the Distributor. Future envelopes are
   * not distributed to unregistered subscribers.
   **/
  public void unregisterSubscriber(Subscriber subscriber) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      subscribers.unregister(subscriber);
    }
  }

  /**
   * Provide a new subscription with its initial fill. If the
   * subscriber of the subscription was persisted, we fill from the
   * persisted information (see rehydrateNewSubscription) otherwise
   * we fill from the Blackboard (blackboard.fillSubscription).
   **/
  public void fillSubscription(Subscription subscription) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      Subscriber subscriber = subscription.getSubscriber();
      PersistenceSubscriberState subscriberState = null;
      if (didRehydrate) {
        if (subscriber.isReadyToPersist()) {
          if (logger.isInfoEnabled()) {
            logger.info(
                name + ": No subscriber state for late subscribe of " +
                subscriber.getName());
          }
        } else {
          subscriberState =
            persistence.getSubscriberState(subscriber);
        }
      }
      if (subscriberState != null &&
          subscriberState.pendingEnvelopes != null) {
        rehydrateNewSubscription(subscription,
            subscriberState.transactionEnvelopes,
            subscriberState.pendingEnvelopes);
      } else {
        blackboard.fillSubscription(subscription);
      }

      // distribute the initialize envelope
      /*
         {
      // option 1
      distribute(new InitializeSubscriptionEnvelope(subscription), null);
      }
       */
      // blackboard subscribes don't need an ISE to fill
      if (subscriber != blackboard) {
        // option 2
        Subscriber s = subscription.getSubscriber();
        List l = new ArrayList(1);
        l.add(new InitializeSubscriptionEnvelope(subscription));
        s.receiveEnvelopes(l);    // queue in the right spot
      }
    }
  }

  public void fillQuery(Subscription subscription) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      blackboard.fillQuery(subscription);
    }
  }

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
    assert  Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (outbox != null &&
        logger.isDebugEnabled() &&
        client != null) {
      logEnvelope(outbox, client);
    }
    if (persistence != null) {
      if (needToPersist) {
        if (timeToPersist()) {
          maybeSetPersistPending();  // Lock out new transactions
        }
      }
    }
    blackboard.prepareForEnvelopes();
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
    // Fill messagesToSend
    blackboard.appendMessagesToSend(messagesToSend);
    if (messagesToSend.size() > 0) {
      if (logger.isDebugEnabled()) {
        for (Iterator i = messagesToSend.iterator(); i.hasNext(); ) {
          DirectiveMessage msg = (DirectiveMessage) i.next();
          Directive[] dirs = msg.getDirectives();
          for (int j = 0; j < dirs.length; j++) {
            logger.debug(name + ": SEND   " + dirs[j]);
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
    }
    outboxes.clear();
  }

  public void restartAgent(ClusterIdentifier cid) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    try {
      startTransaction();
      synchronized (distributorLock) {
        try {
          blackboard.startTransaction();
          blackboard.restart(cid);
          Envelope envelope =
            blackboard.receiveMessages(Collections.EMPTY_LIST);
          distribute(envelope, blackboard.getClient());
        } finally {
          blackboard.stopTransaction();
        }
      }
    } finally {
      finishTransaction();
    }
  }

  /**
   * Process directive and ack messages from other clusters. Acks
   * are given to the message manager. Directive messages are passed
   * through the message manager for validation and then given to
   * the Blackboard for processing. Envelopes resulting from that
   * processing are distributed.
   **/
  public void receiveMessages(List messages) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    try {
      startTransaction(); // Blocks if persistence active

      synchronized (distributorLock) {
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
              if (logger.isDebugEnabled()) {
                Directive[] dirs = msg.getDirectives();
                for (int i = 0; i < dirs.length; i++) {
                  logger.debug(name + ": RECV   " + dirs[i]);
                }
              }
              directiveMessages.add(msg);
            }
          } else if (m instanceof AckDirectiveMessage) {
            AckDirectiveMessage msg = (AckDirectiveMessage) m;
            int code = getMessageManager().receiveAck(msg);
            if ((code & MessageManager.RESTART) != 0) {
              // Remote cluster has restarted
              blackboard.restart(msg.getSource());
            }
          }
        }
        // We nominally ack the messages here so the persisted
        // state will include the acks. The acks are not actually
        // sent until the persistence delta is concluded.
        getMessageManager().acknowledgeMessages(
            directiveMessages.iterator());

        try {
          blackboard.startTransaction();
          Envelope envelope = blackboard.receiveMessages(
              directiveMessages);
          distribute(envelope, blackboard.getClient());
        } finally {
          blackboard.stopTransaction();
        }
        directiveMessages.clear();
      }

    } finally {
      finishTransaction();
    }
  }

  public void invokeABAChangeLPs(Set communities) {
    assert  Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      try {
        blackboard.startTransaction();
        blackboard.invokeABAChangeLPs(communities);
      } finally {
        blackboard.stopTransaction();
      }
    }
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

  private Object doPersistence(
      boolean persistedStateNeeded, boolean full) {
    assert !Thread.holdsLock(distributorLock);
    assert  Thread.holdsLock(transactionLock);
    assert transactionCount == 1 : transactionCount;
    for (Iterator iter = subscribers.iterator(); iter.hasNext(); ) {
      Subscriber subscriber = (Subscriber) iter.next();
      if (subscriber.isReadyToPersist()) {
        subscriberStates.add(
            new PersistenceSubscriberState(subscriber));
      }
    }
    Object result;
    synchronized (getMessageManager()) {
      getMessageManager().advanceEpoch();
      result = persistence.persist(
          epochEnvelopes,
          Collections.EMPTY_LIST,
          subscriberStates,
          persistedStateNeeded,
          full,
          lazyPersistence ? null : getMessageManager());
    }
    epochEnvelopes.clear();
    subscriberStates.clear();
    setPersistPending(false);
    needToPersist = false;
    lastPersist = System.currentTimeMillis();
    return result;
  }

  private boolean timeToLazilyPersist() {
    long overdue =
      System.currentTimeMillis() -
      persistence.getPersistenceTime();
    return overdue > 0L;
  }

  private boolean timeToPersist() {
    assert  Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    // FIXME use of lastPersist outside transactionLock
    long nextPersistTime =
      Math.min(
          lastPersist + MAX_PERSIST_INTERVAL,
          persistence.getPersistenceTime());
    return (System.currentTimeMillis() >= nextPersistTime);
  }

  /**
   * Transaction control
   **/

  public void startTransaction() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (transactionLock) {
      while (persistPending || persistActive) {
        try {
          transactionLock.wait();
        }
        catch (InterruptedException ie) {
        }
      }
      ++transactionCount;
    }
  }

  public void finishTransaction(
      Envelope outbox, BlackboardClient client) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      distribute(outbox, client);
    }
    finishTransaction();
  }

  private void finishTransaction() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (transactionLock) {
      if (persistPending) {
        if (transactionCount == 1) {
          // transactionCount == 1 implies persistActive == false
          assert !persistActive;
          doPersistence();
        } else {
          if (logger.isInfoEnabled()) {
            logger.info(name + ": Persist deferred, "
                + transactionCount
                + " transactions open");
          }
        }
      }
      --transactionCount;
      assert transactionCount >= 0 : transactionCount;
      transactionLock.notifyAll();
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
   * Force a (full) persistence delta to be generated and
   * return result
   **/
  public Object getState() throws PersistenceNotEnabledException {
    persist(false, false);
    System.gc();
    return persist(true, true);
  }

  /**
   * Generate a persistence delta and (maybe) return the data of
   * that delta.
   * <p>
   * This code parallels that of start/finish transaction except
   * that:<pre>
   *   distribute() is not called
   *   we wait for all transactions to close
   * </pre><p>
   * A "persistActive" flag is used to guarantee that only
   * one persist occurs at a time.
   *
   * @param isStateWanted true if the data of a full persistence
   *   delta is wanted
   * @return a state Object including all the data from a full
   * persistence delta if isStateWanted is true, null if
   *   isStateWanted is false.
   **/
  private Object persist(boolean isStateWanted, boolean full)
    throws PersistenceNotEnabledException
    {
      assert !Thread.holdsLock(distributorLock);
      assert !Thread.holdsLock(transactionLock);
      if (persistence == null)
        throw new PersistenceNotEnabledException();
      synchronized (transactionLock) {
        while (persistActive) {
          try {
            transactionLock.wait();
          } catch (InterruptedException ie) {
          }
        }
        persistActive = true;
        transactionCount++;
        assert transactionCount >= 1 : transactionCount;
        try {
          while (transactionCount > 1) {
            try {
              transactionLock.wait();
            } catch (InterruptedException ie) {
            }
          }
          assert transactionCount == 1 : transactionCount;
          // persistPending == don't care, transactionCount == 1
          // We are the only one left in the pool
          return doPersistence(isStateWanted, full);
        } finally {
          persistActive = false;
          --transactionCount;
          assert transactionCount == 0 : transactionCount;
          transactionLock.notifyAll();
        }
      }
    }

  private void maybeSetPersistPending() {
    assert  Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (!lazyPersistence || timeToLazilyPersist()) {
      setPersistPending(true);
    }
  }

  private void setPersistPending(boolean on) {
    // caller may hold the distributorLock
    // caller may already hold the transactionLock
    synchronized (transactionLock) {
      // FIXME holds both locks!
      persistPending = on;
      if (!persistPending) {
        transactionLock.notifyAll();
      }
    }
  }

  private void timerPersist() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    // FIXME unlocked access!
    if (needToPersist &&
        (!lazyPersistence || timeToLazilyPersist())) {
      try {
        persist(false, false);
      } catch (PersistenceNotEnabledException pnee) {
        pnee.printStackTrace();
      }
    }
  }

  private void logEnvelope(Envelope envelope, BlackboardClient client) {
    if (!logger.isDebugEnabled()) return;
    boolean first = true;
    for (Iterator tuples = envelope.getAllTuples(); tuples.hasNext(); ) {
      if (first) {
        logger.debug(
            name + ": Outbox of " + client.getBlackboardClientName());
        first = false;
      }
      EnvelopeTuple tuple = (EnvelopeTuple) tuples.next();
      if (tuple.isBulk()) {
        for (Iterator objects =
            ((BulkEnvelopeTuple) tuple).getCollection().iterator();
            objects.hasNext(); ) {
          logger.debug(name + ": BULK   " + objects.next());
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
        logger.debug(name + ": " + kind + tuple.getObject());
      }
    }
  }

  public String toString() {
    return "<Distributor " + name + ">";
  }


  /**
   * Hold our set of registered Subscribers.
   * <p>
   * The Distributor must lock this object with it's
   * "distributorLock".
   */
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

      class MyIterator implements Iterator {
        private Object n = null;
        private Iterator iter;

        public MyIterator(Iterator it) {
          iter = it;
          advance();
        }

        /** advance to the next non-null element, dropping
         * nulls along the way
         **/
        private void advance() {
          while (iter.hasNext()) {
            WeakReference ref = (WeakReference) iter.next();
            n = ref.get();
            if (n == null) {
              iter.remove();
            } else {
              return;
            }
          }
          // ran off the end,
          n = null;
        }

        public boolean hasNext() {
          return (n != null);
        }
        public Object next() {
          Object x = n;
          advance();
          return x;
        }
        public void remove() {
          iter.remove();
        }
      };
      return new MyIterator(subscribers.iterator());
    }

    private void checkRefQ() {
      Reference ref;
      while ((ref = refQ.poll()) != null) {
        subscribers.remove(ref);
      }
    }
  }

}

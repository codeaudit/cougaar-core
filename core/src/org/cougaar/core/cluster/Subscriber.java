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

import org.cougaar.util.LockFlag;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.util.EmptyIterator;
import org.cougaar.util.UnaryPredicate;
import java.util.*;

// pollution of subscriber purity for completion checking
import org.cougaar.domain.planning.ldm.plan.*;

public class Subscriber {
  private static boolean isEnforcing =
    (Boolean.valueOf(System.getProperty("org.cougaar.enforceTransactions", "true"))).booleanValue();

  private static boolean warnUnpublishChanges = 
    "true".equals(System.getProperty("org.cougaar.subscriber.warnUnpublishChanges","false"));

  private SubscriptionClient theClient = null;
  private Distributor theDistributor = null;
  private String subscriberName = "";
  private boolean shouldBePersisted = true;
  private boolean firstTransactionComplete = false;

  protected Subscriber(){} 

  /** Create a subscriber that provides subscription services 
   * to a client and send outgoing messages to a Distributor.
   * Plugin clients will use this API.
   **/
  public Subscriber(SubscriptionClient client, Distributor Distributor) {
    setClientDistributor(client,Distributor);
  }

  public Subscriber(SubscriptionClient client, Distributor Distributor, String subscriberName) {
    setClientDistributor(client,Distributor);
    setName(subscriberName);
  }

  public void setClientDistributor(SubscriptionClient client, Distributor newDistributor)
  {
    theClient = client;
    if (theDistributor != newDistributor) {
      if (theDistributor != null) {
        theDistributor.unregisterSubscriber(this);
      }
      theDistributor = newDistributor;
      if (theDistributor != null) {
        theDistributor.registerSubscriber(this);
      }
    }
  }

  public void setName(String newName) {
    subscriberName = newName;
  }

  public String getName() {
    return subscriberName;
  }

  public boolean shouldBePersisted() {
    return shouldBePersisted;
  }

  public void setShouldBePersisted(boolean shouldBePersisted) {
    this.shouldBePersisted = shouldBePersisted;
  }

  boolean isReadyToPersist() {
    return firstTransactionComplete;
  }

  public void setReadyToPersist() {
    theDistributor.discardRehydrationInfo(this);
    firstTransactionComplete = true;
  }

  public boolean didRehydrate() {
    boolean result = theDistributor.didRehydrate(this);
    return result;
  }

  /**
   * Move inboxes into subscriptions.
   **/
  protected boolean privateUpdateSubscriptions() {
    boolean changedp = false;
    synchronized (subscriptions) {
      transactionEnvelopes = flushInbox();
      try {
        for (int i = 0, n = subscriptions.size(); i < n; i++) {
          Subscription subscription = (Subscription) subscriptions.get(i);
          for (int j = 0, l = transactionEnvelopes.size(); j<l; j++) {
            Envelope envelope = (Envelope) transactionEnvelopes.get(j);
            try {
              changedp |= envelope.applyToSubscription(subscription);
            } catch (PublishException pe) {
              synchronized (System.err) {
                System.err.println(pe.getMessage());
                SubscriptionClient currentClient = null;
                if (envelope instanceof OutboxEnvelope) {
                  OutboxEnvelope e = (OutboxEnvelope) envelope;
                  currentClient = e.theClient;
                }
                if (currentClient == null) {
                  currentClient = SubscriptionClient.current.getClient();
                }
                String thisPublisher = null;
                if (currentClient != null) {
                  thisPublisher = currentClient.getSubscriptionClientName();
                }
                if (envelope instanceof ALPPlan.PlanEnvelope) {
                  if (thisPublisher == null) {
                    thisPublisher = "ALPPlan";
                  } else {
                    thisPublisher = "ALPPlan after " + thisPublisher;
                  }
                } else if (thisPublisher == null) {
                  thisPublisher = "Unknown";
                }
                pe.printStackTrace(" This publisher: " + thisPublisher);
                if (pe.priorStack == null) {
                  System.err.println("Prior publisher: Unknown");
                } else {
                  pe.priorStack.printStackTrace();
                }
              }
            }
          }
        }
      } catch (RuntimeException re) {
        re.printStackTrace();
      }
    }
    return changedp;
  }

  /**
   * Report changes that the plugin published.
   * These changes are represented by the outbox.
   **/
  protected Envelope privateGetPublishedChanges() {
    Envelope box = flushOutbox();
    if (transactionEnvelopes != null) {
      recycleInbox(transactionEnvelopes);
      transactionEnvelopes = null;
    } else {
      recycleInbox(flushInbox());
    }
    return box;
  }

  /**
   * Accessors to persist our inbox state
   **/
  public List getTransactionEnvelopes() {
    return transactionEnvelopes;
  }

  public List getPendingEnvelopes() {
    return pendingEnvelopes;
  }

  //////////////////////////////////////////////////////
  //              Subscriptions                       //
  //////////////////////////////////////////////////////
  private int publishAddedCount;
  private int publishChangedCount;
  private int publishRemovedCount;

  public int getSubscriptionCount() {
    return subscriptions.size();
  }
  
  public int getSubscriptionSize() {
    int size = 0;
    for (int i = 0; i < subscriptions.size(); i++) {
      Object s = subscriptions.get(i);
      if (s instanceof CollectionSubscription) {
        size += ((CollectionSubscription)s).size();
      }
    }
    return size;
  }

  public int getPublishAddedCount() {
    return publishAddedCount;
  }

  public int getPublishChangedCount() {
    return publishChangedCount;
  }

  public int getPublishRemovedCount() {
    return publishRemovedCount;
  }


  /** our set of active subscriptions */
  protected List subscriptions = new ArrayList();

  protected void resetSubscriptionChanges() {
    synchronized (subscriptions) {
      int l = subscriptions.size();
      for (int i=0; i<l; i++) {
        Subscription s = (Subscription) subscriptions.get(i);
        s.resetChanges();
      }
      resetHaveCollectionsChanged();
    }
  }

  /**
   * Subscribe to a collection service with isMember, default inner
   * collection and supporting incremental change queries.
   **/
  public Subscription subscribe(UnaryPredicate isMember) {
    return subscribe(isMember, null, true);
  }

  /**
   * Subscribe to a collection service with isMember, default inner
   * collection and specifying if you want incremental change query support.
   **/
  public Subscription subscribe(UnaryPredicate isMember, boolean isIncremental) {
    return subscribe(isMember, null, isIncremental);
  }

  /**
   * Subscribe to a collection service with isMember, specifying inner
   * collection and supporting incremental change queries.
   **/
  public Subscription subscribe(UnaryPredicate isMember, Collection realCollection){
    return subscribe(isMember, realCollection, true);
  }

  /**
   * Subscribe to a collection service.
   * Tells the Distributor about its interest, but should not block,
   * even if there are lots of "back issues" to transmit.
   * This is the full form.
   * @param isMember The defining predicate for the slice of the logplan.
   * @param realCollection The real container wrapped by the returned value.
   * @param isIncremental IFF true, returns a container that supports delta
   * lists.
   * @return A Subscription.
   * @see org.cougaar.core.cluster.IncrementalSubscription.
   **/
  public Subscription subscribe(UnaryPredicate isMember, Collection realCollection, boolean isIncremental){
    Subscription sn;

    if (realCollection == null) 
      realCollection = new HashSet();

    if (isIncremental) {
      sn = new IncrementalSubscription(isMember, realCollection);
    } else {
      sn = new CollectionSubscription(isMember, realCollection);
    }
    return subscribe(sn);
  }

  /** Primary subscribe method.  Register a new subscription.
   **/
  public final Subscription subscribe(Subscription subscription) {
//      checkTransactionOK("subscribe()");

    synchronized (subscriptions) {
      subscription.setSubscriber(this);
      subscriptions.add(subscription);
      theDistributor.fillSubscription(subscription);
    }
    setHaveNewSubscriptions();  // make sure we get counted.
    return subscription;
  }
    
  /** lightweight query of ALPPlan **/
  public final Collection query(UnaryPredicate isMember) {
    QuerySubscription s = new QuerySubscription(isMember);
    s.setSubscriber(this);      // shouldn't really be needed
    theDistributor.fillQuery(s);
    return s.getCollection();
  }

  final void checkTransactionOK(String methodname, Object arg) {
    if (this instanceof ALPPlan) return;               // No check for ALPPlan
    if (!isMyTransaction()) {
      synchronized (System.err) {
        if (arg != null) { methodname = methodname+"("+arg+")"; }
        System.err.println(toString()+"."+methodname+" called outside of transaction:");
        Thread.dumpStack();
      }
      //throw new RuntimeException(methodname+" called outside of transaction boundaries");
    }
  }

  final void checkTransactionOK(String methodname) {
    checkTransactionOK(methodname, null);
  }

  /**
   * Stop subscribing to a previously obtained Subscription. The
   * Subscription must have been returned from a previous call to
   * subscribe.
   * @param subscription the Subscription that is to be cancelled.
   **/
  public void unsubscribe(Subscription subscription) {
//      checkTransactionOK("unsubscribe()");
    synchronized (subscriptions) {
      subscriptions.remove(subscription);
    }
  }

  /*
   * Inbox invariants:
   * pendingEnvelopes accumulates new envelopes for the next transaction (always).
   * transactionEnvelopes has the previous pendingEnvelopes during a
   * transaction, null otherwise.
   * idleEnvelopes has an empty list when no transaction is active.
   *
   * The list cycle around from idle to pending to transaction back to
   * idle. idle and transaction are never null at the same time; one
   * of them always has the list the pendingEnvelopes does not have
   *
   */
  private List pendingEnvelopes = new ArrayList();     // Envelopes to be added at next transaction
  private List transactionEnvelopes = null;            // Envelopes of current transaction
  private List idleEnvelopes = new ArrayList();        // Alternate list
  private Object inboxLock = new Object();             // For synchronized access to inboxes

  /** Called by non-client methods to add an envelope to our inboxes */
  public void receiveEnvelopes(List envelopes) {
    synchronized (inboxLock) {
      pendingEnvelopes.addAll(envelopes);
    }
    signalExternalActivity();
  }

  public boolean isBusy() {
    synchronized (inboxLock) {
      return (pendingEnvelopes.size() > 0);
    }
  }

  private List flushInbox() {
    synchronized (inboxLock) {
      List result = pendingEnvelopes;
      pendingEnvelopes = idleEnvelopes;
      idleEnvelopes = null;
      return result;
    }
  }

  private void recycleInbox(List old) {
    old.clear();
    idleEnvelopes = old;
  }

  /** outbox data structure - an Envelope used to encapsulate 
   * outgoing changes to collections.
   **/
  private Envelope outbox = createEnvelope();

  protected Envelope flushOutbox() {
    if (outbox.size() == 0) return null;
    Envelope result = outbox;
    outbox = createEnvelope();
    return result;
  }

  public static class OutboxEnvelope extends Envelope {
    public OutboxEnvelope(SubscriptionClient client) {
      theClient = client;
    }
    public SubscriptionClient theClient;
  }

  /** factory method for creating Envelopes of the correct type **/
  protected Envelope createEnvelope() {
    return new Envelope();
// return new OutboxEnvelope(getClient());  // for debugging
  }

  // might want to make the syncs finer-grained
  /** called whenever the client adds an object to a collection
   * to notify the rest of the world of the change.
   * Actual Changes to the collection only happen via this api.
   **/
  protected EnvelopeTuple clientAddedObject(Object o) {
    checkTransactionOK("add", o);

    // attempt to claim the object
    claimObject(o);

    return outbox.addObject(o);
  }

  /** called whenever the client removes an object from a collection
   * to notify the rest of the world of the change.
   * Actual Changes to the collection only happen via this api.
   **/
  protected EnvelopeTuple clientRemovedObject(Object o) {
    checkTransactionOK("remove", o);

    // attempt to unclaim the object
    unclaimObject(o);

    return outbox.removeObject(o);
  }

  /** called whenever the client changes an object in a collection
   * to notify the rest of the world of the change.
   * Actual Changes to the collection only happen via this api.
   **/
  protected EnvelopeTuple clientChangedObject(Object o, List changes) {
    checkTransactionOK("change", o);    

    return outbox.changeObject(o, changes);
  }
  
  /** Add an object to the Plan.
   * Behavior is not defined if the object was already a member of the plan.
   **/

  public final boolean publishAdd(Object o) {
    if (theDistributor.history != null) theDistributor.history.publishAdd(o);
    if (o instanceof ActiveSubscriptionObject ) {
      if (! ((ActiveSubscriptionObject)o).addingToLogPlan(this)) 
        return false;
    }

    if (o instanceof Publishable) {
      List crs = Transaction.getCurrentTransaction().getChangeReports(o);
    }

    // if we made it this far publish the object and return true.
    clientAddedObject(o);
    publishAddedCount++;
    return true;
  }
  
  /** Remove an object from the Plan.
   * Behavior is not defined if the object was not already a member of the plan.
   **/
  public final boolean publishRemove(Object o) {
    if (theDistributor.history != null) theDistributor.history.publishRemove(o);
    if (o instanceof ActiveSubscriptionObject ) {
      if (! ((ActiveSubscriptionObject)o).removingFromLogPlan(this)) 
        return false;
    }

    if (o instanceof Publishable) {
      List crs = Transaction.getCurrentTransaction().getChangeReports(o);
      if (warnUnpublishChanges) {
        if (crs != null && crs.size()>0) {
          System.err.println("Warning: publishRemove("+o+") is dropping outstanding changes:\n\t"+crs);
        }
      }
    }

    clientRemovedObject(o);
    publishRemovedCount++;
    return true;
  }

  /** Convenience function for publishChange(o, null).
   **/
  public final boolean publishChange(Object o) {
    return publishChange(o, null);
  }

  /** mark an element of the Plan as changed.
   * Behavior is not defined if the object is not a member of the plan.
   * There is no need to call this if the object was added or removed,
   * only if the contents of the object itself has been changed.
   * The changes parameter describes a set of changes made to the
   * object beyond those tracked automatically by the object class
   * (see the object class documentation for a description of which
   * types of changes are tracked).  Any additional changes are
   * merged in <em>after</em> automatically collected reports.
   * @param changes a set of ChangeReport instances or null.
   **/
  public final boolean publishChange(Object o, Collection changes) {
    if (theDistributor.history != null) theDistributor.history.publishChange(o);
    if (o instanceof ActiveSubscriptionObject ) {
      if (! ((ActiveSubscriptionObject)o).changingInLogPlan(this)) 
        return false;
    }

    List crs = null;
    if (o instanceof Publishable) {
      crs = Transaction.getCurrentTransaction().getChangeReports(o);
    }
    if (changes != null) {
      if (crs == null) {
        crs = new ArrayList(changes);
      } else {
        crs.addAll(changes);
      }
    }

    // if we made it this far publish the change and return true.
    clientChangedObject(o, crs);
    publishChangedCount++;
    return true;
  }

  /** A extension subscriber may call this method to execute bulkAdd transactions.
   * This is protected because it is of very limited to other than persistance plugins.
   *  Note that LogPlan does something like
   * this by hand constructing an entire special-purpose envelope.  This, however, is
   * for use in-band, in-transaction.  
   *  The Collection passed MUST be immutable, since there may be many consumers,
   * each running at different times.
   **/
  protected EnvelopeTuple bulkAddObject(Collection c) {
    checkTransactionOK("bulkAdd", c);    

    EnvelopeTuple t;
    t = outbox.bulkAddObject(c);

    return t;
  }

  /** Safer version of bulkAddObject(Collection).
   * Creates a Collection from the Enumeration and passes it into
   * the envelope.
   **/
  protected EnvelopeTuple bulkAddObject(Enumeration en) {
    checkTransactionOK("bulkAdd", en);    

    EnvelopeTuple t;
    t = outbox.bulkAddObject(en);

    return t;
  }

  protected EnvelopeTuple bulkAddObject(Iterator en) {
    checkTransactionOK("bulkAdd", en);    

    EnvelopeTuple t;
    t = outbox.bulkAddObject(en);

    return t;
  }

  //
  // Transaction handling.
  //
  /*
   * It would be nice if we could merge the Transaction object with the
   * older open/close transaction code somehow - there is some redundancy
   * and the current parallel implementations are somewhat confusing.
   */


  /**
   * The transaction lock.  At most one watcher per subscriber gets to
   * have an open transaction at one time.  We could support multiple
   * simultaneously open transactions with multiple subscribers, but
   * this is a feature for another day.
   **/
  private LockFlag transactionLock = new LockFlag();
  
  /** The current in-force transaction instance.
   * The is only kept around as a check to pass to Transaction.close()
   * in order to make sure we're closing the right one.
   * In particular, we cannot use this in the publishWhatever methods 
   * because the LogPlan methods are executing in the wrong thread.
   **/
  private Transaction theTransaction = null;

  /** overridable by extending classes to specify more featureful
   * Transaction semantics.
   **/
  protected Transaction newTransaction() {
    return new Transaction(this);
  }

  /**
   * Open a transaction by grabbing the transaction lock and updating
   * the subscriptions.  This method blocks waiting for the
   * transaction lock.
   **/
  public final void openTransaction() {
    transactionLock.getBusyFlag();
    finishOpenTransaction();
  }

  /**
   * Common routine for both openTransaction and tryOpenTransaction
   * does everything except getting the transactionLock busy flag.
   **/
  private void finishOpenTransaction() {
    int count = transactionLock.getBusyCount();
    if (count > 1) {
      if (isEnforcing) {
        System.err.println("Opened nested transaction (level="+count+")");
        Thread.dumpStack();
      }
      return;
    }

    startTransaction();

    theDistributor.startTransaction();
    if (privateUpdateSubscriptions()) {
      setHaveCollectionsChanged();
    }
    if (haveNewSubscriptions()) {
      setHaveCollectionsChanged();
      resetHaveNewSubscriptions();
    }
  }

  protected final void startTransaction() {
    theTransaction = newTransaction();
    Transaction.open(theTransaction);
  }

  private boolean _haveNewSubscriptions = false;
  private boolean haveNewSubscriptions() { return _haveNewSubscriptions; }
  private void setHaveNewSubscriptions() { _haveNewSubscriptions = true; }
  private void resetHaveNewSubscriptions() { _haveNewSubscriptions = false; }

  /** keep track of whether or not the collections have changed 
   * since the previous openTransaction.
   **/
  private boolean _haveCollectionsChangedSinceLastTransaction = false;

  /** set haveCollectionsChanged() **/
  private void setHaveCollectionsChanged() {
    _haveCollectionsChangedSinceLastTransaction = true;
  }

  /** set haveCollectionsChanged() **/
  private void resetHaveCollectionsChanged() {
    _haveCollectionsChangedSinceLastTransaction = false;
  }

  /** can be called by anyone who can open a transaction to decide what to do.
   * returned value is only valid/useful inside an open transaction.
   **/
  public boolean haveCollectionsChanged() {
    return _haveCollectionsChangedSinceLastTransaction;
  }

  /** Attempt to open a transaction by attempting to grab the 
   * transaction lock and updating the collections (iff we got the 
   * lock).
   *
   * This is equivalent to the old (misnamed) tryLockSubscriber method
   * in PlugInWrapper.
   *
   * @return true IFF a transaction was opened.
   **/
  public final boolean tryOpenTransaction() {
    if (transactionLock.tryGetBusyFlag()) {
      finishOpenTransaction();
      return true;
    }
    return false;
  }

  /** Close a transaction opened by openTransaction() or a 
   * successful tryOpenTransaction().
   * @param resetSubscriptions IFF true, all subscriptions will have
   * their resetChanges() method called to clear any delta lists, etc.
   * @exception SubscriberException IFF we did not own the transaction
   * lock.
   **/
  public final void closeTransaction(boolean resetSubscriptions)
    throws SubscriberException {
    if (transactionLock.getBusyFlagOwner() == Thread.currentThread()) {
      // only do our closeTransaction work when exiting the nest.
      if (transactionLock.getBusyCount() == 1) {
        checkUnpostedChangeReports();

        if (!isReadyToPersist()) {
          setReadyToPersist();
        }
        if (resetSubscriptions)
          resetSubscriptionChanges();
        Envelope box = privateGetPublishedChanges();
        try {
          theDistributor.finishTransaction(box, getClient());
        } finally {
          stopTransaction();
        }
      } else {
        //System.err.println("Closed nested transaction.");
      }        
      if (! transactionLock.freeBusyFlag()) {
        throw new SubscriberException("Failed to close an owned transaction");
      }
    } else {
      throw new SubscriberException("Attempt to close a non-open transaction");
    }
  }

  protected final void stopTransaction() {
    Transaction.close(theTransaction);
    theTransaction = null;
  }

  protected final void checkUnpostedChangeReports() {
    //Map map = theTransaction.getChangeMap();
    Map map = Transaction.getCurrentTransaction().getChangeMap();
    if (warnUnpublishChanges) {
      if (map == null || map.size()==0) return;
      
      System.err.println("\nWarning! ignoring outstanding unpublished changes: ");
      for (Iterator ki = map.keySet().iterator(); ki.hasNext(); ) {
        Object o = ki.next();
        List l = (List)map.get(o);
        System.err.println("\t"+o+" ("+l.size()+")");
        // we could just publish them with something like:
        //handleActiveSubscriptionObjects()
        //clientChangedObject(o, l);
      }
    }
  }

  /** Close a transaction opened by openTransaction() or a 
   * successful tryOpenTransaction().
   * Will reset all subscription change tracking facilities.
   * To avoid this, use closeTransaction(false) instead.
   * @exception SubscriberException IFF we did not own the transaction
   * lock.
   **/
  public final void closeTransaction() {
    closeTransaction(true);
  }

  /** Does someone have an open transaction? **/
  public final boolean isInTransaction() {
    return (transactionLock.getBusyFlagOwner() != null);
  }

  /** Do I have an open transaction?
   * This really translates to "Is is safe to make changes to my
   * collections?"
   **/
  public final boolean isMyTransaction() {
    return (transactionLock.getBusyFlagOwner() == Thread.currentThread());
  }
  

  //
  // Interest Handling - extension of earlier wakeRequest and
  //   interestSemaphore code.
  //

  /** list of SubscriptionWatchers to be notified when something
   * interesting happens.
   **/
  private List watchers = new ArrayList();

  public final SubscriptionWatcher registerInterest(SubscriptionWatcher w) {
    watchers.add(w);
    return w;
  }

  /** Allow a thread of a subscriber to register an interest in the
   * subscriber's collections.  Mainly used to allow threads to monitor
   * changes in collections - that is, the fact of change, not the details.
   * The level of support here is like the old wake and interestSemaphore
   * code.  The client of a subscriber need not register explicitly, as
   * it is done at initialization time.
   **/
  public final SubscriptionWatcher registerInterest() {
    return registerInterest(new SubscriptionWatcher());
  }

  /** Allow a thread to unregister an interest registered by
   * registerInterest.  Should be done if a subordinate (watching)
   * thread exits, or a plugin unloads.
   **/
  public final void unregisterInterest(SubscriptionWatcher w) throws SubscriberException {
    if (! watchers.remove(w) ) {
      throw new SubscriberException("Attempt to unregisterInterest of unknown SubscriptionWatcher");
    }
  }


  //
  // watcher triggers
  //

  private boolean _externalActivity = false;
  private boolean _internalActivity = false;
  private boolean _clientActivity = false;

  public boolean wasExternalActivity() { return _externalActivity; }
  public boolean wasInternalActivity() { return _internalActivity; }
  public boolean wasClientActivity() { return _clientActivity; }

  /** called when external activity changes the subscriber's collections.
   * by default, just calls wakeSubscriptionWatchers, but subclasses
   * may be more circumspect.
   **/
  public void signalExternalActivity() {
    _externalActivity = true;
    wakeSubscriptionWatchers(SubscriptionWatcher.EXTERNAL);
  }
  /** called when internal activity actually changes the subscriber's
   * collections. 
   * by default, just calls wakeSubscriptionWatchers, but subclasses
   * may be more circumspect.
   **/
  public void signalInternalActivity() {
    _internalActivity = true;
    wakeSubscriptionWatchers(SubscriptionWatcher.INTERNAL);
  }
  /** called when the client (PlugIn) requests that it be waked again.
   * by default, just calls wakeSubscriptionWatchers, but subclasses
   * may be more circumspect.
   **/
  public void signalClientActivity() {
    _clientActivity = true;
    wakeSubscriptionWatchers(SubscriptionWatcher.CLIENT);
  }
  
    
  /** called to notify all SubscriptionWatchers.
   **/
  private final void wakeSubscriptionWatchers(int event) {
    synchronized (watchers) {
      int l = watchers.size();
      for (int i=0; i<l; i++) {
        ((SubscriptionWatcher) (watchers.get(i))).signalNotify(event);
      }
    }
  }

  //
  // usability and debugability methods
  // 

  public String toString() {
    String cs = "(self)";
    if (theClient != this)
      cs = theClient.toString();

    return "<"+getClass().getName()+" "+this.hashCode()+" for "+cs+" and "+
      theDistributor+">";
  }


  /** Quality Assurance for logplan object adds.
   * Checks to make sure that adds of complex objects also
   * add the simpler subs.
   **/
  protected void checkAdds(Envelope outbox) {
    List tuples = outbox.getRawDeltas();
    int l = tuples.size();
    for (int i = 0; i<l; i++) {
      EnvelopeTuple tup = (EnvelopeTuple) tuples.get(i);
      if (tup.getAction() == Envelope.ADD) {
        Object obj = tup.getObject();
        if (obj instanceof PlanElement) {
          if (obj instanceof Expansion) {
            Workflow w = ((Expansion) obj).getWorkflow();
            if (! checkFor(w, Envelope.ADD, outbox)) {
              System.err.println("Add of PE Expansion without WF:\n"+
                                 "\tPE = "+obj+"\n"+
                                 "\tWF = "+w);
              Thread.dumpStack();
            }
          }
        } else if (obj instanceof Workflow) {
          Enumeration tasks = ((Workflow) obj).getTasks();
          while (tasks.hasMoreElements()) {
            Task t = (Task) tasks.nextElement();
            if (! checkFor(t, Envelope.ADD, outbox)) {
              System.err.println("Add of Workflow without SUBTASK:\n"+
                                 "\tWF = "+obj+"\n"+
                                 "\tSubTask = "+t);
              Thread.dumpStack();
            }
          }
        }
      }
    }
  }

  // used by checkAdds to see if added objects are in
  // the same envelope.
  private boolean checkFor(Object o, int action, Envelope outbox) {
    List tuples = outbox.getRawDeltas();
    int l = tuples.size();
    for (int i = 0; i<l; i++) {
      EnvelopeTuple tup = (EnvelopeTuple) tuples.get(i);
      if (tup.getObject() == o && tup.getAction() == action) return true;
    }
    return false;
  }
    
  /** utility to claim an object as ours **/
  protected void claimObject(Object o) {
    if (o instanceof PlanElement) {
      PlanElement pe = (PlanElement) o;
      Task t = pe.getTask();
      if (t instanceof Claimable) {
        //System.err.println("\n->"+getClient()+" claimed "+t);
        ((Claimable)t).setClaim(getClient());
      }
    }
  }

  /** utility to release a claim on an object **/
  protected void unclaimObject(Object o) {
    if (o instanceof PlanElement) {
      PlanElement pe = (PlanElement) o;
      Task t = pe.getTask();
      // done in planelementimpl
      //((mil.darpa.log.org.cougaar.domain.planning.ldm.plan.TaskImpl)t).privately_resetPlanElement();
      if (t instanceof Claimable) {
        ((Claimable)t).resetClaim(getClient());
      }
    }
  }
  
  /** return the client of the the subscriber.
   * May be overridden by subclasses in case they are really
   * delegating to some other object.
   **/
  public SubscriptionClient getClient() {
    return theClient;
  }

  /** Accept an event from an EventSubscription.
   * @param event The event to be accepted.
   * @return true IFF the event is actually accepted.
   **/
  public boolean triggerEvent(Object event) {
    return theClient.triggerEvent(event);
  }

}

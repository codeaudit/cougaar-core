/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import java.util.*;

import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.domain.planning.ldm.RootFactory;
import org.cougaar.core.plugin.PlugInServesCluster;
import org.cougaar.core.cluster.ClusterServesPlugIn;
import org.cougaar.core.cluster.Claimable;
import org.cougaar.core.cluster.SubscriptionClient;
import org.cougaar.core.cluster.Subscriber;
import org.cougaar.core.cluster.CollectionSubscription;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.Distributor;
import org.cougaar.core.cluster.SubscriptionWatcher;
import org.cougaar.core.cluster.SubscriberException;
import org.cougaar.core.cluster.Alarm;
import org.cougaar.core.cluster.MetricsSnapshot;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.util.GenericStateModel;
import org.cougaar.util.StateModelException;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.UnaryPredicate;

public abstract class PlugInAdapter
  extends GenericStateModelAdapter
  implements PlugInServesCluster, SubscriptionClient, ParameterizedPlugIn
{

  // 
  // constructor
  //

  public PlugInAdapter() {
  }

  //
  // Implement (some of) SubscriptionClient
  //
  protected String subscriptionClientName = null;

  public String getSubscriptionClientName() {
    if (subscriptionClientName == null) {
      StringBuffer buf = new StringBuffer();
      buf.append(getClass().getName());
      if (parameters != null) {
	buf.append("[");
	String sep = "";
	for (Enumeration params = parameters.elements(); params.hasMoreElements(); ) {
	  buf.append(sep);
	  buf.append(params.nextElement().toString());
	  sep = ",";
	}
	buf.append("]");
      }
      subscriptionClientName = buf.substring(0);
    }
    return subscriptionClientName;
  }

  public String toString() {
    return getSubscriptionClientName();
  }

  public boolean triggerEvent(Object event) {
    return false;
  }

  //
  // implement ParameterizedPlugIn
  //


    /**
     * Support "interval parameters" which are long values that can be
     * expressed with time period units (e.g. seconds)
     **/
    private static class Interval {
        String name;
        long factor;
        public Interval(String name, long factor) {
            this.name = name;
            this.factor = factor;
        }
    }

    /**
     * The known unit names
     **/
    private static Interval[] intervals = {
        new Interval("seconds", 1000L),
        new Interval("minutes", 1000L * 60L),
        new Interval("hours",   1000L * 60L * 60L),
        new Interval("days",    1000L * 60L * 60L * 24L),
        new Interval("weeks",   1000L * 60L * 60L * 24L * 7L),
    };

    /**
     * Make this utility trivially accessible to plugins
     **/
    public long parseIntervalParameter(int paramIndex) {
        return parseInterval((String) getParameters().get(paramIndex));
    }

    public long parseInterval(String param) {
        param = param.trim();
        int spacePos = param.indexOf(' ');
        long factor = 1L;
        if (spacePos >= 0) {
            String units = param.substring(spacePos + 1).toLowerCase();
            param = param.substring(0, spacePos);
            for (int i = 0; i < intervals.length; i++) {
                if (intervals[i].name.startsWith(units)) {
                    factor = intervals[i].factor;
                    break;
                }
            }
        }
        return Long.parseLong(param) * factor;
    }

  
  private Vector parameters = null;

  public void setParameters(Vector params) {
    parameters = params;
  }

  /** get any PlugIn parameters passed by the plugin instantiator.
   * If they haven't been set, will return null.
   * Should be set between plugin construction and initialization.
   **/
  public Vector getParameters() {
    return parameters;
  }


  //
  // StateModel extensions
  //

  /** Watch for who we are plugging into **/
  public void load(Object object) throws StateModelException {
    super.load(object);
    if (!(object instanceof ClusterServesPlugIn)) {
      throw new RuntimeException("Loaded plugin "+this+" into non Cluster "+object);
    }
    theCluster =  (ClusterServesPlugIn) object;
    theDistributor = theCluster.getDistributor();
    theSubscriber = constructSubscriber(theDistributor);
    theLDM = theCluster.getLDM();
    theLDMF = theLDM.getFactory();
    setThreadingModel(createThreadingModel());
  }

  /** start threads but isn't yet allowed to ask component for anything */
  public void start() throws StateModelException {
    startThreadingModel();
    super.start();
  }


  //
  // Customization of PlugInAdapter
  //


  /** Construct (and return) a new Subscriber instance for the
   * plugin to use.  This method is called in the default version
   * of the load() method to contruct a (the) critical subscriber 
   * instance for the plugin - that is, the value that is returned by
   * getSubscriber().  This method should not do anything beyond
   * the critical construction of this instance.  Subscriber.start() is
   * called in the (default) plugin.start() method.
   *
   * Override to change the class of the created instance.  The default
   * should be sufficient for nearly all plugins.  Only persistence plugins
   * are known to sometimes need this level of control, and doing this 
   * wrong can be A Very Bad Thing.
   *
   * This default method creates and initializes an instance
   * of org.cougaar.core.cluster.Subscriber.
   */
  protected Subscriber constructSubscriber(Distributor distributor) {
    return new Subscriber(this, distributor);
  }

  public int getSubscriptionCount() {
    return getSubscriber().getSubscriptionCount();
  }
  
  public int getSubscriptionSize() {
    return getSubscriber().getSubscriptionSize();
  }

  public int getPublishAddedCount() {
    return getSubscriber().getPublishAddedCount();
  }

  public int getPublishChangedCount() {
    return getSubscriber().getPublishChangedCount();
  }

  public int getPublishRemovedCount() {
    return getSubscriber().getPublishRemovedCount();
  }

  //
  // Ivars and accessor methods
  //

  private Subscriber theSubscriber = null;

  /** Safely return our Subscription service (Subscriber)
   * PlugIn.load() must have completed in order 
   * for the value to be defined.
   **/
  protected final Subscriber getSubscriber() {
    return theSubscriber; 
  }
    
  /** a reference to our subscription handler **/
  private Distributor theDistributor = null;

  /** Safely return our Distribution service (Distributor).
   * load() must have completed for this to 
   * be defined.
   **/
  protected final Distributor getDistributor() {
    return theDistributor; 
  }
    
  /** a reference to the Cluster **/
  private ClusterServesPlugIn theCluster = null;

  /** let subclasses get ahold of the cluster without having to catch it at
   * load time.  May through a runtime exception if the plugin hasn't been 
   * loaded yet.
   **/
  protected final ClusterServesPlugIn getCluster() {
    if (theCluster != null) {
      return theCluster; 
    } else {
      throw new RuntimeException("PlugIn must be LOADED before getCluster() is defined.");
    }
  }

  // 
  // aliases for Transaction handling 
  //

  /** alias for getSubscriber().openTransaction() **/
  protected final void openTransaction() {
    theSubscriber.openTransaction();
  }

  /** alias for getSubscriber().tryOpenTransaction() **/
  protected final boolean tryOpenTransaction() {
    return theSubscriber.tryOpenTransaction();
  }

  /** alias for getSubscriber().closeTransaction() **/
  protected final void closeTransaction() throws SubscriberException {
    theSubscriber.closeTransaction();
  }
  /** alias for getSubscriber().closeTransaction(boolean) **/
  protected final void closeTransaction(boolean resetp) throws SubscriberException {
    theSubscriber.closeTransaction(resetp);
  }


  //
  // aliases for kicking watchers
  //

  /** storage for wasAwakened. Set/reset by run() **/
  private boolean explicitlyAwakened = false;

  /** true IFF were we awakened explicitly (i.e. we were asked to run
   * even if no subscription activity has happened).
   * The value is valid only while running in the main plugin thread.
   */
  protected boolean wasAwakened() { return explicitlyAwakened; }

  /** For adapter use only **/
  protected final void setAwakened(boolean value) { explicitlyAwakened = value; }

  /** 
   * Hook which allows a plugin thread to request that the
   * primary plugin thread (the execute() method) be called.
   * Generally used when you want the plugin to be stimulated
   * by some non-internal state change ( e.g. when a timer goes off,
   * database activity, offline server activity, etc.)
   *
   * For plugin use only; No longer called by the infrastructure.
   **/
  protected final void wake() {
    theSubscriber.signalClientActivity();
  }

  /** Convenience method to specify given time to stimulate plugin.
   * (based on COUGAAR scenario time). 
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   * @param wakeTime actual scenario time to wake in milliseconds.
   **/ 	
  protected Alarm wakeAt(long wakeTime) { 
    if (wakeTime < theCluster.currentTimeMillis()) {
      System.err.println("\nwakeAt("+wakeTime+") is in the past!");
      Thread.dumpStack();
      wakeTime = theCluster.currentTimeMillis()+1000;
    }
      
    PluginAlarm pa = new PluginAlarm(wakeTime);
    theCluster.addAlarm(pa);
    return pa;
  };

  /** Convenience method to specify period of time to wait before
   * stimulating plugin (based on COUGAAR scenario time).
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   * @param delayTime (Scenario) milliseconds to wait before waking.
   **/
  protected Alarm wakeAfter(long delayTime) { 
    if (delayTime<=0) {
      System.err.println("\nwakeAfter("+delayTime+") is in the past!");
      Thread.dumpStack();
      delayTime=1000;
    }
      
    long absTime = theCluster.currentTimeMillis()+delayTime;
    PluginAlarm pa = new PluginAlarm(absTime);
    theCluster.addAlarm(pa);
    return pa;
  };

  /** like wakeAt() except always in real (wallclock) time.
   **/ 	
  protected Alarm wakeAtRealTime(long wakeTime) { 
    if (wakeTime < System.currentTimeMillis()) {
      System.err.println("\nwakeAtRealTime("+wakeTime+") is in the past!");
      Thread.dumpStack();
      wakeTime = System.currentTimeMillis()+1000;
    }

    PluginAlarm pa = new PluginAlarm(wakeTime);
    theCluster.addRealTimeAlarm(pa);
    return pa;
  };

  /** like wakeAfter() except always in real (wallclock) time.
   **/
  protected Alarm wakeAfterRealTime(long delayTime) { 
    if (delayTime<=0) {
      System.err.println("\nwakeAfterRealTime("+delayTime+") is in the past!");
      Thread.dumpStack();
      delayTime=1000;
    }

    long absTime = System.currentTimeMillis()+delayTime;
    PluginAlarm pa = new PluginAlarm(absTime);
    theCluster.addRealTimeAlarm(pa);
    return pa;
  };


  /** What is the current Scenario time? 
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   **/
  public long currentTimeMillis() {
    return theCluster.currentTimeMillis();
  }

  /** what is the current (COUGAAR) time as a Date object?
   * Note: currentTimeMillis() is preferred, as it gives
   * more control over timezone, calendar, etc.
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   **/
  protected Date getDate() {
    return new Date(currentTimeMillis());
  }

  //
  // aliases for subscriptions
  //


  /** Request a subscription to all objects for which 
   * isMember.execute(object) is true.  The returned Collection
   * is a transactionally-safe set of these objects which is
   * guaranteed not to change out from under you during run()
   * execution.
   * 
   * subscribe() may be called any time after 
   * load() completes.
   *
   * NOTE: we'll probably want a "new things" sort of collection 
   * for expanders.
   * Alias for getSubscriber().subscribe(UnaryPredicate);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember) {
    return theSubscriber.subscribe(isMember);
  }

  /** like subscribe(UnaryPredicate), but allows specification of
   * some other type of Collection object as the internal representation
   * of the collection.
   * Alias for getSubscriber().subscribe(UnaryPredicate, Collection);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember, Collection realCollection){
    return theSubscriber.subscribe(isMember, realCollection);
  }

  /**
   * Alias for getSubscriber().subscribe(UnaryPredicate, boolean);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember, boolean isIncremental) {
    return theSubscriber.subscribe(isMember, isIncremental);
  }
  /**
   * Alias for <code>getSubscriber().subscribe(UnaryPredicate, Collection, boolean);</code>
   * @param isMember a predicate to execute to ascertain
   * membership in the collection of the subscription.
   * @param realCollection a container to hold the contents of the subscription.
   * @param isIncremental should be true if an incremental subscription is desired.
   * An incremental subscription provides access to the incremental changes to the subscription.
   * @return the Subsciption.
   * @see org.cougaar.core.cluster.Subscriber#subscribe
   * @see org.cougaar.core.cluster.Subscription
   **/
  protected final Subscription subscribe(UnaryPredicate isMember, Collection realCollection, boolean isIncremental) {
    return theSubscriber.subscribe(isMember, realCollection, isIncremental);
  }

  /** Issue a query against the logplan.  Similar in function to
   * opening a new subscription, getting the results and immediately
   * closing the subscription, but can be implemented much more efficiently.
   * Note: the initial implementation actually does exactly this.
   **/
  protected final Collection query(UnaryPredicate isMember) {
    /*
      // old implementation
    Subscription s = theSubscriber.subscribe(isMember);
    Collection c = ((CollectionSubscription)s).getCollection();
    theSubscriber.unsubscribe(s);
    return c;
    */
    return theSubscriber.query(isMember);
  }

  /**
   * Cancels the given Subscription which must have been returned by a
   * previous invocation of subscribe().  Alias for
   * <code> getSubscriber().unsubscribe(Subscription)</code>.
   * @param subscription the subscription to cancel
   * @see org.cougaar.core.cluster.Subscriber#unsubscribe
   **/
  protected final void unsubscribe(Subscription subscription) {
    theSubscriber.unsubscribe(subscription);
  }


  // 
  // LDM access
  //
  protected LDMServesPlugIn theLDM = null;

  protected final LDMServesPlugIn getLDM() {
    return theLDM;
  }

  protected RootFactory theLDMF = null;
  protected final RootFactory getFactory() {
    return theLDMF;
  }
  /** @deprecated Use getFactory() */
  protected final RootFactory getLdmFactory() {
    return theLDMF;
  }

  protected final Factory getFactory(String s) {
    return theLDM.getFactory(s);
  }
  
  
  //
  // cluster
  // 

  protected final ClusterIdentifier getClusterIdentifier() {
    return theCluster.getClusterIdentifier();
  }

  //
  // LogPlan changes publishing
  //

  /** alias for getSubscriber().publishAdd(Object) */
  protected final boolean publishAdd(Object o) {
    return theSubscriber.publishAdd(o);
  }
  /** alias for getSubscriber().publishRemove(Object) */
  protected final boolean publishRemove(Object o) {
    return theSubscriber.publishRemove(o);
  }

  /** alias for publishChange(object, null) **/
  protected final boolean publishChange(Object o) {
    return theSubscriber.publishChange(o, null);
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
  protected final boolean publishChange(Object o, Collection changes) {
    return theSubscriber.publishChange(o, changes);
  }
    

  private PlugInDelegate delegate = null;

  /** @return an object that exposes the protected plugin methods
   * as publics.
   **/
  protected final PlugInDelegate getDelegate() {
    if (delegate == null) 
      delegate = createDelegate();
    return delegate;
  }
    
  protected PlugInDelegate createDelegate() {
    return new Delegate();
  }

  //
  // implement PlugInDelegate
  //
  protected class Delegate implements PlugInDelegate {
    public Subscriber getSubscriber() { 
      return theSubscriber;
    }
    public Distributor getDistributor() {
      return theDistributor;
    }
    public ClusterServesPlugIn getCluster() {
      return theCluster;
    }
    public LDMServesPlugIn getLDM() {
      return theLDM;
    }
    public RootFactory getFactory() {
      return theLDMF;
    }
    /** @deprecated use getFactory() **/
    public RootFactory getLdmFactory() {
      return theLDMF;
    }
    public Factory getFactory(String s) {
      return theLDM.getFactory(s);
    }
    public ClusterIdentifier getClusterIdentifier() {
      return theCluster.getClusterIdentifier();
    }
    public MetricsSnapshot getMetricsSnapshot() {
      return theCluster.getMetricsSnapshot();
    }
    public void openTransaction() {
      theSubscriber.openTransaction();
    }
    public boolean tryOpenTransaction() {
      return theSubscriber.tryOpenTransaction();
    }
    public void closeTransaction() throws SubscriberException {
      theSubscriber.closeTransaction();
    }
    public void closeTransaction(boolean resetp) throws SubscriberException {
      theSubscriber.closeTransaction(resetp);
    }

    public boolean wasAwakened() { return PlugInAdapter.this.wasAwakened(); }

    public void wake() {
      PlugInAdapter.this.wake();
    }
    public Alarm wakeAt(long n) {
      return PlugInAdapter.this.wakeAt(n);
    }
    public Alarm wakeAfter(long n) {
      return PlugInAdapter.this.wakeAfter(n);
    }
    public Alarm wakeAtRealTime(long n) {
      return PlugInAdapter.this.wakeAtRealTime(n);
    }
    public Alarm wakeAfterRealTime(long n) {
      return PlugInAdapter.this.wakeAfterRealTime(n);
    }
    public long currentTimeMillis() {
      return theCluster.currentTimeMillis();
    }
    public Date getDate() {
      return new Date(currentTimeMillis());
    }

    public Subscription subscribe(UnaryPredicate isMember) {
      return theSubscriber.subscribe(isMember);
    }
    public Subscription subscribe(UnaryPredicate isMember, Collection realCollection) {
      return theSubscriber.subscribe(isMember, realCollection);
    }
    public Subscription subscribe(UnaryPredicate isMember, boolean isIncremental) {
      return theSubscriber.subscribe(isMember,isIncremental);
    }
    public Subscription subscribe(UnaryPredicate isMember, Collection realCollection, boolean isIncremental) {
      return theSubscriber.subscribe(isMember, realCollection, isIncremental);
    }
    public void unsubscribe(Subscription collection) {
      theSubscriber.unsubscribe(collection);
    }
    public Collection query(UnaryPredicate isMember) {
      return PlugInAdapter.this.query(isMember);
    }

    public void publishAdd(Object o) {
      theSubscriber.publishAdd(o);
    }
    public void publishRemove(Object o) {
      theSubscriber.publishRemove(o);
    }
    public void publishChange(Object o) {
      theSubscriber.publishChange(o, null);
    }
    public void publishChange(Object o, Collection changes) {
      theSubscriber.publishChange(o, changes);
    }
    public Collection getParameters() {
      return parameters;
    }
    public boolean didRehydrate() {
      return didRehydrate(getSubscriber());
    }
    public boolean didRehydrate(Subscriber subscriber) {
      return subscriber.didRehydrate();
    }

    public boolean claim(Object o) {
      return PlugInAdapter.this.claim(o);
    }
    public void unclaim(Object o) {
      PlugInAdapter.this.unclaim(o);
    }
  }

  public class PluginAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    public PluginAlarm (long expirationTime) {
      expiresAt = expirationTime;
    }
    public long getExpirationTime() { return expiresAt; }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        theSubscriber.signalClientActivity();
      }
    }
    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired=true;
      return was;
    }
    public String toString() {
      return "<PlugInAlarm "+expiresAt+
        (expired?"(Expired) ":" ")+
        "for "+PlugInAdapter.this.toString()+">";
    }
  }

  public boolean didRehydrate() {
    return getSubscriber().didRehydrate();
  }

  public boolean didRehydrate(Subscriber subscriber) {
    return subscriber.didRehydrate();
  }

  /** Attempt to stake a claim on a logplan object, essentially telling 
   * everyone else that you and only you will be disposing, modifying, etc.
   * it.
   * Calls Claimable.tryClaim if the object is Claimable.
   * @return true IFF success.
   **/
  protected boolean claim(Object o) {
    if (o instanceof Claimable) {
      return ((Claimable)o).tryClaim(theSubscriber);
    } else {
      return false;
    }
  }
      
  /** Release an existing claim on a logplan object.  This is likely to
   * thow an exception if the object had not previously been (successfully) 
   * claimed by this plugin.
   **/
  protected void unclaim(Object o) {
    ((Claimable) o).resetClaim(theSubscriber);
  }

  // 
  // threading model
  //

  private Threading threadingModel = null;
  
  private void setThreadingModel(Threading t) {
    threadingModel = t;
  }

  protected final Threading getThreadingModel() { 
    return threadingModel;
  }
  
  public final static int UNSPECIFID_THREAD = -1;
  public final static int NO_THREAD = 0;
  public final static int SHARED_THREAD = 1;
  public final static int SINGLE_THREAD = 2;
  public final static int ONESHOT_THREAD = 3;

  private int threadingChoice = UNSPECIFID_THREAD;

  /** Set the current choice of threading model.  Will have no effect if
   * the threading model has already been acted on.
   **/
  protected final void setThreadingChoice(int m) {
    if (threadingModel != null) 
      throw new IllegalArgumentException("Too late to select threading model.");
    threadingChoice = m;
  }

  /** @deprecated use setThreadingChoice instead. **/
  protected final void chooseThreadingModel(int m) {
    setThreadingChoice(m);
  }

  /** @return the current choice of threading model.  **/
  protected final int getThreadingChoice() {
    return threadingChoice;
  }

  /** return the default threading model for this class.
   **/
  protected final int getDefaultThreadingChoice() {
    return SHARED_THREAD;
  }

  /** create a Threading model object as specified by the plugin.
   * The default implementation creates a Threading object
   * based on the value of threadingChoice.
   * The default choice is to use a SharedThreading model, which
   * shares thread of execution with others of the same sort in
   * the cluster.
   * Most plugins can ignore this altogether.  Most that
   * want to select different behavior should
   * call chooseThreadingModel() in their constructer.
   * PlugIns which implement their own threading model
   * will need to override createThreadingModel.
   * createThreadingModel is called late in PlugInAdapter.load(). 
   * if an extending plugin class wishes to examine or alter
   * the threading model object, it will be available only when 
   * PlugInAdapter.load() returns, which is usually called by
   * the extending plugin classes overriding load() method.
   * The constructed Threading object is initialized by
   * PlugInAdapter.start().
   **/
  protected Threading createThreadingModel() {
    Threading t;
    int choice = getThreadingChoice();
    if (choice == UNSPECIFID_THREAD) 
      choice = getDefaultThreadingChoice();
    switch (threadingChoice) {
    case NO_THREAD:
      t = new NoThreading();
      break;
    case SHARED_THREAD: 
      t = new SharedThreading();
      break;
    case SINGLE_THREAD:
      t = new SingleThreading();
      break;
    case ONESHOT_THREAD:
      t = new OneShotThreading();
      break;
    default:
      throw new RuntimeException("Invalid Threading model "+threadingChoice);
    }
    return t;
  }

  private void startThreadingModel() {
    try {
      threadingModel.initialize();
      threadingModel.load(getCluster());
      threadingModel.start();
    } catch (RuntimeException e) {
      System.err.println("Caught exception during threadingModel initialization: "+e);
      e.printStackTrace();
    }
  }

  protected abstract class Threading implements GenericStateModel {
    public void initialize() {}
    /** the argument passed to load is a ClusterServesPlugIn **/
    public void load(Object o) {}
    public void start() {}
    public void suspend() {}
    public void resume() {}
    public void stop() {}
    public void halt() {}
    public void unload() {}
    public int getState() { 
      return UNINITIALIZED; 
    }
    public String toString() {
      return getClusterIdentifier()+"/"+(PlugInAdapter.this);
    }
  }

  /** up to the class to implement what it needs **/
  protected class NoThreading extends Threading {
  }
    
  /** prerun only: cycle will never be called. **/
  protected class OneShotThreading extends Threading {
    public OneShotThreading() {}
    public void start() {
      prerun1();
    }
  }

  /** shares a Thread with other SharedThreading plugins in the same cluster **/
  protected class SharedThreading extends Threading implements ScheduleablePlugIn {
    public SharedThreading() {}
    public void start() {
      getCluster().schedulePlugIn(this);
      prerun1();
    }

    //
    // implementation of ScheduleablePlugIn API 
    //

    public final void addExternalActivityWatcher(SubscriptionWatcher watcher) {
      getSubscriber().registerInterest(watcher);
    }

    public final void externalCycle(boolean wasExplicit) {
      setAwakened(wasExplicit);
      cycle1();
    }
  }

  /** has its own Thread **/
  protected class SingleThreading extends Threading implements Runnable {
    /** a reference to personal Thread which each PlugIn runs in **/
    private Thread myThread = null;
    /** our subscription watcher **/
    private SubscriptionWatcher waker = null;
    
    public SingleThreading() {}

    private int priority = Thread.NORM_PRIORITY;

    /** plugins and subclasses may set the Thread priority to 
     * a value lower than standard.  Requests to raise the priority
     * are ignored as are all requests after start()
     * Note that the default priority is one level below the
     * usual java priority - that is one level below where the
     * infrastructure runs.
     **/
    public void setPriority(int newPriority) {
      if (newPriority<priority) {
        priority = newPriority;
      }
    }
    
    private boolean isYielding = true;

    /** If isYielding is true, the plugin will force a thread yield
     * after each call to cycle().  This is on by default since plugins
     * generally need reaction from infrastructure and other plugins
     * to progress.
     * This may be set at any time, even though the effect is only periodic.
     * Most plugins would want to (re)set this value at initialization.
     **/
    public void setIsYielding(boolean v) {
      isYielding = v;
    }

    public void load(Object object) {
      waker = getSubscriber().registerInterest();
    }
    public void start() {
      myThread = new Thread(this, "Plugin/"+getClusterIdentifier()+"/"+(PlugInAdapter.this));
      myThread.setPriority(priority);
      myThread.start();
    }

    private boolean suspendRequest = false;
    public void suspend() { 
      if (myThread != null) {
        suspendRequest = true;
        signalStateChange();
      }
    }

    private boolean resumeRequest = false;
    public void resume() {  
      if (myThread != null) {
        resumeRequest = true;
        signalStateChange();
      }
    }
    private boolean stopRequest = false;
    public void stop() {
      if (myThread != null) {
        stopRequest = true;
        signalStateChange();
      }
    }

    private void signalStateChange() {
      if (waker != null) {
        waker.signalNotify(waker.INTERNAL);
      }
    }

    private boolean isRunning = true;
    private boolean isActive = true;
    public final void run() {
      prerun1();                 // plugin first time through
      while (isRunning) {
        boolean xwakep = waker.waitForSignal();
        setAwakened(xwakep);
        if (suspendRequest) {
          suspendRequest = false;
          isActive = false;
        }
        if (resumeRequest) {
          resumeRequest = false;
          isActive = true;
        }
        if (stopRequest) {
          stopRequest = false;
          isRunning = false;
          isActive = false;
        }
        if (isActive) {
          cycle1();                // do work
          if (isYielding)
            Thread.yield();
        }
      }
    }
  }

  /** Called by all the standard Threading models to instruct the plugin 
   * to do plugin-specific initializations.  E.g. setup its subscriptions, etc.
   *
   * Non-standard threading models are encouraged but not required to use 
   * this method to retain compatability.
   **/
  private void prerun1() {
    SubscriptionClient.current.set(this);
    prerun();
    SubscriptionClient.current.set(null);
  }
  protected void prerun() { }

  /** Called by all the standard Threading models (except for OneShotThreading)
   * each time there is work to be done.
   *
   * Non-standard threading models are encouraged but not required to use 
   * this method to retain compatability.
   **/
  private void cycle1() {
    SubscriptionClient.current.set(this);
    cycle();
    SubscriptionClient.current.set(null);
  }
  protected void cycle() {}

}

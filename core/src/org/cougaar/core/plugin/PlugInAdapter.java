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
import org.cougaar.util.*;

import org.cougaar.core.cluster.*;

import org.cougaar.core.cluster.MetricsSnapshot;
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.core.plugin.PlugInServesCluster;
import org.cougaar.core.plugin.ScheduleablePlugIn;

import org.cougaar.core.cluster.AlarmService;
import org.cougaar.core.cluster.AlarmServiceProvider;
import org.cougaar.core.cluster.ClusterServesPlugIn;
import org.cougaar.core.cluster.Claimable;
import org.cougaar.core.cluster.Subscriber;
import org.cougaar.core.cluster.CollectionSubscription;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.Distributor;
import org.cougaar.core.cluster.SubscriptionWatcher;
import org.cougaar.core.cluster.SubscriberException;
import org.cougaar.core.cluster.Alarm;
import org.cougaar.core.cluster.MetricsSnapshot;
import org.cougaar.core.cluster.MetricsService;
import org.cougaar.core.cluster.ClusterIdentifier;

import org.cougaar.core.plugin.PlugInServesCluster;

import org.cougaar.util.GenericStateModel;
import org.cougaar.util.StateModelException;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.domain.planning.ldm.*;
import org.cougaar.core.component.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.cluster.*;

public abstract class PlugInAdapter
  extends GenericStateModelAdapter
  implements PlugInServesCluster, BlackboardClient, ParameterizedPlugIn, PluginBase
{

  /** keep this around for compatability with old plugins **/
  protected RootFactory theLDMF = null;

  protected LDMServesPlugIn theLDM = null;

  // 
  // constructor
  //

  public PlugInAdapter() {
  }

  private PluginBindingSite bindingSite = null;

  public final void setBindingSite(BindingSite bs) {
    if (bs instanceof PluginBindingSite) {
      bindingSite = (PluginBindingSite) bs;
    } else {
      throw new RuntimeException("Tried to load "+this+" into "+bs);
    }
  }

  protected final PluginBindingSite getBindingSite() {
    return bindingSite;
  }

  //
  // extra services
  //

  private LDMService ldmService = null;
  public final void setLDMService(LDMService s) {
    ldmService = s;
  }
  protected final LDMService getLDMService() {
    return ldmService;
  }

  // metrics service

  private MetricsService metricsService = null;
  public final void setMetricsService(MetricsService s) {
    metricsService = s;
  }
  protected final MetricsService getMetricsService() {
    return metricsService;
  }

  /** @deprecated supply a <code>MetricsSnapshot</code> object **/
  protected final MetricsSnapshot getMetricsSnapshot() {
    if (metricsService != null) {
      return metricsService.getMetricsSnapshot();
    } else {
      return null;
    }
  }

  protected final MetricsSnapshot getMetricsSnapshot(MetricsSnapshot ms, boolean resetMsgStats) {
    if (metricsService != null) {
      return metricsService.getMetricsSnapshot(ms, resetMsgStats);
    } else {
      return null;
    }
  }

  // alarm service
  private AlarmService alarmService = null;
  public final void setAlarmService(AlarmService s) {
    alarmService = s;
  }
  protected final AlarmService getAlarmService() {
    return alarmService;
  }

  // demo control service
  private DemoControlService demoControlService = null;
  public final void setDemoControlService(DemoControlService dcs) {
    demoControlService = dcs;
  }
  protected final DemoControlService getDemoControlService() {
    return demoControlService;
  }

  // shared threading service
  private SharedThreadingService sharedThreadingService = null;
  public final void setSharedThreadingService(SharedThreadingService sts) {
    sharedThreadingService = sts;
  }
  protected final SharedThreadingService getSharedThreadingService() {
    return sharedThreadingService;
  }

  //UID service
  private UIDService theUIDService = null;
  public void setUIDService(UIDService us) {
    theUIDService = us;
  }
  public final UIDService getUIDService() {
    return theUIDService;
  }

  //Domain service (factory service piece of old LDM)
  private DomainService theDomainService = null;
  public void setDomainService(DomainService ds) {
    theDomainService = ds;
  }
  public final DomainService getDomainService() {
    return theDomainService;
  }

  //PrototypeRegistryService (prototype/property piece of old LDM)
  private PrototypeRegistryService thePrototypeRegistryService = null;
  public void setPrototypeRegistryService(PrototypeRegistryService prs) {
    thePrototypeRegistryService = prs;
  }
  public final PrototypeRegistryService getPrototypeRegistryService() {
    return thePrototypeRegistryService;
  }

  //
  // Implement (some of) BlackboardClient
  //
  protected String blackboardClientName = null;

  public String getBlackboardClientName() {
    if (blackboardClientName == null) {
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
      blackboardClientName = buf.substring(0);
    }
    return blackboardClientName;
  }

  public String toString() {
    return getBlackboardClientName();
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

  
  // Many plugins expect a non-null value
  private Vector parameters = new Vector(0);

  public void setParameter(Object param) {
    if (param != null) {
      if (param instanceof Vector) {
        parameters = (Vector) param;
      } else {
        System.err.println("Warning: "+this+" initialized with non-vector parameter "+param);
      }
    }
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

  /** Component Model <em>AND</em> GenericStateModel initialization **/
  public void initialize() {
    super.initialize();         // uninitialized->unloaded (defined in GSMAdapter)
  }

  public void load() throws StateModelException {
    super.load();
    load(null);
  }

  /** Load the plugin.  No longer pays any attention to the passed object,
   * as it will now always be null.
   **/
  public void load(Object object) {
    setThreadingChoice(getThreadingChoice()); // choose the threading model
    theLDM = getLDMService().getLDM();
    theLDMF = getDomainService().getFactory();

    if (this instanceof PrototypeProvider) {
      getPrototypeRegistryService().addPrototypeProvider((PrototypeProvider)this);
    }
    if (this instanceof PropertyProvider) {
      getPrototypeRegistryService().addPropertyProvider((PropertyProvider)this);
    }
    if (this instanceof LatePropertyProvider) {
      getPrototypeRegistryService().addLatePropertyProvider((LatePropertyProvider)this);
    }
    
    //ServiceBroker sb = getBindingSite().getServiceBroker();

    // fire up the threading model
    setThreadingModel(createThreadingModel());
  }

  /** */
  public void start() throws StateModelException {
    super.start();
    startThreadingModel();
  }


  public void suspend() throws StateModelException {
    super.suspend();
    threadingModel.suspend();
  }

  public void resume() throws StateModelException {
    super.resume();
    threadingModel.resume();
  }

  //
  // Customization of PlugInAdapter
  //


  /** Was a method of specifying the class of Subscriber to use.  This is 
   * now a function of the Binder, so is no longer appropriate.
   */
  protected BlackboardService constructSubscriber(Distributor distributor) {
    throw new RuntimeException("Dont call me!");
  }

  public int getSubscriptionCount() {
    return getBlackboardService().getSubscriptionCount();
  }
  
  public int getSubscriptionSize() {
    return getBlackboardService().getSubscriptionSize();
  }

  public int getPublishAddedCount() {
    return getBlackboardService().getPublishAddedCount();
  }

  public int getPublishChangedCount() {
    return getBlackboardService().getPublishChangedCount();
  }

  public int getPublishRemovedCount() {
    return getBlackboardService().getPublishRemovedCount();
  }

  //
  // Ivars and accessor methods
  //

  //Blackboard service
  private BlackboardService theBlackboard = null;

  public void setBlackboardService(BlackboardService s) {
    theBlackboard = s;
  }

  /** Safely return our BlackboardService 
   * PlugIn.load() must have completed in order 
   * for the value to be defined.
   * This method is public as it is part of the API required by PluginBindingSite to
   * support the threading models.
   **/
  public final BlackboardService getBlackboardService() {
    return theBlackboard;
  }

  /** Safely return our Distribution service (Distributor).
   * load() must have completed for this to 
   * be defined.
   * @deprecated The Distributor is no longer directly accessible to plugins: This method
   * returns null.
   *
   **/
  protected Distributor getDistributor() {
    return null;
  }
    
  /** let subclasses get ahold of the cluster without having to catch it at
   * load time.  May throw a runtime exception if the plugin hasn't been 
   * loaded yet.
   * @deprecated This method no longer allows direct access to the Cluster (Agent): instead
   * it will always return null.
   **/
  protected final ClusterServesPlugIn getCluster() {
    return dummyCluster;
  }

  private ClusterServesPlugIn dummyCluster = new ClusterServesPlugIn() {
      // real ones
      public ConfigFinder getConfigFinder() { return ((PluginBindingSite) getBindingSite()).getConfigFinder(); }
      public ClusterIdentifier getClusterIdentifier() { return PlugInAdapter.this.getClusterIdentifier();}
      public UIDServer getUIDServer() { return PlugInAdapter.this.getUIDServer(); }
      public LDMServesPlugIn getLDM() { return PlugInAdapter.this.getLDM(); }
      
      // evil ones
      public Distributor getDistributor() { throw new RuntimeException("Should not be called"); }
      public void schedulePlugIn(ScheduleablePlugIn p) {throw new RuntimeException("Should not be called");}

      // DemoControl service
      public void setTime(long time) { getDemoControlService().setTime(time);}
      public void setTime(long time, boolean foo) { getDemoControlService().setTime(time,foo);}
      public void setTimeRate(double rate) { getDemoControlService().setTimeRate(rate); }
      public void advanceTime(long period) { getDemoControlService().advanceTime(period); }
      public void advanceTime(long period, boolean foo) { getDemoControlService().advanceTime(period, foo); }
      public void advanceTime(long period, double rate) { getDemoControlService().advanceTime(period, rate); }
      public void advanceTime(ExecutionTimer.Change[] changes) { getDemoControlService().advanceTime(changes); }
      public double getExecutionRate() { return getDemoControlService().getExecutionRate(); }

      // alarm service
      public long currentTimeMillis() { return getAlarmService().currentTimeMillis(); }
      public void addAlarm(Alarm alarm) {getAlarmService().addAlarm(alarm);}
      public void addRealTimeAlarm(Alarm a) {getAlarmService().addRealTimeAlarm(a);}

      // metrics service
      public MetricsSnapshot getMetricsSnapshot() { return getMetricsService().getMetricsSnapshot(); }
      public MetricsSnapshot getMetricsSnapshot(MetricsSnapshot ms, boolean resetMsgStats) { return getMetricsService().getMetricsSnapshot(ms, resetMsgStats); }

      // ??
      public java.sql.Connection getDatabaseConnection(Object locker) {throw new RuntimeException("Should not be called");}
      public void releaseDatabaseConnection(Object locker) {throw new RuntimeException("Should not be called");}
    };

  protected ConfigFinder getConfigFinder() {
    return ((PluginBindingSite) getBindingSite()).getConfigFinder();
  }

  // 
  // aliases for Transaction handling 
  //

  protected final void openTransaction() {
    getBlackboardService().openTransaction();
  }

  protected final boolean tryOpenTransaction() {
    return getBlackboardService().tryOpenTransaction();
  }

  protected final void closeTransaction() throws SubscriberException {
    getBlackboardService().closeTransaction();
  }
  
  protected final void closeTransaction(boolean resetp) throws SubscriberException {
    getBlackboardService().closeTransaction(resetp);
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
  public final void setAwakened(boolean value) { explicitlyAwakened = value; }

  /** 
   * Hook which allows a plugin thread to request that the
   * primary plugin thread (the execute() method) be called.
   * Generally used when you want the plugin to be stimulated
   * by some non-internal state change ( e.g. when a timer goes off,
   * database activity, offline server activity, etc.)
   *
   * For plugin use only; No longer called by the infrastructure.
   **/
  public final void wake() {
    getBlackboardService().signalClientActivity();
  }


  /** Convenience method to specify given time to stimulate plugin.
   * (based on COUGAAR scenario time). 
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   * @param wakeTime actual scenario time to wake in milliseconds.
   **/ 	
  public Alarm wakeAt(long wakeTime) { 
    if (wakeTime < getAlarmService().currentTimeMillis()) {
      System.err.println("\nwakeAt("+wakeTime+") is in the past!");
      Thread.dumpStack();
      wakeTime = getAlarmService().currentTimeMillis()+1000;
    }
      
    PluginAlarm pa = new PluginAlarm(wakeTime);
    getAlarmService().addAlarm(pa);
    return pa;
  };

  /** Convenience method to specify period of time to wait before
   * stimulating plugin (based on COUGAAR scenario time).
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   * @param delayTime (Scenario) milliseconds to wait before waking.
   **/
  public Alarm wakeAfter(long delayTime) { 
    if (delayTime<=0) {
      System.err.println("\nwakeAfter("+delayTime+") is in the past!");
      Thread.dumpStack();
      delayTime=1000;
    }
      
    long absTime = getAlarmService().currentTimeMillis()+delayTime;
    PluginAlarm pa = new PluginAlarm(absTime);
    getAlarmService().addAlarm(pa);
    return pa;
  };

  /** like wakeAt() except always in real (wallclock) time.
   **/ 	
  public Alarm wakeAtRealTime(long wakeTime) { 
    if (wakeTime < System.currentTimeMillis()) {
      System.err.println("\nwakeAtRealTime("+wakeTime+") is in the past!");
      Thread.dumpStack();
      wakeTime = System.currentTimeMillis()+1000;
    }

    PluginAlarm pa = new PluginAlarm(wakeTime);
    getAlarmService().addRealTimeAlarm(pa);
    return pa;
  };

  /** like wakeAfter() except always in real (wallclock) time.
   **/
  public Alarm wakeAfterRealTime(long delayTime) { 
    if (delayTime<=0) {
      System.err.println("\nwakeAfterRealTime("+delayTime+") is in the past!");
      Thread.dumpStack();
      delayTime=1000;
    }

    long absTime = System.currentTimeMillis()+delayTime;
    PluginAlarm pa = new PluginAlarm(absTime);
    getAlarmService().addRealTimeAlarm(pa);
    return pa;
  };


  /** What is the current Scenario time? 
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   **/
  public long currentTimeMillis() {
    return getAlarmService().currentTimeMillis();
  }

  /** what is the current (COUGAAR) time as a Date object?
   * Note: currentTimeMillis() is preferred, as it gives
   * more control over timezone, calendar, etc.
   * Note that this facility is not appropriate to use for 
   * load-balancing purposes, as scenario time is discontinuous
   * and may even stop.
   **/
  public Date getDate() {
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
   * Alias for getBlackboardService().subscribe(UnaryPredicate);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember) {
    return getBlackboardService().subscribe(isMember);
  }

  /** like subscribe(UnaryPredicate), but allows specification of
   * some other type of Collection object as the internal representation
   * of the collection.
   * Alias for getBlackboardService().subscribe(UnaryPredicate, Collection);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember, Collection realCollection){
    return getBlackboardService().subscribe(isMember, realCollection);
  }

  /**
   * Alias for getBlackboardService().subscribe(UnaryPredicate, boolean);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember, boolean isIncremental) {
    return getBlackboardService().subscribe(isMember, isIncremental);
  }
  /**
   * Alias for <code>getBlackboardService().subscribe(UnaryPredicate, Collection, boolean);</code>
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
    return getBlackboardService().subscribe(isMember, realCollection, isIncremental);
  }

  /** Issue a query against the logplan.  Similar in function to
   * opening a new subscription, getting the results and immediately
   * closing the subscription, but can be implemented much more efficiently.
   * Note: the initial implementation actually does exactly this.
   **/
  protected final Collection query(UnaryPredicate isMember) {
    return getBlackboardService().query(isMember);
  }

  /**
   * Cancels the given Subscription which must have been returned by a
   * previous invocation of subscribe().  Alias for
   * <code> getBlackboardService().unsubscribe(Subscription)</code>.
   * @param subscription the subscription to cancel
   * @see org.cougaar.core.cluster.Subscriber#unsubscribe
   **/
  protected final void unsubscribe(Subscription subscription) {
    getBlackboardService().unsubscribe(subscription);
  }


  // 
  // LDM access
  //

  protected final LDMServesPlugIn getLDM() {
    return theLDM;
  }

  protected final RootFactory getFactory() {
    return theLDMF;
  }
  /** @deprecated Use getFactory() */
  protected final RootFactory getLdmFactory() {
    return getFactory();
  }

  protected final Factory getFactory(String s) {
    return getDomainService().getFactory(s);
  }
  
  
  //
  // cluster
  // 

  protected final ClusterIdentifier getAgentIdentifier() {
    return ((PluginBindingSite) getBindingSite()).getAgentIdentifier();
  }
  protected final ClusterIdentifier getClusterIdentifier() {
    return getAgentIdentifier();
  }

  protected final UIDServer getUIDServer() {
    return getUIDService();
  }

  //
  // LogPlan changes publishing
  //

  protected final boolean publishAdd(Object o) {
    return getBlackboardService().publishAdd(o);
  }
  protected final boolean publishRemove(Object o) {
    return getBlackboardService().publishRemove(o);
  }
  protected final boolean publishChange(Object o) {
    return getBlackboardService().publishChange(o, null);
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
    return getBlackboardService().publishChange(o, changes);
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
    public BlackboardService getBlackboardService() { 
      return theBlackboard;
    }
    public BlackboardService getSubscriber() { 
      return theBlackboard;
    }
    public Distributor getDistributor() {
      throw new RuntimeException("Delegate.getDistributor() no longer allowed");
    }
    public ClusterServesPlugIn getCluster() {
      return PlugInAdapter.this.getCluster();
    }
    public LDMServesPlugIn getLDM() {
      return getLDMService().getLDM();
    }
    public RootFactory getFactory() {
      return getDomainService().getFactory();
    }
    /** @deprecated use getFactory() **/
    public RootFactory getLdmFactory() {
      return getDomainService().getFactory();
    }
    public Factory getFactory(String s) {
      return getDomainService().getFactory(s);
    }
    public ClusterIdentifier getClusterIdentifier() {
      return ((PluginBindingSite) getBindingSite()).getAgentIdentifier();
    }
    public MetricsSnapshot getMetricsSnapshot() {
      return getMetricsService().getMetricsSnapshot();
    }
    public MetricsSnapshot getMetricsSnapshot(MetricsSnapshot ms, boolean resetMsgStats) {
      return getMetricsService().getMetricsSnapshot(ms, resetMsgStats);
    }
    public void openTransaction() {
      getBlackboardService().openTransaction();
    }
    public boolean tryOpenTransaction() {
      return getBlackboardService().tryOpenTransaction();
    }
    public void closeTransaction() throws SubscriberException {
      getBlackboardService().closeTransaction();
    }
    public void closeTransaction(boolean resetp) throws SubscriberException {
      getBlackboardService().closeTransaction(resetp);
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
      return getAlarmService().currentTimeMillis();
    }
    public Date getDate() {
      return new Date(currentTimeMillis());
    }

    public Subscription subscribe(UnaryPredicate isMember) {
      return getBlackboardService().subscribe(isMember);
    }
    public Subscription subscribe(UnaryPredicate isMember, Collection realCollection) {
      return getBlackboardService().subscribe(isMember, realCollection);
    }
    public Subscription subscribe(UnaryPredicate isMember, boolean isIncremental) {
      return getBlackboardService().subscribe(isMember,isIncremental);
    }
    public Subscription subscribe(UnaryPredicate isMember, Collection realCollection, boolean isIncremental) {
      return getBlackboardService().subscribe(isMember, realCollection, isIncremental);
    }
    public void unsubscribe(Subscription collection) {
      getBlackboardService().unsubscribe(collection);
    }
    public Collection query(UnaryPredicate isMember) {
      return PlugInAdapter.this.query(isMember);
    }

    public void publishAdd(Object o) {
      getBlackboardService().publishAdd(o);
    }
    public void publishRemove(Object o) {
      getBlackboardService().publishRemove(o);
    }
    public void publishChange(Object o) {
      getBlackboardService().publishChange(o, null);
    }
    public void publishChange(Object o, Collection changes) {
      getBlackboardService().publishChange(o, changes);
    }
    public Collection getParameters() {
      return parameters;
    }
    public boolean didRehydrate() {
      return getBlackboardService().didRehydrate();
    }
    public boolean didRehydrate(BlackboardService subscriber) {
      return subscriber.didRehydrate();
    }

    public boolean claim(Object o) {
      return PlugInAdapter.this.claim(o);
    }
    public void unclaim(Object o) {
      PlugInAdapter.this.unclaim(o);
    }
  }

  public boolean didRehydrate() {
    return getBlackboardService().didRehydrate();
  }

  public boolean didRehydrate(BlackboardService subscriber) {
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
      return ((Claimable)o).tryClaim(getBlackboardService());
    } else {
      return false;
    }
  }
      
  /** Release an existing claim on a logplan object.  This is likely to
   * thow an exception if the object had not previously been (successfully) 
   * claimed by this plugin.
   **/
  protected void unclaim(Object o) {
    ((Claimable) o).resetClaim(getBlackboardService());
  }

  // 
  // threading model
  //

  /** called from PluginBinder **/
  public void plugin_prerun() {
    try {
      //start(); // just in case..  ACK! NO!
      BlackboardClient.current.set(this);
      prerun();
    } finally {
      BlackboardClient.current.set(null);
    }
  }

  /** override to define prerun behavior **/
  protected void prerun() { }

  /** called from PluginBinder **/
  public void plugin_cycle() {
    try {
      BlackboardClient.current.set(this);
      cycle();
    } finally {
      BlackboardClient.current.set(null);
    }
  }

  /** override to define cycle behavior **/
  protected void cycle() { }


  // 
  // compatability methods
  //
  
  /** alias for getBlackboardService **/
  protected BlackboardService getSubscriber() {
    return getBlackboardService();
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
        getBlackboardService().signalClientActivity();
      }
    }
    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired=true;
      return was;
    }
    public String toString() {
      return "<PluginAlarm "+expiresAt+
        (expired?"(Expired) ":" ")+
        "for "+PlugInAdapter.this.toString()+">";
    }
  }


  //
  // threading model support
  // 

  private Threading threadingModel = null;
  
  protected final void setThreadingModel(Threading t) {
    threadingModel = t;
  }

  protected final Threading getThreadingModel() { 
    return threadingModel;
  }
  
  public final static int UNSPECIFIED_THREAD = -1;
  public final static int NO_THREAD = 0;
  public final static int SHARED_THREAD = 1;
  public final static int SINGLE_THREAD = 2;
  public final static int ONESHOT_THREAD = 3;

  private int threadingChoice = UNSPECIFIED_THREAD;

  /** Set the current choice of threading model.  Will have no effect if
   * the threading model has already been acted on.
   **/
  protected final void setThreadingChoice(int m) {
    if (threadingModel != null) 
      throw new IllegalArgumentException("Too late to select threading model for "+this);
    threadingChoice = m;
  }

  /** Set the current choice of threading model.  Will have no effect if
   * the threading model has already been acted on.
   * @deprecated call PluginBindingSite.setThreadingChoice(m) instead.
   **/
  protected final void chooseThreadingModel(int m) {
    setThreadingChoice(m);
  }

  /** @return the current choice of threading model.  **/
  protected final int getThreadingChoice() {
    return threadingChoice;
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
   * createThreadingModel is called late in PluginBinder.load(). 
   * if an extending plugin class wishes to examine or alter
   * the threading model object, it will be available only when 
   * PluginBinder.load() returns, which is usually called by
   * the extending plugin classes overriding load() method.
   * The constructed Threading object is initialized by
   * PluginBinder.start().
   **/
  protected Threading createThreadingModel() {
    Threading t;
    switch (getThreadingChoice()) {
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
      throw new RuntimeException("Invalid Threading model "+getThreadingChoice());
    }
    return t;
  }

  public void startThreadingModel() {
    try {
      threadingModel.initialize();
      threadingModel.load();
      threadingModel.start();
    } catch (RuntimeException e) {
      System.err.println("Caught exception during threadingModel initialization: "+e);
      e.printStackTrace();
    }
  }


  protected abstract class Threading implements GenericStateModel {
    public void initialize() {}
    /** the argument passed to load is a ClusterServesPlugIn **/
    public void load() {}
    public void start() {}
    public void suspend() {}
    public void resume() {}
    public void stop() {}
    public void halt() {}
    public void unload() {}
    public int getModelState() { 
      return UNINITIALIZED; 
    }
    public String toString() {
      return getAgentIdentifier()+"/"+PlugInAdapter.this;
    }
  }

  /** up to the class to implement what it needs **/
  protected class NoThreading extends Threading {
  }
    
  /** prerun only: cycle will never be called. **/
  protected class OneShotThreading extends Threading {
    public OneShotThreading() {}
    public void start() {
      plugin_prerun();
    }
  }

  /** shares a Thread with other SharedThreading plugins in the same cluster **/
  protected class SharedThreading extends Threading implements ScheduleablePlugIn  {
    public void start() {
      getSharedThreadingService().registerPlugIn(this);
      plugin_prerun();
    }

    //
    // implementation of ScheduleablePlugIn API 
    //

    public void addExternalActivityWatcher(SubscriptionWatcher watcher) { 
     getBlackboardService().registerInterest(watcher);
    }

    public final void externalCycle(boolean wasExplicit) {
      setAwakened(wasExplicit);
      plugin_cycle();
    }

  }

  /** has its own Thread **/
  protected class SingleThreading extends Threading implements Runnable {
    /** a reference to personal Thread which each PlugIn runs in **/
    private Thread myThread = null;
    /** our subscription watcher **/
    private SubscriptionWatcher waker = null;
    private static final int STOPPED = 0;
    private static final int SUSPENDED = 1;
    private static final int RUNNING = 2;
    private int state = STOPPED;
    private boolean firstRun = true;
    
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

    public void load() {
      setWaker(getBlackboardService().registerInterest());
    }

    public void unload() {
      getBlackboardService().unregisterInterest(getWaker());
    }

    public synchronized void start() {
      if (state != STOPPED) throw new RuntimeException("Not stopped");
      state = RUNNING;
      firstRun = true;
      startThread();
    }

    public synchronized void suspend() {
      if (state != RUNNING) throw new RuntimeException("Not running");
      state = SUSPENDED;
      stopThread();
    }

    public synchronized void resume() {
      if (state != SUSPENDED) throw new RuntimeException("Not suspended");
      state = RUNNING;
      startThread();
    }

    public synchronized void stop() {
      if (state != SUSPENDED) throw new RuntimeException("Not suspended");
      state = RUNNING;
      startThread();
      suspend();
    }

    private void startThread() {
      myThread =
        new Thread(this, "Plugin/"+getAgentIdentifier()+"/"+PlugInAdapter.this);
      myThread.setPriority(priority);
      myThread.start();
    }

    private void stopThread() {
      signalStateChange();
      try {
        myThread.join(60000);
      } catch (InterruptedException ie) {
      }
      myThread = null;
    }

    private void signalStateChange() {
      if (waker != null) {
        waker.signalNotify(waker.INTERNAL);
      }
    }

    public final void run() {
      if (firstRun) {
        plugin_prerun();                 // plugin first time through
        firstRun = false;
      }
      while (state == RUNNING) {
	boolean xwakep = waker.waitForSignal();
	setAwakened(xwakep);
        plugin_cycle();                // do work
        if (isYielding)
          Thread.yield();
      }
    }
    public void setWaker(SubscriptionWatcher sw) {
      waker = sw;
    }

    public SubscriptionWatcher getWaker() {
      return waker;
    }
  }
}

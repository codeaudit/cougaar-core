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


import org.cougaar.core.blackboard.BlackboardService;
import org.cougaar.core.blackboard.BlackboardClient;
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
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Services;

import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.domain.planning.ldm.RootFactory;

import org.cougaar.core.plugin.PlugInServesCluster;

import org.cougaar.util.GenericStateModel;
import org.cougaar.util.StateModelException;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.UnaryPredicate;

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

  // metrics service

  private MetricsService metricsService = null;
  public final void setMetricsService(MetricsService s) {
    metricsService = s;
  }
  protected final MetricsService getMetricsService() {
    return metricsService;
  }

  protected final MetricsSnapshot getMetricsSnapshot() {
    if (metricsService != null) {
      return metricsService.getMetricsSnapshot();
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

  
  private Vector parameters = null;

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

  /** Load the plugin.  No longer pays any attention to the passed object,
   * as it will now always be null.
   **/
  public void load(Object object) throws StateModelException {
    ((PluginAdapterBinder) getBindingSite()).setThreadingChoice(getThreadingChoice());
    super.load(object);
    theLDM = ((PluginAdapterBinder) getBindingSite()).getLDM();
    theLDMF = ((PluginAdapterBinder) getBindingSite()).getFactory();
    
    Services services = ((PluginAdapterBinder) getBindingSite()).getServices();
  }

  /** */
  public void start() throws StateModelException {}


  //
  // Customization of PlugInAdapter
  //


  /** Was a method of specifying the class of Subscriber to use.  This is 
   * now a function of the Binder, so is no longer appropriate.
   */
  protected Subscriber constructSubscriber(Distributor distributor) {
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

  private BlackboardService theBlackboard = null;

  public void setBlackboardService(BlackboardService s) {
    theBlackboard = s;
  }

  /** Safely return our BlackboardService (Subscriber)
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
      public ConfigFinder getConfigFinder() { return ((PluginAdapterBinder) getBindingSite()).getConfigFinder(); }
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

      // ??
      public java.sql.Connection getDatabaseConnection(Object locker) {throw new RuntimeException("Should not be called");}
      public void releaseDatabaseConnection(Object locker) {throw new RuntimeException("Should not be called");}
    };

  protected ConfigFinder getConfigFinder() {
    return ((PluginAdapterBinder) getBindingSite()).getConfigFinder();
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
   * Alias for getSubscriber().subscribe(UnaryPredicate);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember) {
    return getBlackboardService().subscribe(isMember);
  }

  /** like subscribe(UnaryPredicate), but allows specification of
   * some other type of Collection object as the internal representation
   * of the collection.
   * Alias for getSubscriber().subscribe(UnaryPredicate, Collection);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember, Collection realCollection){
    return getBlackboardService().subscribe(isMember, realCollection);
  }

  /**
   * Alias for getSubscriber().subscribe(UnaryPredicate, boolean);
   **/
  protected final Subscription subscribe(UnaryPredicate isMember, boolean isIncremental) {
    return getBlackboardService().subscribe(isMember, isIncremental);
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
   * <code> getSubscriber().unsubscribe(Subscription)</code>.
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
    return ((PluginAdapterBinder) getBindingSite()).getLDM();
  }

  protected final RootFactory getFactory() {
    return ((PluginAdapterBinder) getBindingSite()).getFactory();
  }
  /** @deprecated Use getFactory() */
  protected final RootFactory getLdmFactory() {
    return ((PluginAdapterBinder) getBindingSite()).getFactory();
  }

  protected final Factory getFactory(String s) {
    return ((PluginAdapterBinder) getBindingSite()).getFactory(s);
  }
  
  
  //
  // cluster
  // 

  protected final ClusterIdentifier getClusterIdentifier() {
    return ((PluginAdapterBinder) getBindingSite()).getClusterIdentifier();
  }

  protected final UIDServer getUIDServer() {
    return ((PluginAdapterBinder) getBindingSite()).getUIDServer();
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
      return ((PluginAdapterBinder) getBindingSite()).getLDM();
    }
    public RootFactory getFactory() {
      return ((PluginAdapterBinder) getBindingSite()).getFactory();
    }
    /** @deprecated use getFactory() **/
    public RootFactory getLdmFactory() {
      return ((PluginAdapterBinder) getBindingSite()).getFactory();
    }
    public Factory getFactory(String s) {
      return ((PluginAdapterBinder) getBindingSite()).getFactory(s);
    }
    public ClusterIdentifier getClusterIdentifier() {
      return ((PluginAdapterBinder) getBindingSite()).getClusterIdentifier();
    }
    public MetricsSnapshot getMetricsSnapshot() {
      return getMetricsService().getMetricsSnapshot();
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
      return didRehydrate(getBlackboardService().getSubscriber());
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

  public boolean didRehydrate() {
    return getBlackboardService().getSubscriber().didRehydrate();
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

  //dropped
  //protected final Threading getThreadingModel() { 
  //  return null;
  //}
  

  // keep these here for 
  public final static int UNSPECIFIED_THREAD = PluginAdapterBinder.UNSPECIFIED_THREAD;
  public final static int NO_THREAD = PluginAdapterBinder.NO_THREAD;
  public final static int SHARED_THREAD = PluginAdapterBinder.SHARED_THREAD;
  public final static int SINGLE_THREAD = PluginAdapterBinder.SINGLE_THREAD;
  public final static int ONESHOT_THREAD = PluginAdapterBinder.ONESHOT_THREAD;

  private int threadingChoice = UNSPECIFIED_THREAD;

  /** Set the current choice of threading model.  Will have no effect if
   * the threading model has already been acted on.
   * @deprecated better to call PluginBindingSite.setThreadingChoice
   **/
  protected final void setThreadingChoice(int m) {
    threadingChoice = m;
  }

  /** Set the current choice of threading model.  Will have no effect if
   * the threading model has already been acted on.
   * @deprecated call PluginBindingSite.setThreadingChoice(m) instead.
   **/
  protected final void chooseThreadingModel(int m) {
    threadingChoice = m;
  }

  /** @return the current choice of threading model.  **/
  protected final int getThreadingChoice() {
    return threadingChoice;
  }

  // /** return the default threading model for this class.
  //  * @deprecated ignored
  //  **/
  // protected final int getDefaultThreadingChoice() {
  //   return SHARED_THREAD;
  // }


  /** called from PluginBinder **/
  public void plugin_prerun() {
    BlackboardClient.current.set(this);
    prerun();
    BlackboardClient.current.set(null);
  }

  /** override to define prerun behavior **/
  protected void prerun() { }

  /** called from PluginBinder **/
  public void plugin_cycle() {
    BlackboardClient.current.set(this);
    cycle();
    BlackboardClient.current.set(null);
  }

  /** override to define cycle behavior **/
  protected  void cycle() { }

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


}

/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.MetricsSnapshot;
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.core.plugin.PlugInServesCluster;
import org.cougaar.util.ConfigFinder;

/**
 * ClusterServesPlugIn is the API which plugins may use to access
 * cluster-level services.
 **/
public interface ClusterServesPlugIn 
  extends ClusterContext
{

  /**
   * @return ClusterIdentifier the ClusterIdentifier associated with 
   * the Cluster where the PlugIn is plugged in.
   */
  ClusterIdentifier getClusterIdentifier();
        
  /**
   * Returns the Distributer associated with this Cluster.
   */
  //Distributor getDistributor();

  /**
   * @return the cluster's ConfigFinder instance.
   **/
  ConfigFinder getConfigFinder();

  /**
   * return our LDM instance.  You can get the factory(ies) from
   * the LDM instance.
   **/
  LDMServesPlugIn getLDM();

  /**
   * This method sets the COUGAAR scenario time to a specific time
   * in the future, leaving the clock stopped.
   * Time is in milliseconds.
   * Equivalent to setTime(time, false);
   * <em>Only UI PlugIns controlling the demonstration should use
   * this method.</em>
  **/
  void setTime(long time);

  /** General form of setTime, allowing the clock to be left running.
   * <em>Only UI PlugIns controlling the demonstration should use
   * this method.</em>
   **/
  void setTime(long time, boolean leaveRunning);

  /**
   * Changes the rate at which execution time advances. There is no
   * discontinuity in the value of execution time; it flows smoothly
   * from the current rate to the new rate.
   **/
  void setTimeRate(double newRate);

  /**
   * This method advances the COUGAAR scenario time a period of time
   * in the future, leaving the clock stopped.
   * Time is in milliseconds.
   * Equivalent to advanceTime(timePeriod, false);
   * <em>Only UI PlugIns controlling the demonstration should use
   * this method.</em>
   **/
  void advanceTime(long timePeriod);

  /** General form of advanceTime, allowing the clock to be left running.
   * <em>Only UI PlugIns controlling the demonstration should use
   * this method.</em>
   **/
  void advanceTime(long timePeriod, boolean leaveRunning);

  /** General form of advanceTime, allowing the clock to be left running at a new rate.
   * <em>Only UI PlugIns controlling the demonstration should use
   * this method.</em>
   **/
  void advanceTime(long timePeriod, double newRate);

  /**
   * Set a series of time parameter changes. The number of such
   * changes is limited. See ExecutionTimer.create() for details.
   **/
  void advanceTime(ExecutionTimer.Change[] changes);

  /**
   * Get the current execution time rate.
   **/
  double getExecutionRate();

  /**
   * This method gets the current COUGAAR scenario time. 
   * The returned time is in milliseconds.
   **/
  long currentTimeMillis( );

  /**
   * Called by a plugin to schedule an Alarm to ring 
   * at some future Scenario time.
   * This alarm functions over Scenario time which may be discontinuous
   * and/or offset from realtime.
   * If you want real (wallclock time, use addRealTimeAlarm instead).
   * Most plugins will want to just use the wake() functionality,
   * which is implemented in terms of addAlarm().
   **/
  void addAlarm(Alarm alarm);

  /**
   * Called by a plugin to schedule an Alarm to ring 
   * at some future Real (wallclock) time.
   **/
  void addRealTimeAlarm(Alarm alarm);

  /**
   * Return a snapshot of gathered cluster metrics.
   * This is exclusively for the use of UI-like plugins - there is
   * absolutely no guarantee of the degree of consistency required
   * for planning, nor will there ever be via this method.
   * @deprecated
   **/
  //  MetricsSnapshot getMetricsSnapshot();

  /**
   * Return a snapshot of gathered cluster metrics.
   * This is exclusively for the use of UI-like plugins - there is
   * absolutely no guarantee of the degree of consistency required
   * for planning, nor will there ever be via this method.
   * @param ms a <code>MetricsSnapshot</code> whose slots should be filled in
   * @param resetMsgStats a <bode>boolean</code> whether to reset the message transport counters
   * @return a <code>MetricsSnapshot</code> with the new values
   **/
  //MetricsSnapshot getMetricsSnapshot(MetricsSnapshot ms, boolean resetMsgStats);

  /**
   * Cluster-wide database support that can be integrated with persistence
   **/
  java.sql.Connection getDatabaseConnection(Object locker);
  void releaseDatabaseConnection(Object locker);
}

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
   * Cluster-wide database support that can be integrated with persistence
   **/
  java.sql.Connection getDatabaseConnection(Object locker);
  void releaseDatabaseConnection(Object locker);
}

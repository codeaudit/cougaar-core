/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
 
package org.cougaar.core.plugin;

import org.cougaar.core.cluster.SubscriptionWatcher;

public interface ScheduleablePlugIn {
  /** Support for SharedThreading.
   * When a plugin needs to be actived by any sort of plugin Scheduler,
   * this method will be called exactly once by the scheduler
   * during initialization so that it can tell when each plugin
   * might need to be awakened.
   * ONLY FOR INFRASTRUCTURE
   **/
  void addExternalActivityWatcher(SubscriptionWatcher watcher);

  /** Support for SharedThreading.
   * When the plugin scheduler decides that there is work for a 
   * plugin to do, it calls this method to execute the code.
   * ONLY FOR INFRASTRUCTURE
   **/
  void externalCycle(boolean wasExplicit);
}

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

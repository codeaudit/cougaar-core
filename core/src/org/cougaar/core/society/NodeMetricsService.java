/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.society;

import org.cougaar.core.component.Service;

/** A NodeMetricsService is an API which may be supplied by a 
 * ServiceProvider registered in a ServiceBroker that provides metrics for
 * the Node (VM). 
 */

public interface NodeMetricsService extends Service {
  
  /** Free Memory snapshot from the Java VM at the time of
   * the method call.
  **/
  long getFreeMemory();

  /** Total memory snaphsot from the Java VM at the time of the
   * method call
   */
  long getTotalMemory();

  /** The number of active Threads in the main COUGAAR threadgroup **/
  int getActiveThreadCount();

}

/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.core.society.UID;

import java.util.Enumeration;

/** Notification Interface
 * Notification is a response to a task that was sent to a cluster.
 * The Notification will inlcude the task and the allocationresult
 **/

public interface Notification extends Directive {

  /**
   * Returns the task the notification is in reference to.
   * @return Task
   **/
  UID getTaskUID();
   
  /**
   * Returns the estimated allocation result from below
   * @return AllocationResult
   **/
  AllocationResult getAllocationResult();
   
  /** Get the child task's UID that was disposed.  It's parent task is getTask();
   * Useful for keeping track of which subtask of an Expansion caused
   * the re-aggregation of the Expansion's reported allocationresult.
   * @return UID
   */
  UID getChildTaskUID();
   
}

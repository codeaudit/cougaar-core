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

package org.cougaar.planning.ldm.plan;

import org.cougaar.core.util.UID;
import org.cougaar.core.blackboard.Directive;

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

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


/**  SubTaskResults
   * Allows access to the sub-task's allocation result information
   * used to aggregate this Expansion's latest reported allocationresult
   *
   * @author  ALPINE <alpine-software@bbn.com>
   *
   **/

public class SubTaskResult implements java.io.Serializable {
  
  Task t;
  AllocationResult ar;
  boolean changed;
  
  /** Simple Constructor for saving state of a single sub-task's results
    * when the AllocationResult Aggregator is run.  The boolean changed
    * keeps track of whether this allocationresult changed to cause the
    * re-aggregation.
    * @param task  the subtask of the workflow
    * @param haschanged  whether this is a new allocationresult causing the recalculation
    * @param result the AllocationResult used to Aggregate the results of the workflow
    * @return SubTaskResults
    */
  public SubTaskResult (Task task, boolean haschanged, AllocationResult result) {
    this.t = task;
    this.changed = haschanged;
    this.ar = result;
  }
  
  /** @return Task  The sub-task this information is about. **/
  public Task getTask() { return t; }
  /** @return AllocationResult  The AllocationResult for this sub-task used by the Aggregator **/
  public AllocationResult getAllocationResult() { return ar; }
  /** @return boolean  Whether this was a new AllocationResult that caused the re-aggregation **/
  public boolean hasChanged() { return changed; }
}

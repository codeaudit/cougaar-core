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


/**  SubTaskResults
   * Allows access to the sub-task's allocation result information
   * used to aggregate this Expansion's latest reported allocationresult
   *
   * @author  ALPINE <alpine-software@bbn.com>
   * @version $Id: SubTaskResult.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
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

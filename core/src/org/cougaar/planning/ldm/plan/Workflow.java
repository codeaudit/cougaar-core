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

import java.util.Vector;
import java.util.Enumeration;
import org.cougaar.planning.ldm.plan.AllocationResult;

import org.cougaar.core.util.UniqueObject;

/**
 * Workflow Interface
 * A Workflow is the result of a Task expansion consisting primarily
 * of a partially ordered set of Task instances.  There are many sorts
 * of Workflow implementations possible, ranging from a strictly-ordered
 * vector of subtasks, to an unordered Bag, to a set of DAGs, or even 
 * a complex, temporally-ordered set.
 **/
  
public interface Workflow 
  extends UniqueObject, Annotatable
{
 	
  /**  
   * <PRE> Task parenttask = myworkflow.getParentTask(); </PRE>
   * @return Task  Return the Task for which this Workflow is an expansion.
   **/
  Task getParentTask();
 	
  /** 
   * Return an Enumeration which walks over all tasks 
   * which are members of this Workflow.
   * <PRE> Enumeration mytasks = myworkflow.getTasks(); </PRE>
   * @return Enumeration{Task}
   **/
  Enumeration getTasks();
 	
  /** 
   * Returns an Enumeration which walks over
   * all Constraints that are members of
   * this workflow.
   * <PRE> Enumeration myconstraints = myworkflow.getConstraints(); </PRE>
   * @return Enumeration{Constraint}
   **/
  Enumeration getConstraints();
   
  /** 
   * Returns an Enumeration which walks over
   * all Constraints that have a relationship
   * to the passed Task.
   * <PRE> Enumeration myconstraints = myworkflow.getTaskConstraints(mytask); </PRE>
   * @param task - The task you are checking for Constraints. 
   * @return Enumeration{Constraint}
   **/
  Enumeration getTaskConstraints(Task task);
   
  /** 
   * Returns and Enumeration which walks over
   * all Constraints that have a pair-wise 
   * relationship with two passed tasks.
   * <PRE> Enumeration myconstraints = myworkflow.getPairConstraints(mytask1, mytask2); </PRE>
   * @param constrainedTask - Task that is constrained
   * @param constrainingTask - Task that is constraining
   * @return Enumeration{Constraint}
   **/
  Enumeration getPairConstraints(Task constrainedTask, Task constrainingTask);
   
  /** Ask the workflow to compute an AllocationResult based on the 
   * AllocationResults of the Workflow's sub Tasks.  If the aggregate
   * AllocationResult is undefined (e.g. some of the sub Tasks have not yet
   * been allocated), computePenaltyValue should return null.
   *
   * @return AllocationResult - the result of aggregating the AllocationResults
   * of the Workflow using the defined (or default) AllocationResultAggregator.
   * @see org.cougaar.planning.ldm.plan.AllocationResultAggregator
   **/
  AllocationResult aggregateAllocationResults();
  
  /** Has a constraint been violated?
    * @return boolean
    */
    boolean constraintViolation();
  
  /** Get the constraints that were violated.
    * @return Enumeration{Constraint}
    */
    Enumeration getViolatedConstraints();

  /** Should subtasks be rescinded by the infrastructure when the
   * expansion this workflow is attached to is rescinded?
   * Defaults to true.
   * Set to false (meaning the Plugin is responsible for rescinding or
   * reattaching the workflow and its subtasks to an expansion and parent task)
   * by NewWorkflow.setIsPropagatingToSubtasks(isProp);
   * @return boolean 
   **/
  boolean isPropagatingToSubtasks();

   /** Return constraint for which constraining event is defined and
    * constraining event is undefined or violated
    **/
   
  Constraint getNextPendingConstraint();

}


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

package org.cougaar.domain.planning.ldm.plan;

import java.util.List;

/** Composition Interface
   * An Composition represents the aggregation of multiple tasks
   * into a single task.  Compositions are referenced by Aggregation PlanElements.
   *
   * @author  ALPINE <alpine-software@bbn.com>
   * @version $Id: Composition.java,v 1.3 2001-08-22 20:14:10 mthome Exp $
   **/

public interface Composition
{
  
  /** Returns the Aggregation PlanElements of the Tasks that
    * are being combined
    * @return List
    * @see org.cougaar.domain.planning.ldm.plan.Aggregation
    */
  List getAggregations();
  
  /** Convenienve method that calculates the Tasks that are 
   * being aggregated by looking at all of the Aggregations.
   * (Aggregation.getTask())
   * @return List
   * @see org.cougaar.domain.planning.ldm.plan.Task
   **/
  List getParentTasks();
  
  /** Returns the newly created task that represents all 'parent' tasks.
    * The new task should be created as an MPTask.
    * @return Task
    * @see org.cougaar.domain.planning.ldm.plan.MPTask
    */
  MPTask getCombinedTask();
  
  /** Allows the AllocationResult to be properly dispersed among the 
    * original (or parent) tasks.
    * @return AllocationResultDistributor
    * @see org.cougaar.domain.planning.ldm.plan.AllocationResultDistributor    
    */
  AllocationResultDistributor getDistributor();
  
  /**Calculate seperate AllocationResults for each parent task of the Composition.
    * @return TaskScoreTable
    * @see org.cougaar.domain.planning.ldm.plan.TaskScoreTable
    */
  TaskScoreTable calculateDistribution();
  
  /** Should all related Aggregations, and the combined task be rescinded 
   * when a single parent task and its Aggregation is rescinded.
   * When false, and a single 'parent' Aggregation is rescinded,
   * the infrastructure removes references to that task/Aggregation in the
   * Composition and the combined MPTask.  However, the Composition and combined
   * task are still valid as are the rest of the parent tasks/Aggregations that
   * made up the rest of the Composition.
   * Defaults to true.
   * set to false by NewComposition.setIsPropagating(isProp);
   **/
  boolean isPropagating();
  
}
  

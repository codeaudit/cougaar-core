/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import java.util.Enumeration;
import java.util.Collection;

/** NewComposition Interface
   * Used to build complete Composition objects.
   *
   * @author  ALPINE <alpine-software@bbn.com>
   * @version $Id: NewComposition.java,v 1.2 2001-04-05 19:27:16 mthome Exp $
   **/

public interface NewComposition extends Composition {
  
  /** Set the Aggregation PlanElements of the tasks being combined
    * @param Collection  The Aggregations
    * @see org.cougaar.domain.planning.ldm.plan.Aggregation
    */
  void setAggregations(Collection aggs);
  
  /** Add a single Aggregation to the existing collection
   * @param Aggregation
   */
  void addAggregation(Aggregation singleagg);
  
  /** Set the newly created task that represents all 'parent' tasks.
    * @param newTask
    * @see org.cougaar.domain.planning.ldm.plan.Task
    */
  void setCombinedTask(MPTask newTask);
  
  /** Allows the AllocationResult to be properly dispersed among the 
    * original (or parent) tasks.
    * @param distributor
    * @see org.cougaar.domain.planning.ldm.plan.AllocationResultDistributor
    */
  void setDistributor(AllocationResultDistributor distributor);
  
  /** Tells the infrastructure that all members of this composition should
   * be rescinded when one of the Aggregations is rescinded, this includes all
   * of the Aggregations (one for each parent task), the combined task and 
   * planelements against the combined task.
   * If flag is set to False, the infrastructure does NOT rescind the other
   * Aggregations or the combined task.  It only removes the reference of the
   * rescinded Aggregation and its task (a parent task) from the composition
   * and the combined task.
   * @param isProp
   **/
  void setIsPropagating(boolean isProp);
  
  /** @deprecated  Use setIsPropagating(boolean isProp) - defaults to true**/
  void setIsPropagating();
  
}
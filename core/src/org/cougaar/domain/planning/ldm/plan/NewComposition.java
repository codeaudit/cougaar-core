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

import java.util.Enumeration;
import java.util.Collection;

/** NewComposition Interface
   * Used to build complete Composition objects.
   *
   * @author  ALPINE <alpine-software@bbn.com>
   * @version $Id: NewComposition.java,v 1.3 2001-08-22 20:14:12 mthome Exp $
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
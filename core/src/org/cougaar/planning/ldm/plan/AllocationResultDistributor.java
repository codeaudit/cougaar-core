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
import java.util.Hashtable;

/** AllocationResultDistributor is a class which specifies how allocation results
  * should be distributed amongst 'parent' tasks of a Composition.
  * Distributes all aspect values amongst all parent tasks, divides COST and 
  * QUANTITY aspects evenly among all parent tasks.
  * Distributes all AuxiliaryQueryTypes and data to all parent tasks.
  * @see org.cougaar.planning.ldm.plan.AllocationResult
  **/

public interface AllocationResultDistributor
  extends AspectType // for Constants
{
  
  /** Calculate seperate AllocationResults for each parent task of 
   * the Composition.
   * @param parents Vector of Parent Tasks.
   * @param aggregateAllocationResult The allocationResult of the subtask.
   * @return distributedresults
   * @see org.cougaar.planning.ldm.plan.Composition
   * @see org.cougaar.planning.ldm.plan.TaskScoreTable
   * @see org.cougaar.planning.ldm.plan.AllocationResult
   */
  TaskScoreTable calculate(Vector parents, AllocationResult aggregateAllocationResult);
  
  /* static accessor for a default distributor */
  AllocationResultDistributor DEFAULT = new DefaultDistributor();
  
  // implementation of the default distributor
  /** Default distributor makes the best guess computation possible
   * without examining the details of the parent or sub tasks.
   * In particular all result values are copied to the values passed
   * to the parent, except for COST and QUANTITY, whose values are
   * distributed equally among the parents. This may or may not be
   * the right thing, depending on what sort of tasks are being 
   * aggregated.
   **/

  class DefaultDistributor
    implements AllocationResultDistributor 
  {
    public DefaultDistributor() {}
    public TaskScoreTable calculate(Vector parents, AllocationResult ar) {
      int l = parents.size();

      if (l == 0 || ar == null) return null;

      // create the shared value vector and fill in the values for the defined aspects ONLY.
      int[] types = ar.getAspectTypes();
      double acc[] = new double[types.length];
      for (int x = 0; x < types.length; x++) {
        // if the aspect is COST or QUANTITY divide evenly across parents
        if ( (types[x] == COST) || (types[x] == QUANTITY) ) {
          acc[x] = ar.getValue(types[x]) / l;
        } else {
          acc[x] = ar.getValue(types[x]);
        }
      }
      
      AllocationResult newar = new AllocationResult(ar.getConfidenceRating(),
                                                    ar.isSuccess(),
                                                    types,
                                                    acc);
      // fill in the auxiliaryquery info
      // each of the new allocationresults(for the parents) will have the SAME
      // auxiliaryquery info that the allocationresult (of the child) has.  
      for (int aq = 0; aq < AuxiliaryQueryType.AQTYPE_COUNT; aq++) {
        String info = ar.auxiliaryQuery(aq);
        if (info != null) {
          newar.addAuxiliaryQueryInfo(aq, info);
        }
      }
      
      AllocationResult results[] = new AllocationResult[l];
      for (int i = 0; i<l; i++) {
        results[i] = newar;
      }

      Task tasks[] = new Task[l];
      parents.copyInto(tasks);

      return new TaskScoreTable(tasks, results);
    }
  } // end of DefaultDistributor inner class
  
}

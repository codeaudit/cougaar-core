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

import java.util.Vector;
import java.util.Hashtable;

/** AllocationResultDistributor is a class which specifies how allocation results
  * should be distributed amongst 'parent' tasks of a Composition.
  * Distributes all aspect values amongst all parent tasks, divides COST and 
  * QUANTITY aspects evenly among all parent tasks.
  * Distributes all AuxiliaryQueryTypes and data to all parent tasks.
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: AllocationResultDistributor.java,v 1.2 2001-04-05 19:27:13 mthome Exp $
  * @see org.cougaar.domain.planning.ldm.plan.AllocationResult
  **/

public interface AllocationResultDistributor
  extends AspectType // for Constants
{
  
  /** Calculate seperate AllocationResults for each parent task of 
   * the Composition.
   * @param parents Vector of Parent Tasks.
   * @param aggregateAllocationResult The allocationResult of the subtask.
   * @return distributedresults
   * @see org.cougaar.domain.planning.ldm.plan.Composition
   * @see org.cougaar.domain.planning.ldm.plan.TaskScoreTable
   * @see org.cougaar.domain.planning.ldm.plan.AllocationResult
   */
  public TaskScoreTable calculate(Vector parents, AllocationResult aggregateAllocationResult);
  
  /* static accessor for a default distributor */
  public static AllocationResultDistributor DEFAULT = new DefaultDistributor();
  
  // implementation of the default distributor
  /** Default distributor makes the best guess computation possible
   * without examining the details of the parent or sub tasks.
   * In particular all result values are copied to the values passed
   * to the parent, except for COST and QUANTITY, whose values are
   * distributed equally among the parents. This may or may not be
   * the right thing, depending on what sort of tasks are being 
   * aggregated.
   **/

  public static class DefaultDistributor
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
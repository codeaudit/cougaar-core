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
import java.util.Enumeration;

/** AllocationResultAggregator is a class which specifies how allocationresults
 * should be aggregated.  Currenlty used by Workflow.computeAllocationResult
 * @see org.cougaar.domain.planning.ldm.plan.AllocationResult
 **/

public interface AllocationResultAggregator 
  extends AspectType // for Constants
{
  public static final double SIGNIFICANT_CONFIDENCE_RATING_DELTA = 0.0001;
  
  /** @return AllocationResult The computed AllocationResult
   * @param wf The workflow that is using this aggregator to compute it's AllocationResult
   * @param tst The TaskScoreTable associated with this workflow
   * @param currentar The current AllocationResult
   * @see org.cougaar.domain.planning.ldm.plan.Workflow
   * @see org.cougaar.clusteorbject.TaskPenaltyTable
   **/
  public AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar);
  
  /** static accessor for a default/generic AllocationResultAggregator that does
   * a simple summation of all measures (including extensions) found in all allocationresults in the
   * subtasks of the workflow.
   */
  public static AllocationResultAggregator SUM = new Sum();

  public static AllocationResultAggregator DEFAULT = new DefaultARA();
  
  // implementation of a default/generic AllocationResultAggregator
  public class Sum implements AllocationResultAggregator {
    private static final double UNDEF = -1;
    public Sum() {}
    public AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar) {
      // write new implementation - stub for now.
      return null;
    }
  } // end of Sum class

      
  /** Does the right computation for workflows which are made up of
   * equally important tasks with no inter-task constraints.
   * START_TIME is minimized.
   * END_TIME is maximized.
   * DURATION is overall END_TIME - overall START_TIME.
   * COST is summed.
   * DANGER is maximized.
   * RISK is maximized.
   * QUANTITY is summed.
   * INTERVAL is summed.
   * TOTAL_QUANTITY is summed.
   * TOTAL_SHIPMENTS is summed.
   * CUSTOMER_SATISFACTION is averaged.
   * Any extended aspect types are ignored.
   * 
   * For AuxiliaryQuery information, if all the query values are the same
   * across subtasks or one subtask has query info it will be place in the 
   * aggregate result.  However, if there are conflicting query values, no
   * information will be put in the aggregated result.
   * 
   * returns null when there are no subtasks or any task has no result.
   **/
  public class DefaultARA implements AllocationResultAggregator {
    private static final String UNDEFINED = "UNDEFINED";

    public AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar) {
      double acc[] = new double[AspectType._ASPECT_COUNT];
      acc[START_TIME] = Double.MAX_VALUE;
      acc[END_TIME] = 0.0;
      // duration is computed from end values of start and end
      acc[COST] = 0.0;
      acc[DANGER] = 0.0;
      acc[RISK] = 0.0;
      acc[QUANTITY] = 0.0;
      acc[INTERVAL] = 0.0;
      acc[TOTAL_QUANTITY] = 0.0;
      acc[TOTAL_SHIPMENTS] = 0.0;
      acc[CUSTOMER_SATISFACTION] = 1.0; // start at best

      boolean suc = true;
      double rating = 0.0;
      
      if (tst == null) return null;
      int tstSize = tst.size();
      if (tstSize == 0) return null;
      
      String auxqsummary[] = new String[AuxiliaryQueryType.AQTYPE_COUNT];
      // initialize all values to UNDEFINED for comparison purposes below.
      int aql = auxqsummary.length;
      for (int aqs = 0; aqs < aql; aqs++) {
        auxqsummary[aqs] = UNDEFINED;
      }

      for (int i = 0; i < tstSize; i++) {
        Task t = tst.getTask(i);
        AllocationResult ar = tst.getAllocationResult(i);
        if (ar == null) return null; // bail if undefined

        suc = suc && ar.isSuccess();
        rating += ar.getConfidenceRating();
        
        int[] definedaspects = ar.getAspectTypes();
        int al = definedaspects.length;
        for (int b = 0; b < al; b++) {
          // accumulate the values for the defined aspects
          switch (definedaspects[b]) {
          case START_TIME: acc[START_TIME] = Math.min(acc[START_TIME], ar.getValue(START_TIME));
            break;
          case END_TIME: acc[END_TIME] = Math.max(acc[END_TIME], ar.getValue(END_TIME));
            break;
            // compute duration later
          case COST: acc[COST] += ar.getValue(COST);
            break;
          case DANGER: acc[DANGER] = Math.max(acc[DANGER], ar.getValue(DANGER));
            break;
          case RISK: acc[RISK] = Math.max(acc[RISK], ar.getValue(RISK));
            break;
          case QUANTITY: acc[QUANTITY] += ar.getValue(QUANTITY);
            break;
            // for now simply add the repetitve task values
          case INTERVAL: acc[INTERVAL] += ar.getValue(INTERVAL);
            break;
          case TOTAL_QUANTITY: acc[TOTAL_QUANTITY] += ar.getValue(TOTAL_QUANTITY);
            break;
          case TOTAL_SHIPMENTS: acc[TOTAL_SHIPMENTS] += ar.getValue(TOTAL_SHIPMENTS);
            break;
            //end of repetitive task specific aspects
          case CUSTOMER_SATISFACTION: acc[CUSTOMER_SATISFACTION] += ar.getValue(CUSTOMER_SATISFACTION);
            break;
          }
        }
        
        // Sum up the auxiliaryquery data.  If there are conflicting data
        // values, send back nothing for that type.  If only one subtask
        // has information about a querytype, send it back in the 
        // aggregated result.
        for (int aq = 0; aq < AuxiliaryQueryType.AQTYPE_COUNT; aq++) {
          String data = ar.auxiliaryQuery(aq);
          if (data != null) {
            String sumdata = auxqsummary[aq];
            // if sumdata = null, there has already been a conflict.
            if (sumdata != null) {
              if (sumdata.equals(UNDEFINED)) {
                // there's not a value yet, so use this one.
                auxqsummary[aq] = data;
              } else if (! data.equals(sumdata)) {
                // there's a conflict, pass back null
                auxqsummary[aq] = null;
              }
            }
          }
        }

      } // end of looping through all subtasks
      
      acc[DURATION] = acc[END_TIME] - acc[START_TIME];
      acc[CUSTOMER_SATISFACTION] /= tstSize;

      rating /= tstSize;

      boolean delta = false;
      //for (int i = 0; i <= _LAST_ASPECT; i++) {
        //if (acc[i] != currentar.getValue(i)) {
          //delta = true;
          //break;
        //}
      //}
      
      // only check the defined aspects and make sure that the currentar is not null
      if (currentar == null) {
        delta = true;		// if the current ar == null then set delta true
      } else {
        int[] caraspects = currentar.getAspectTypes();
        if (caraspects.length != acc.length) {
          //if the current ar length is different than the length of the new
          // calculations (acc) there's been a change
          delta = true;
        } else {
          int il = caraspects.length;
          for (int i = 0; i < il; i++) {
            int da = caraspects[i];
            if (acc[da] != currentar.getValue(da)) {
              delta = true;
              break;
            }
          }
        }
      
        if (!delta) {
	  if (currentar.isSuccess() != suc) {
	    delta = true;
	  } else if (Math.abs(currentar.getConfidenceRating() - rating) > SIGNIFICANT_CONFIDENCE_RATING_DELTA) {
	    delta = true;
	  }
        }
      }

      if (delta) {
        AllocationResult artoreturn = new AllocationResult(rating, suc, _STANDARD_ASPECTS, acc);
        int aqll = auxqsummary.length;
        for (int aqt = 0; aqt < aql; aqt++) {
          String aqdata = auxqsummary[aqt];
          if ( (aqdata !=null) && (aqdata != UNDEFINED) ) {
            artoreturn.addAuxiliaryQueryInfo(aqt, aqdata);
          }
        }
        return artoreturn;
      } else {
        return currentar;
      }
    }
  }
}

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

import java.util.Enumeration;
import org.cougaar.core.society.UniqueObject;
import org.cougaar.util.TimeSpan;
import org.cougaar.core.cluster.*;

/** PlanElement Interface
 * PlanElements are the primitive building blocks from which 
 * LogPlans are constructed. A single PlanElement represents a 
 * cycle of work completed against a Task. A PlanElement is of 
 * type Expansion (represented by a Workflow and the implied tasks 
 * embodied in it), Allocation (represented by an Asset),
 *  Aggregation (represented by an MPWorkflow) or AssetTransfer. 
 **/

public interface PlanElement 
  extends TimeSpan, UniqueObject, Annotatable, Publishable
{
	
  /** @return Plan the Plan of this plan element.
   **/
  Plan getPlan();

  /** This returns the Task of the PlanElement. 
   * @return Task
   **/
  
  Task getTask();
  
  /** Returns the estimated allocation result that is related to performing
   * the Task.
   * @return AllocationResult
   **/

   AllocationResult getEstimatedResult();

  /**
   * Returns the reported allocation result. The reported result is
   * computed from the received and observed results
   * @return AllocationResult
   **/
   AllocationResult getReportedResult();

  /** Returns the received allocation result.
   * @return AllocationResult
   **/
   AllocationResult getReceivedResult();

  /**
   * Returns the observed allocation result.
   * @return AllocationResult
   **/
   AllocationResult getObservedResult();
   
  /** Set the estimated allocation result so that a notification will
   * propagate up another level.
   * @param estimatedresult
   **/
  void setEstimatedResult(AllocationResult estimatedresult);
   
  /**
   * Set the observed allocation result that be incorporated into the
   * reported result
   * @param observedresult
   **/
  void setObservedResult(AllocationResult observedResult);
  
  public static interface PlanElementChangeReport extends ChangeReport {
  }

  public abstract static class ResultChangeReport implements PlanElementChangeReport {
    private int type;
    public final static int UNDEFINED_TYPE = AspectType.UNDEFINED;
    private double oldValue;
    public final static double UNDEFINED_VALUE = Double.MIN_VALUE;

    public ResultChangeReport() {
      type = UNDEFINED_TYPE;
      oldValue = UNDEFINED_VALUE;
    }
    public ResultChangeReport(int t) {
      type=t;
      oldValue = UNDEFINED_VALUE;
    }
    public ResultChangeReport(int t, double o) {
      type=t; oldValue=o;
    }
    /** May return AspectType.UNDEFINED if the aspect type id is unknown **/
    public int getAspectType() { return type; }
    /** old value if known.  If unknown (e.g. no previous value)
     * will return Double.MIN_VALUE.
     **/
    public double getOldValue() { return oldValue; }

    public int hashCode() { return getClass().hashCode()+type; }
    public boolean equals(Object o) {
      if (o == null) return false;

      return (this == o) ||
        (o.getClass() == getClass() &&
         ((ResultChangeReport)o).type == type);
    }
    public String toString() {
      if (type == UNDEFINED_TYPE) {
        return " (?)";
      } else {
        return " ("+type+")";
      }
    }
  }
  // change reports

  /** Something in the Estimated result changed. **/
  public static class EstimatedResultChangeReport extends ResultChangeReport {
    public EstimatedResultChangeReport() { super(); }
    public EstimatedResultChangeReport(int t) { super(t); }
    public EstimatedResultChangeReport(int t, double ov) { super(t,ov); }
    public String toString() {
      return "EstimatedResultChangeReport"+super.toString();
    }
  }
  /** Something in the reported result changed. **/
  public static class ReportedResultChangeReport extends ResultChangeReport {
    public ReportedResultChangeReport() { super(); }
    public ReportedResultChangeReport(int t) { super(t); }
    public ReportedResultChangeReport(int t, double ov) { super(t,ov); }
    public String toString() {
      return "ReportedResultChangeReport"+super.toString();
    }
  }
  /** Something in the observed result changed. **/
  public static class ObservedResultChangeReport extends ResultChangeReport {
    public ObservedResultChangeReport() { super(); }
    public ObservedResultChangeReport(int t) { super(t); }
    public ObservedResultChangeReport(int t, double ov) { super(t,ov); }
    public String toString() {
      return "ObservedResultChangeReport"+super.toString();
    }
  }
}

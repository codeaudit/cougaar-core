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

/** NewPlanElement Interface
 * provides setter methods to build a PlanElement object
 **/

public interface NewPlanElement extends PlanElement {
  /** 
   * @param p - Set the Plan associated with the PlanElement.
   **/
  void setPlan(Plan p);

  /** This sets the Task of the PlanElement. 
   * @param t - The Task that the PlanElement is referencing.
   **/
  void setTask(Task t);
}

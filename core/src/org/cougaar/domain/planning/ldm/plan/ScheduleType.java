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

/** Constant names for types of Schedules
 **/
public interface ScheduleType {
  static final String ASSIGNED_RELATIONSHIP = "Assigned_Relationship";
  static final String ASSIGNED_AVAILABILITY = "Assigned_Availability";
  static final String OTHER = "Other";
  static final String RELATIONSHIP = "Relationship";
  
  /** @deprecated Use org.cougaar.domain.glm.ldm.plan.PlanScheduleType.TOTAL_CAPACITY
   **/
  static final String TOTAL_CAPACITY = "Total_Capacity";

  /** @deprecated Use org.cougaar.domain.glm.ldm.plan.PlanScheduleType.ALLOCATED_CAPACITY
   **/
  static final String ALLOCATED_CAPACITY = "Allocated_Capacity";

  /** @deprecated Use org.cougaar.domain.glm.ldm.plan.PlanScheduleType.AVAILABLE_CAPACITY
   **/
  static final String AVAILABLE_CAPACITY = "Available_Capacity";

  /** @deprecated Use org.cougaar.domain.glm.ldm.plan.PlanScheduleType.TOTAL_INVENTORY
   **/
  static final String TOTAL_INVENTORY = "Total_Inventory";

  /** @deprecated Use org.cougaar.domain.glm.ldm.plan.PlanScheduleType.ACTUAL_CAPACITY
   **/
  static final String ACTUAL_CAPACITY = "Actual_Capacity";

  /** @deprecated Use org.cougaar.domain.glm.ldm.plan.PlanScheduleType.LABOR
   **/
  static final String LABOR = "Labor";
}

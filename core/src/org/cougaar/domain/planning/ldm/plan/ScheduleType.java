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

/** Constant names for types of Schedules
 **/
public interface ScheduleType {
  static final String ASSIGNED_RELATIONSHIP = "Assigned_Relationship";
  static final String ASSIGNED_AVAILABILITY = "Assigned_Availability";
  static final String OTHER = "Other";
  static final String RELATIONSHIP = "Relationship";
  static final String ROLE = "Role";
  
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

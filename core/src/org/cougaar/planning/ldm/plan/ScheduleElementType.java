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

/** Constant names for types of SchedulesElements
 **/
public interface ScheduleElementType {
  
  Class SIMPLE = ScheduleElement.class;
  Class MIXED = ScheduleElement.class;

  Class ASSIGNED_RELATIONSHIP = AssignedRelationshipElement.class;
  Class ASSIGNED_AVAILABILITY = AssignedAvailabilityElement.class;
  Class ITINERARY = ItineraryElement.class;
  Class LOCATION = LocationScheduleElement.class;
  Class LOCATIONRANGE = LocationRangeScheduleElement.class;
  Class RELATIONSHIP = Relationship.class;
  Class PLAN_ELEMENT = PlanElement.class;

  /**deprecated Use org.cougaar.glm.ldm.plan.PlanScheduleElementType.QUANTITY
   **/
  Class QUANTITY = ScheduleElement.class;

  /**deprecated Use org.cougaar.glm.ldm.plan.PlanScheduleElementType.QUANTIITYRANGE
   **/
  Class QUANTITYRANGE = ScheduleElement.class;

 /**deprecated Use org.cougaar.glm.ldm.plan.PlanScheduleElementType.RATE
   **/
  Class RATE = ScheduleElement.class;

 /**deprecated Use org.cougaar.glm.ldm.plan.PlanScheduleElementType.CAPACITY
   **/
  Class CAPACITY = ScheduleElement.class;

  /** @deprecated move to appropriate class in the domain layer
   **/
  Class LABOR = ScheduleElement.class;
}










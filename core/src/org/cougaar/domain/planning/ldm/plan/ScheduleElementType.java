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

/** Constant names for types of SchedulesElements
 **/
public interface ScheduleElementType {
  
  static final Class SIMPLE = ScheduleElement.class;
  static final Class MIXED = ScheduleElement.class;

  static final Class ASSIGNED_RELATIONSHIP = AssignedRelationshipElement.class;
  static final Class ASSIGNED_AVAILABILITY = AssignedAvailabilityElement.class;
  static final Class ITINERARY = ItineraryElement.class;
  static final Class LOCATION = LocationScheduleElement.class;
  static final Class LOCATIONRANGE = LocationRangeScheduleElement.class;
  static final Class RELATIONSHIP = Relationship.class;
  static final Class PLAN_ELEMENT = PlanElement.class;

  /**deprecated Use org.cougaar.domain.glm.ldm.plan.PlanScheduleElementType.QUANTITY
   **/
  static final Class QUANTITY = ScheduleElement.class;

  /**deprecated Use org.cougaar.domain.glm.ldm.plan.PlanScheduleElementType.QUANTIITYRANGE
   **/
  static final Class QUANTITYRANGE = ScheduleElement.class;

 /**deprecated Use org.cougaar.domain.glm.ldm.plan.PlanScheduleElementType.RATE
   **/
  static final Class RATE = ScheduleElement.class;

 /**deprecated Use org.cougaar.domain.glm.ldm.plan.PlanScheduleElementType.CAPACITY
   **/
  static final Class CAPACITY = ScheduleElement.class;

  /** @deprecated move to appropriate class in the domain layer
   **/
  static final Class LABOR = ScheduleElement.class;
}










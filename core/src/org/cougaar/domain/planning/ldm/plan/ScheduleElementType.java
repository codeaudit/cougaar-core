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










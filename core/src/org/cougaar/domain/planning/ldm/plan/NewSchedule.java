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

/**
 * NewSchedule adds mutator methods to any Schedule.
 * It is a real bad idea to change schedules unless you are the 
 * "owner" of a schedule (e.g. you actually created it).
 * This API does no type checking of elements.
 **/

public interface NewSchedule extends Schedule {
  /** add a scheduleElement to the schedule. **/
  void addScheduleElement(ScheduleElement aScheduleElement);

  /** remove a scheduleElement from the schedule.  **/
  void removeScheduleElement(ScheduleElement aScheduleElement);

  /** remove all schedule elements from the schedule **/
  void clearScheduleElements();
}


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

import java.util.Date;
import org.cougaar.util.TimeSpan;

/**
 * A ScheduleElement is an encapsulation of spatio-temporal relationships.
 * Current thought is to bind up both time and space into a single
 * object which may then be queried in various ways to test for
 * overlaps, inclusion, exclusion, etc with other schedules.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: ScheduleElement.java,v 1.2 2001-04-05 19:27:20 mthome Exp $
 **/

public interface ScheduleElement 
  extends TimeSpan
{
	
  /** Start date is a millisecond-precision, inclusive time of start.
   * @return Date Start time for the task 
   **/
  Date getStartDate();
	
  /** End Date is millisecond-precision, <em>exclusive</em> time of end.
   * @return Date End time for the task 
   **/
  Date getEndDate();
	
  /** is the Date on or after the start time and strictly before the end time?
   *  @return boolean whether the date is included in this time interval.  
   **/
  boolean included(Date date);
	
  /** is the time on or after the start time and strictly before the end time?
   * @return boolean whether the time is included in this time interval 
   **/
  boolean included(long time);

  /** Does the scheduleelement overlap (not merely abut) the schedule?
   * @return boolean whether schedules overlap 
   **/
  boolean overlapSchedule(ScheduleElement scheduleelement);

  /** Does the scheduleElement meet/abut the schedule?
   **/
  boolean abutSchedule(ScheduleElement scheduleelement);

} 

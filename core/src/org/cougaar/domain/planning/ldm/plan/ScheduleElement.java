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

import java.util.Date;
import org.cougaar.util.TimeSpan;

/**
 * A ScheduleElement is an encapsulation of spatio-temporal relationships.
 * Current thought is to bind up both time and space into a single
 * object which may then be queried in various ways to test for
 * overlaps, inclusion, exclusion, etc with other schedules.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: ScheduleElement.java,v 1.3 2001-08-22 20:14:15 mthome Exp $
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

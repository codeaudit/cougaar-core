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
 
 /** NewScheduleElement extends ScheduleElement and provides
   * setter methods for building valid ScheduleElement objects.
   *
   * @author  ALPINE <alpine-software@bbn.com>
   * @version $Id: NewScheduleElement.java,v 1.3 2001-07-16 15:19:59 jwinston Exp $
   **/
 	 
public interface NewScheduleElement extends ScheduleElement {
 	
  /** @param startdate Set Start time for the time interval */
  void setStartDate(Date startdate);
	
  /** @param starttime Set Start time for the time interval */
  void setStartTime(long starttime);

  /** Note that end time is the <em>open</em> end of the interval.
   * @param enddate Set End time for the time interval 
   **/
  void setEndDate(Date enddate);
	
  /** Note that end time is the <em>open</em> end of the interval.
   * @param endtime Set End time for the time interval 
   **/
  void setEndTime(long endtime);

  /** One shot setter
   * @param starttime Set Start time for the time interval 
   * @param endtime Set End time for the time interval. 
   * Note that end time is the <em>open</em> end of the interval.
   */
  void setStartEndTimes(long starttime, long endtime);
}

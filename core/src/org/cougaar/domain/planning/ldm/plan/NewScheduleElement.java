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
 
 /** NewScheduleElement extends ScheduleElement and provides
   * setter methods for building valid ScheduleElement objects.
   *
   * @author  ALPINE <alpine-software@bbn.com>
   * @version $Id: NewScheduleElement.java,v 1.4 2001-08-22 20:14:14 mthome Exp $
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

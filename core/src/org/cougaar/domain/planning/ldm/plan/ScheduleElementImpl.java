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

import org.cougaar.domain.planning.ldm.plan.NewScheduleElement;
import org.cougaar.domain.planning.ldm.plan.ScheduleElement;

import java.util.Date;

/**
 * Base class for ScheduleElement.  Millisecond accuracy times are stored
 * in seconds since java epoch start, GMT.  Start time is closed, end time is open.
 */

public class ScheduleElementImpl 
  implements ScheduleElement, NewScheduleElement, java.io.Serializable
{
  protected long stime = 0;
  protected long etime = 0;

  /** no-arg constructor */
  public ScheduleElementImpl () { }
        
  /** constructor for factory use that takes the start and end dates */
  public ScheduleElementImpl(Date start, Date end) {
    stime = start.getTime();
    etime = end.getTime();
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }

  /** constructor for factory use that takes the start and end dates */
  public ScheduleElementImpl(long start, long end) {
    stime = start;
    etime = end;
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }
        
  public Date getStartDate() { return new Date(stime); }
  public long getStartTime() { return stime; }
  public Date getEndDate() { return new Date(etime); }
  public long getEndTime() { return etime; }
        
        
  /** @return boolean whether the date is included in this schedule */
  public boolean included(Date date) {
    return included(date.getTime());
  }
        
  public boolean included(long time) {
    return ( (time >= stime) && (time < etime) );
  }

  /** @return boolean whether schedules overlap */
  public boolean overlapSchedule(ScheduleElement se) {
    long tstime = se.getStartTime();
    long tetime = se.getEndTime();
                
    return ( tstime < etime &&
             tetime > stime );
  }
        
  public boolean abutSchedule(ScheduleElement se) {
    long tstime = se.getStartTime();
    long tetime = se.getEndTime();
                
    return ( tstime == etime ||
             tetime == stime );
  }

  // NewSchedule interface implementations
        
  /** @param startdate Set Start time for the task */
  public void setStartDate(Date startdate) {
    stime = startdate.getTime();
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }
  
  public void setStartTime(long t) { 
    stime = t; 
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }

                
  /** @param enddate Set End time for the task */
  public void setEndDate(Date enddate) {
    etime = enddate.getTime();
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }
  public void setEndTime(long t) { 
    etime = t; 
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }
        
  public String toString() {
    return "<"+getStartDate()+"-"+getEndDate()+">";
  }

}

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

import java.util.Date;

/** An AspectValue that deals with Time
 * Note that longValue() will return the time in milliseconds.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 */
 
public class TimeAspectValue extends AspectValue {
  /** Simple Constructor that takes the time value in Date format.
   * @param type  The AspectType
   * @param adate  The associated date.
   * @return TimeAspectValue
   */
  public TimeAspectValue(int type, Date adate) {
    super(type, (double) adate.getTime());
  }
   
  /** Simple Constructor that takes the time value in long format.
   * @param type  The AspectType
   * @param along  The associated date.
   * @return TimeAspectValue
   */
  public TimeAspectValue(int type, long along) {
    super(type, (double) along);
  }
   
  /** @return Date The Date representation of the value of the aspect. */
  public Date dateValue() {
    return new Date(Math.round(value));
  }

  /** Alias for longValue() **/
  public long timeValue() {
    return longValue();
  }
  
  public Object clone() {
    return new TimeAspectValue(type, (long) value);
  }
}

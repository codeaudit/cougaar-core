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

import java.util.Date;
import java.text.SimpleDateFormat;

/** An AspectValue that deals with Time
 * Note that longValue() will return the time in milliseconds.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 */
 
public class TimeAspectValue extends AspectValue {
  /** Simple Constructor that takes the time value in Date format.
   * @param type  The AspectType
   * @param adate  The associated date.
   */
  public TimeAspectValue(int type, Date adate) {
    super(type, (double) adate.getTime());
  }
   
  /** Simple Constructor that takes the time value in long format.
   * @param type  The AspectType
   * @param along  The associated date.
   */
  public TimeAspectValue(int type, long along) {
    super(type, (double) along);
  }
   
  /** @return The Date representation of the value of the aspect. */
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

  private static SimpleDateFormat dateTimeFormat =
    new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS z");

  private static Date formatDate = new Date();

  public String toString() {
    synchronized (formatDate) {
      formatDate.setTime((long) value);
      return dateTimeFormat.format(formatDate) + "[" + type + "]";
    }
  }
}

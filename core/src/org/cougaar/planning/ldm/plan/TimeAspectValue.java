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

/** An AspectValue implementation which stores a time.
 */
 
public class TimeAspectValue extends LongAspectValue {
  protected TimeAspectValue(int type, long value) {
    super(type,value);
  }

  public static AspectValue create(int type, Object o) {
    long l;
    if (o instanceof Date) {
      l = ((Date)o).getTime();
    } else if (o instanceof Number) {
      l = ((Number)o).longValue();
    } else {
      throw new IllegalArgumentException("Cannot create a TimeAspectValue from "+o);
    }
    return new TimeAspectValue(type,l);
  }
   
  public static AspectValue create(int type, long o) {
    return new TimeAspectValue(type,o);
  }


  /** @return The Date representation of the value of the aspect. */
  public Date dateValue() {
    return new Date(longValue());
  }

  /** Alias for longValue() **/
  public long timeValue() {
    return longValue();
  }
  

  // not thrilled with this...
  private static SimpleDateFormat dateTimeFormat =
    new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS z");
  private static Date formatDate = new Date();

  public String toString() {
    synchronized (formatDate) {
      formatDate.setTime(longValue());
      return dateTimeFormat.format(formatDate) + "[" + getType() + "]";
    }
  }
}

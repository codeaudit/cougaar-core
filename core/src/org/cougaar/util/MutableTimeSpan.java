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

package org.cougaar.util;

import java.util.*;

/**
 * Implementation of NewTimeSpan interface
 * @see NewTimeSpan
 */
public class MutableTimeSpan implements NewTimeSpan {

  private long myStartTime = MIN_VALUE;
  private long myEndTime = MAX_VALUE;

  /**
   * Constructor - startTime initialized to TimeSpan.MIN_VALUE, endTime
   * initialized to TimeSpan.MAX_VALUE
   */
  public MutableTimeSpan() {
  }

  /** The first point in time to be considered part of the 
   * interval.
   * @return MIN_VALUE IFF unbounded.
   **/
  public long getStartTime() {
    return myStartTime;
  }

  /** The first point in time after start to be considered
   * <em> not </em> part of the interval.
   * @return MAX_VALUE IFF unbounded.
   **/
  public long getEndTime() {
    return myEndTime;
  }

  /**
   * setTimeSpan - sets the start and end time of the time span
   * Expected to enforce that startTime < endTime
   * 
   * @throw IllegalArgumentException if startTime >= endTime
   */
  public void setTimeSpan(long startTime, long endTime) {
    if ((startTime >= MIN_VALUE) &&
        (endTime <= MAX_VALUE) &&
        (endTime >= startTime + EPSILON)) {
      myStartTime = startTime;
      myEndTime = endTime;
    } else {
      throw new IllegalArgumentException();
    }
  }
}

/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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

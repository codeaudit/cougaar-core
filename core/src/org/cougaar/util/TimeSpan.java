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
 * An abstraction of an object which starts at a known point
 * in time and ends before a known point in time.
 *
 * Note that the interval is closed with respect to the 
 * start point, open with respect to the end point and start must
 * be strictly less than end.
 *
 * An iterval where start==end is illegal, as it would indicate a 
 * negative 1 millisecond duration.  A point in time must be represented
 * with end = start+EPSILON.
 *
 * A TimeSpan that does not have well-defined start and end times is
 * also illegal. For example a set of TimeSpans might also be a
 * TimeSpan (e.g. Schedule) (with start and end times bounding the
 * start and end times of all the members of the set). If the set were
 * empty, there would be no well-defined start and end times. Such a
 * TimeSpan would be illegal.
 *
 * The values are usually interpreted to mean milliseconds in java time,
 * though there is nothing which actually requires these semantics.
 *
 * Note that while the interface is not required to be serializable,
 * most implementations will actually be so.
 **/
public interface TimeSpan 
{
  /** The minimum Time increment. **/
  long EPSILON = 1;

  /** A value to indicate unbounded StartTime.  
   * The actual value was chosen so that (MAX_VALUE-MIN_VALUE) is still a long. 
   **/
  long MIN_VALUE = -(Long.MAX_VALUE>>2);	//was Long.MIN_VALUE;
  
  /** A value to indicate unbounded EndTime.
   * The actual value was chosen so that (MAX_VALUE-MIN_VALUE) is still a long. 
   **/
  long MAX_VALUE = (Long.MAX_VALUE>>2);  	//was Long.MAX_VALUE;

  /** The first point in time to be considered part of the 
   * interval.
   * @return MIN_VALUE IFF unbounded.
   **/
  long getStartTime();
  
  /** The first point in time after start to be considered
   * <em> not </em> part of the interval.
   * @return MAX_VALUE IFF unbounded.
   **/
  long getEndTime();
}

/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import java.util.*;

/**
 * Adds ability to set start and end time of a TimeSpan
 *
 * @see TimeSpan
  */

public interface NewTimeSpan extends TimeSpan {

  /**
   * setTimeSpan - sets the start and end time of the time span
   * Expected to enforce that startTime < endTime
   * 
   * Should throw IllegalArgumentException if startTime >= endTime
   */
  public void setTimeSpan(long startTime, long endTime);
}

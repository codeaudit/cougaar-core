/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

/** Interface for Periodic Alarms which are alarms which do not
 * naturally expire.
 *  hasExpired should only be true when the alarm was cancelled.
 **/

public interface PeriodicAlarm extends Alarm {
  /** start counting down again.  The current time is passed in
   * so that the computation of the next expirationtime can 
   * take it into account - typically, by adding some number of
   * millis to currentTime.
   **/
  void reset(long currentTime);
}

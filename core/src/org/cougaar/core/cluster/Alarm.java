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

/** Interface for Alp-time-based Alarms.  See
 * ClusterServesPlugIn.addAlarm(Alarm).
 **/

public interface Alarm {
  /** @return absolute time (in milliseconds) that the Alarm should
   * go off.  
   * This value must be implemented as a fixed value.
   **/
  long getExpirationTime();
  
  /** Called by the cluster clock when clock-time >= getExpirationTime().
   * The system will attempt to Expire the Alarm as soon as possible on 
   * or after the ExpirationTime, but cannot guarantee any specific
   * maximum latency.
   * NOTE: this will be called in the thread of the cluster clock.  
   * Implementations should make certain that this code does not block
   * for a significant length of time.
   * If the alarm has been canceled, this should be a no-op.
   **/
  void expire();

  /** @return true IFF the alarm has rung (expired) or was canceled. **/
  boolean hasExpired();

  /** can be called by a client to cancel the alarm.  May or may not remove
   * the alarm from the queue, but should prevent expire from doing anything.
   * @returns false IF the the alarm has already expired or was already canceled.
   **/
  boolean cancel();
}

/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.agent.service.alarm;

/** Interface for Alp-time-based Alarms.
 *
 * @see org.cougaar.core.service.AlarmService
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
   * @return false IF the the alarm has already expired or was already canceled.
   **/
  boolean cancel();
}

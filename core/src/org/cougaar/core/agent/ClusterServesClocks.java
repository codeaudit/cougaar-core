/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.agent;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.mts.MessageAddress;

/**
 * <b>ONLY</b> for use by AlarmServiceProvider and 
 * DemoControlServiceProvider.
 *
 * Will be replaced with service-based API.
 */
public interface ClusterServesClocks  {

  /**
   * @return MessageAddress the MessageAddress associated with 
   * the Cluster where the Plugin is plugged in.
   */
  MessageAddress getMessageAddress();
        
  /**
   * This method sets the COUGAAR scenario time to a specific time
   * in the future, leaving the clock stopped.
   * Time is in milliseconds.
   * Equivalent to setTime(time, false);
   * <em>Only UI Plugins controlling the demonstration should use
   * this method.</em>
  **/
  void setSocietyTime(long time);

  /** General form of setTime, allowing the clock to be left running.
   * <em>Only UI Plugins controlling the demonstration should use
   * this method.</em>
   **/
  void setSocietyTime(long time, boolean leaveRunning);

  /**
   * Changes the rate at which execution time advances. There is no
   * discontinuity in the value of execution time; it flows smoothly
   * from the current rate to the new rate.
   **/
  void setSocietyTimeRate(double newRate);

  /**
   * This method advances the COUGAAR scenario time a period of time
   * in the future, leaving the clock stopped.
   * Time is in milliseconds.
   * Equivalent to advanceTime(timePeriod, false);
   * <em>Only UI Plugins controlling the demonstration should use
   * this method.</em>
   **/
  void advanceSocietyTime(long timePeriod);

  /** General form of advanceTime, allowing the clock to be left running.
   * <em>Only UI Plugins controlling the demonstration should use
   * this method.</em>
   **/
  void advanceSocietyTime(long timePeriod, boolean leaveRunning);

  /** General form of advanceTime, allowing the clock to be left running at a new rate.
   * <em>Only UI Plugins controlling the demonstration should use
   * this method.</em>
   **/
  void advanceSocietyTime(long timePeriod, double newRate);

  /**
   * Set a series of time parameter changes. The number of such
   * changes is limited. See ExecutionTimer.create() for details.
   **/
  void advanceSocietyTime(ExecutionTimer.Change[] changes);

  /** General form of advanceNodeTime, allowing the clock to be left running at a new rate.
   * <em>Only UI Plugins controlling the demonstration should use
   * this method.</em>
   * This affects the time on this node only.
   **/
  void advanceNodeTime(long timePeriod, double newRate);

  /** General form of setNodeTime, allowing the clock to be left running at a new rate.
   * <em>Only UI Plugins controlling the demonstration should use
   * this method.</em>
   * This affects the time on this node only.
   **/
  void setNodeTime(long time, double newRate);

  /** General form of setNodeTime, allowing the clock to be left running at a new rate.
   * <em>Only UI Plugins controlling the demonstration should use
   * this method.</em>
   * This affects the time on this node only.
   **/
  void setNodeTime(long time, double newRate, long changeTime);
  
  /**
   * Get the current execution time rate.
   **/
  double getExecutionRate();

  /**
   * This method gets the current COUGAAR scenario time. 
   * The returned time is in milliseconds.
   **/
  long currentTimeMillis( );

  /**
   * Called by a plugin to schedule an Alarm to ring 
   * at some future Scenario time.
   * This alarm functions over Scenario time which may be discontinuous
   * and/or offset from realtime.
   * If you want real (wallclock time, use addRealTimeAlarm instead).
   * Most plugins will want to just use the wake() functionality,
   * which is implemented in terms of addAlarm().
   **/
  void addAlarm(Alarm alarm);

  /**
   * Called by a plugin to schedule an Alarm to ring 
   * at some future Real (wallclock) time.
   **/
  void addRealTimeAlarm(Alarm alarm);
}

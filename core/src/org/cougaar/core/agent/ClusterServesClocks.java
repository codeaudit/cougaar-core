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
 * Common interface between time control components ({@link
 * org.cougaar.core.agent.AlarmComponent} and {@link
 * org.cougaar.core.agent.DemoControl}) and their service
 * providers ({@link
 * org.cougaar.core.agent.service.alarm.AlarmServiceProvider} and
 * {@link
 * org.cougaar.core.agent.service.democontrol.DemoControlServiceProvider}). 
 * <p>
 * Long ago these two time services were tightly intertwined, but with
 * recent refactorings these could be separated and this interface
 * could be removed.
 */
public interface ClusterServesClocks  {
  // alarm service:
  long currentTimeMillis();
  void addAlarm(Alarm alarm);
  void addRealTimeAlarm(Alarm alarm);
  // demo service:
  MessageAddress getMessageAddress();
  void setSocietyTime(long time);
  void setSocietyTime(long time, boolean leaveRunning);
  void setSocietyTimeRate(double newRate);
  void advanceSocietyTime(long timePeriod);
  void advanceSocietyTime(long timePeriod, boolean leaveRunning);
  void advanceSocietyTime(long timePeriod, double newRate);
  void advanceSocietyTime(ExecutionTimer.Change[] changes);
  void advanceNodeTime(long timePeriod, double newRate);
  void setNodeTime(long time, double newRate);
  void setNodeTime(long time, double newRate, long changeTime);
  double getExecutionRate();
}

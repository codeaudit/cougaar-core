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

package org.cougaar.core.service;

import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.component.Service;

/**
 * This service controls the execution time in {@link
 * AlarmService#currentTimeMillis} and {@link
 * AlarmService#addAlarm}.
 */
public interface DemoControlService extends Service {
  /**
   * Get the local node's execution rate.
   */
  double getExecutionRate();

  /**
   * Modify the local node's execution time.
   */
  void advanceNodeTime(long period, double rate);
  void setNodeTime(long time, double rate);
  void setNodeTime(long time, double rate, long changeTime);

  /**
   * Modify the execution time on all nodes in the society.
   * <p>
   * These methods have known scalability issues:<ul>
   * <li>What if the naming service finds a partial node list?</li>
   * <li>What if a node is unreachable?</li> 
   * <li>What if it takes too long to reach a node?</li> 
   * </ul> 
   */
  void setSocietyTime(long time);
  void setSocietyTime(long time, boolean forceRunning);
  void setSocietyTimeRate(double rate);
  void advanceSocietyTime(long period);
  void advanceSocietyTime(long period, boolean forceRunning);
  void advanceSocietyTime(long period, double rate);
  void advanceSocietyTime(ExecutionTimer.Change[] changes);
}

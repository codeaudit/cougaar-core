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

/** a Service for controlling COUGAAR demonstration facilities 
 * from loaded components. 
 **/

public interface DemoControlService extends Service {
  /**
   * These all send remote AdvanceClockMessage messages
   * to the whole society (ie the old way)
   */
  void setSocietyTime(long time);
  void setSocietyTime(long time, boolean foo);
  void setSocietyTimeRate(double rate);
  void advanceSocietyTime(long period);
  void advanceSocietyTime(long period, boolean foo);
  void advanceSocietyTime(long period, double rate);
  void advanceSocietyTime(ExecutionTimer.Change[] changes);
  
  /**
   * These all send remote AdvanceClockMessage messages
   * to the whole society (ie the old way)
   */
  void advanceNodeTime(long period, double rate);
  void setNodeTime(long time, double rate);
  void setNodeTime(long time, double rate, long changeTime);
  double getExecutionRate();
}

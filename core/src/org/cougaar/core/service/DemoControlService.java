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
  double getExecutionRate();
}

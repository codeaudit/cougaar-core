/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

package org.cougaar.core.agent.service.democontrol;

import org.cougaar.core.agent.ClusterServesClocks;
import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.DemoControlService;

/** a Service for getting at Alarm information
 **/

public class DemoControlServiceProvider implements ServiceProvider {
  private ClusterServesClocks agentClock;

  public DemoControlServiceProvider(ClusterServesClocks agentClock) {
    this.agentClock = agentClock;
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (DemoControlService.class.isAssignableFrom(serviceClass)) {
      return new DemoControlServiceImpl();
    } else {
      return null;
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
  }

  private final class DemoControlServiceImpl implements DemoControlService {
    public void setTime(long time) { agentClock.setTime(time); }
    public void setTime(long time, boolean foo) {agentClock.setTime(time,foo);}
    public void setTimeRate(double rate) {agentClock.setTimeRate(rate); }
    public void advanceTime(long period) {agentClock.advanceTime(period); }
    public void advanceTime(long period, boolean foo) {agentClock.advanceTime(period,foo); }
    public void advanceTime(long period, double rate) {agentClock.advanceTime(period,rate); }
    public void advanceTime(ExecutionTimer.Change[] changes) {agentClock.advanceTime(changes); }
    public double getExecutionRate() { return agentClock.getExecutionRate(); }
  }
}

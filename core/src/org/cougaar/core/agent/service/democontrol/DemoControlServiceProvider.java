/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.agent.service.democontrol;

import org.cougaar.core.agent.ClusterServesClocks;
import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.DemoControlService;

/**
 * The service provider for the DemoControlService, based upon the
 * ClusterServesClocks API.
 */
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
    public void setSocietyTime(long time) { agentClock.setSocietyTime(time); }
    public void setSocietyTime(long time, boolean foo) {agentClock.setSocietyTime(time,foo);}
    public void setSocietyTimeRate(double rate) {agentClock.setSocietyTimeRate(rate); }
    public void advanceSocietyTime(long period) {agentClock.advanceSocietyTime(period); }
    public void advanceSocietyTime(long period, boolean foo) {agentClock.advanceSocietyTime(period,foo); }
    public void advanceSocietyTime(long period, double rate) {agentClock.advanceSocietyTime(period,rate); }
    public void advanceSocietyTime(ExecutionTimer.Change[] changes) {agentClock.advanceSocietyTime(changes); }
    public double getExecutionRate() { return agentClock.getExecutionRate(); }
    public void advanceNodeTime(long period, double rate) {
      agentClock.advanceNodeTime(period, rate);
    }
    public void setNodeTime(long time, double rate) {
      agentClock.setNodeTime(time, rate);
    }
    public void setNodeTime(long time, double rate, long changeTime) {
      agentClock.setNodeTime(time, rate, changeTime);
    }

  }
}

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

package org.cougaar.core.agent.service.alarm;

import org.cougaar.core.agent.ClusterServesClocks;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.AlarmService;

/**
 * The service provider for the AlarmService, based upon the
 * ClusterServesClocks API.
 */
public class AlarmServiceProvider implements ServiceProvider {
  private ClusterServesClocks agentClock;

  public AlarmServiceProvider(ClusterServesClocks agentClock) {
    this.agentClock = agentClock;
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (AlarmService.class.isAssignableFrom(serviceClass)) {
      return new AlarmServiceImpl();
    } else {
      return null;
    }
  }

  public void releaseService(
      ServiceBroker sb, Object requestor,
      Class serviceClass, Object service) {
  }

  private final class AlarmServiceImpl implements AlarmService {
    public long currentTimeMillis() {
      return agentClock.currentTimeMillis();
    }
    public void addAlarm(Alarm alarm) {
      agentClock.addAlarm(alarm);
    }
    public void addRealTimeAlarm(Alarm alarm) {
      agentClock.addRealTimeAlarm(alarm);
    }
  }
}

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
import org.cougaar.core.agent.service.alarm.AlarmServiceProvider;
import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.service.NaturalTimeService;
import org.cougaar.core.node.service.RealTimeService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The AlarmComponent adds the agent's {@link AlarmService}, based
 * upon the node-level {@link RealTimeService} and
 * {@link NaturalTimeService}.
 * 
 * @see AlarmService 
 */
public final class AlarmComponent
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private RealTimeService rTimer;
  private NaturalTimeService xTimer;

  private AlarmServiceProvider asp;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    // get execution timer
    xTimer = (NaturalTimeService) 
      sb.getService(this, NaturalTimeService.class, null);

    // get real timer
    rTimer = (RealTimeService) 
      sb.getService(this, RealTimeService.class, null);

    // add alarm service
    ClusterServesClocks alarmClock =
      new AlarmClockAdapter(xTimer, rTimer);
    asp =
      new AlarmServiceProvider(alarmClock);
    sb.addService(AlarmService.class, asp);
  }

  public void unload() {
    super.unload();

    sb.revokeService(AlarmService.class, asp);
    asp = null;

    if (rTimer != null) {
      sb.releaseService(this, RealTimeService.class, rTimer);
      rTimer = null;
    }

    if (xTimer != null) {
      sb.releaseService(this, NaturalTimeService.class, xTimer);
      xTimer = null;
    }
  }
  private static final class AlarmClockAdapter
    implements ClusterServesClocks {
      private final NaturalTimeService xTimer;
      private final RealTimeService rTimer;
      public AlarmClockAdapter(
          NaturalTimeService xTimer,
          RealTimeService rTimer) {
        this.xTimer = xTimer;
        this.rTimer = rTimer;
      }
      // alarm service:
      public long currentTimeMillis() {
        return xTimer.currentTimeMillis();
      }
      public void addAlarm(Alarm alarm) {
        xTimer.addAlarm(alarm);
      }
      public void addRealTimeAlarm(Alarm alarm) {
        rTimer.addAlarm(alarm);
      }
      // demo service:
      private void die() { throw new UnsupportedOperationException(); }
      public MessageAddress getMessageAddress() { die(); return null; }
      public void setSocietyTime(long time) { die(); }
      public void setSocietyTime(long time, boolean leaveRunning) { die(); }
      public void setSocietyTimeRate(double newRate) { die(); }
      public void advanceSocietyTime(long timePeriod) { die(); }
      public void advanceSocietyTime(long timePeriod, boolean leaveRunning) { die(); }
      public void advanceSocietyTime(long timePeriod, double newRate) { die(); }
      public void advanceSocietyTime(ExecutionTimer.Change[] changes) { die(); }
      public void advanceNodeTime(long timePeriod, double newRate) {die();}
      public void setNodeTime(long time, double newRate) {die();}
      public void setNodeTime(long time, double newRate, long changeTime) {die();}
      public double getExecutionRate() { die(); return -1; }
    }
}

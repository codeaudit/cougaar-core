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

package org.cougaar.core.agent;

import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.agent.service.democontrol.DemoControlServiceProvider;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageHandler;
import org.cougaar.core.node.service.NaturalTimeService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DemoControlService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The DemoControl component adds the agent's
 * {@link DemoControlService}.
 */
public final class DemoControl
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private MessageAddress localAgent;
  private NaturalTimeService xTimer;
  private MessageSwitchService mss;

  private ServiceProvider dcsp;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    // get execution timer
    xTimer = (NaturalTimeService) 
      sb.getService(this, NaturalTimeService.class, null);
    if (xTimer == null) {
      throw new RuntimeException(
          "Unable to obtain NaturalTimeService");
    }

    mss = (MessageSwitchService)
      sb.getService(this, MessageSwitchService.class, null);
    if (mss == null) {
      throw new RuntimeException(
          "Unable to obtain MessageSwitchService");
    }

    // register message handler for "AdvanceClock" messages
    MessageHandler mh = new MessageHandler() {
      public boolean handleMessage(Message message) {
        if (message instanceof AdvanceClockMessage) {
          AdvanceClockMessage acm = (AdvanceClockMessage) message;
          xTimer.setParameters(acm.getParameters());
          return true;
        } else {
          return false;
        }
      }
    };
    mss.addMessageHandler(mh);

    // add demo control
    ClusterServesClocks demoClock = new DemoClockAdapter();
    dcsp =
      new DemoControlServiceProvider(demoClock);
    sb.addService(DemoControlService.class, dcsp);
  }

  public void unload() {
    super.unload();

    sb.revokeService(DemoControlService.class, dcsp);
    dcsp = null;

    if (mss != null) {
      // mss.unregister?
      sb.releaseService(this, MessageSwitchService.class, mss);
      mss = null;
    }

    if (xTimer != null) {
      sb.releaseService(this, NaturalTimeService.class, xTimer);
      xTimer = null;
    }
  }

  private final class DemoClockAdapter
    implements ClusterServesClocks {
      // alarm service:
      private void die() { throw new UnsupportedOperationException(); }
      public long currentTimeMillis() { die(); return -1; }
      public void addAlarm(Alarm alarm) { die(); }
      public void addRealTimeAlarm(Alarm alarm) { die(); }
      // demo service:
      public MessageAddress getMessageAddress() {
        return localAgent;
      }
      public void setSocietyTime(long time) {
        sendAdvanceClockMessage(
            time, true, 0.0, false, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void setSocietyTime(long time, boolean running) {
        sendAdvanceClockMessage(
            time, true, 0.0, running, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void setSocietyTimeRate(double newRate) {
        sendAdvanceClockMessage(
            0L, false, newRate, false, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void advanceSocietyTime(long timePeriod){
        sendAdvanceClockMessage(
            timePeriod, false, 0.0, false, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void advanceSocietyTime(long timePeriod, boolean running){
        sendAdvanceClockMessage(
            timePeriod, false, 0.0, running, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void advanceSocietyTime(long timePeriod, double newRate){
        sendAdvanceClockMessage(
            timePeriod, false, newRate, false, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void advanceSocietyTime(ExecutionTimer.Change[] changes) {
        ExecutionTimer.Parameters[] params = xTimer.createParameters(changes);
        for (int i = 0; i < params.length; i++) {
          sendAdvanceClockMessage(params[i]);
        }
      }
      public void advanceNodeTime(long timePeriod, double newRate) {
        ExecutionTimer.Parameters newParameters =
          xTimer.createParameters(
              timePeriod,
              false, // millisIsAbsolute,
              newRate,
              false, // forceRunning,
              NaturalTimeService.DEFAULT_CHANGE_DELAY,
              false); // changeIsAbsolute

        xTimer.setParameters(newParameters);
      }
      public void setNodeTime(long time, double newRate) {
        ExecutionTimer.Parameters newParameters =
          xTimer.createParameters(
              time,
              true, // millisIsAbsolute,
              newRate,
              false, // forceRunning,
              NaturalTimeService.DEFAULT_CHANGE_DELAY,
              false); // changeIsAbsolute
        xTimer.setParameters(newParameters);
      }
      public void setNodeTime(long time, double newRate, long changeTime) {
        ExecutionTimer.Parameters newParameters =
          xTimer.createParameters(
              time,
              true, // millisIsAbsolute,
              newRate,
              false, // forceRunning,
              changeTime,
              true); // changeIsAbsolute
        xTimer.setParameters(newParameters);
      }

      public double getExecutionRate() {
        return xTimer.getRate();
      }
      private void sendAdvanceClockMessage(long millis,
          boolean millisIsAbsolute,
          double newRate,
          boolean forceRunning,
          long changeDelay)
      {
        ExecutionTimer.Parameters newParameters =
          xTimer.createParameters(
              millis, millisIsAbsolute, newRate,
              forceRunning, changeDelay, false);
        sendAdvanceClockMessage(newParameters);
      }
      private void sendAdvanceClockMessage(
          ExecutionTimer.Parameters newParameters) {
        AdvanceClockMessage acm =
          new AdvanceClockMessage(localAgent, newParameters);
        sendAdvanceClockMessage(acm);
      }
      private void sendAdvanceClockMessage(AdvanceClockMessage acm) {
        mss.sendMessage(acm);
      }
    }
}

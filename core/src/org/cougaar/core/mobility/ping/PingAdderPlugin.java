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
 **/

package org.cougaar.core.mobility.ping;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

/**
 * Plugin that creates Pings to remote agents,
 * repeats the pings up to a specified limit.
 */
public class PingAdderPlugin
extends ComponentPlugin 
{

  private static final UnaryPredicate STATE_PRED =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof MyStartTime);
      }
    };

  private long startTime;
  private long timeoutMillis;
  private int limit;
  private boolean ignoreRollback;
  private int sendFillerSize;
  private int echoFillerSize;
  private Set /*<MessageAddress>*/ targetIds;

  private MessageAddress agentId;

  private LoggingService log;
  private UIDService uidService;

  private WakeAlarm wakeAlarm;

  private MyStartTime myStartTime;

  public void load() {
    super.load();

    // get the log
    log = (LoggingService)
      getServiceBroker().getService(
          this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    // get agent id
    AgentIdentificationService agentIdService = 
      (AgentIdentificationService) 
      getServiceBroker().getService(
          this, AgentIdentificationService.class, null);
    if (agentIdService == null) {
      throw new RuntimeException(
          "Unable to obtain agent-id service");
    }
    agentId = agentIdService.getMessageAddress();
    getServiceBroker().releaseService(
        this, AgentIdentificationService.class, agentIdService);
    if (agentId == null) {
      throw new RuntimeException(
          "Agent id is null");
    }

    // get UID service
    uidService = (UIDService) 
      getServiceBroker().getService(
          this, UIDService.class, null);
    if (uidService == null) {
      throw new RuntimeException(
          "Unable to obtain agent-id service");
    }

    // parse parameters
    long nowTime = System.currentTimeMillis();
    List params = (List) getParameters();
    int targetsIdx;
    try {
      String s = (String) params.get(0);
      boolean b = s.startsWith("+");
      if (b) {
        s = s.substring(1);
      }
      startTime = Long.parseLong(s);
      if (b) {
        startTime += nowTime;
      }
      if (startTime < nowTime) {
        startTime = nowTime;
      }
      timeoutMillis = Long.parseLong((String) params.get(1));
      String s2 = (String) params.get(2);
      ignoreRollback = "true".equals(s2);
      if (ignoreRollback || "false".equals(s2)) {
        // new style usage
        limit = Integer.parseInt((String) params.get(3));
        sendFillerSize = Integer.parseInt((String) params.get(4));
        echoFillerSize = Integer.parseInt((String) params.get(5));
        targetsIdx = 6;
      } else {
        // old style usage
        ignoreRollback = false;
        limit = Integer.parseInt(s2);
        sendFillerSize = -1;
        echoFillerSize = -1;
        targetsIdx = 3;
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Expected parameters:\n"+
          " [+]startMillis,\n"+
          " timeoutMillis,\n"+
          " (repeatLimit |\n"+
          "    ignoreRollback,\n"+
          "    repeatLimit,\n"+
          "    sendFillerSize,\n"+
          "    echoFillerSize),\n"+
          " [,target0, .. targetN]),\n"+
          " not "+params);
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Start time is "+startTime+
          ", now is "+System.currentTimeMillis());
    }

    // parse optional set of targets
    for (int i = targetsIdx, n = params.size(); i < n; i++) {
      String si = (String) params.get(i);
      MessageAddress ai = MessageAddress.getMessageAddress(si);
      if (agentId.equals(ai)) {
        throw new IllegalArgumentException(
            "Agent "+agentId+" matches target["+i+"] "+ai);
      }
      // add to targets
      if (n == targetsIdx) { 
        targetIds = Collections.singleton(ai);
      } else {
        if (targetIds == null) {
          targetIds = new HashSet(n - targetsIdx);
        }
        targetIds.add(ai);
      }
    }
  }

  public void suspend() {
    if (wakeAlarm != null) {
      wakeAlarm.cancel();
    }
    super.suspend();
  }

  public void resume() {
    super.resume();
    if (myStartTime != null) {
      setAlarm();
    }
  }

  public void unload() {
    if (uidService != null) {
      getServiceBroker().releaseService(
          this, UIDService.class, uidService);
      uidService = null;
    }
  }

  protected void setupSubscriptions() {

    if ((targetIds == null) ||
        (targetIds.isEmpty())) {
      // nothing to do?
      return;
    }

    if (!(blackboard.didRehydrate())) {
      if (startTime > 0) {
        myStartTime = new MyStartTime();
        myStartTime.time = startTime;
        blackboard.publishAdd(myStartTime);
        setAlarm();
      } else {
        createPings();
      }
    } else {
      Collection c = blackboard.query(STATE_PRED);
      if ((c != null) && (!(c.isEmpty()))) {
        myStartTime = (MyStartTime) c.iterator().next();
        startTime = myStartTime.time;
        setAlarm();
      }
    }
  }

  private void createPings() {
    // create pings
    if (targetIds != null) {
      Iterator targetIter = targetIds.iterator();
      for (int i = 0, n = targetIds.size(); i < n; i++) {
        MessageAddress ai = (MessageAddress) targetIter.next();
        UID uid = uidService.nextUID();
        Ping ping = new PingImpl(
            uid, agentId, ai, timeoutMillis, 
            ignoreRollback, limit,
            sendFillerSize, echoFillerSize);
        blackboard.publishAdd(ping);

        if (log.isDebugEnabled()) {
          log.debug(
              "Created ping "+i+" of "+n+
              ", from "+agentId+" to "+ping.getTarget()+
              ", uid "+uid);
        }
      }
    }
  }

  private void setAlarm() {
    if (log.isDebugEnabled()) {
      log.debug("Will wake at "+startTime);
    }
    wakeAlarm = new WakeAlarm(startTime);
    alarmService.addRealTimeAlarm(wakeAlarm);
  }

  protected void execute() {

    if (myStartTime == null) {
      // already created
      return;
    }

    // wait for timer
    if ((wakeAlarm != null) &&
        (!(wakeAlarm.hasExpired()))) {
      // shouldn't happen
      return;
    }
    wakeAlarm = null;

    createPings();

    // did the add
    blackboard.publishRemove(myStartTime);
    myStartTime = null;
  }

  // class to remember if ADD was done yet
  private static class MyStartTime implements Serializable {
    public long time;
  }

  // nothing special..
  private class WakeAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    public WakeAlarm (long expirationTime) {
      expiresAt = expirationTime;
    }
    public long getExpirationTime() { 
      return expiresAt; 
    }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        getBlackboardService().signalClientActivity();
      }
    }
    public boolean hasExpired() { 
      return expired; 
    }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      return was;
    }
    public String toString() {
      return "WakeAlarm "+expiresAt+
        (expired?"(Expired) ":" ")+
        "for "+PingAdderPlugin.this.toString();
    }
  }

}

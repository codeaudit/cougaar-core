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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin that creates Pings to remote agents.
 * <p>
 * The PingTimerPlugin manages these pings and their timeouts.
 * <p>
 * Minimally the "target" plugin parameter should be specified,
 * e.g.:<pre>
 *   plugin=<i>this_class</i>(target=AgentX)
 * </pre>
 * otherwise this plugin does nothing.
 * <p>
 * The additional (optional) parameters are:
 * <table border=1>
 * <tr><th align=left>parameter</th>
 *     <th align=left>meaning</th>
 *     <th align=left>default</th></tr>
 * <tr><td>startTime=[+]millis</td>
 *     <td>when to add the ping(s)</td>
 *     <td>+0</td></tr>
 * <tr><td>delayMillis=millis</td>
 *     <td>minimum time between pings</td>
 *     <td>2000</td></tr>
 * <tr><td>timeoutMillis=millis</td>
 *     <td>maximum ping time, or negative for none</td>
 *     <td>-1</td></tr>
 * <tr><td>eventMillis=millis</td>
 *     <td>millis between cougaar events, or negative for none</td>
 *     <td>-1</td></tr>
 * <tr><td>eventCount=millis</td>
 *     <td>pings between cougaar events, or negative for none</td>
 *     <td>-1</td></tr>
 * <tr><td>limit=int</td>
 *     <td>number of pings, or negative for no limit</td>
 *     <td>-1</td></tr>
 * <tr><td>ignoreRollback=boolean</td>
 *     <td>ignore counter rollbacks upon agent restarts</td>
 *     <td>false</td></tr>
 * <tr><td>sendFillerSize=int</td>
 *     <td>pad outgoing ping byte size, or negative for none</td>
 *     <td>-1</td></tr>
 * <tr><td>echoFillerSize=int</td>
 *     <td>pad reply ping byte size, or negative for none</td>
 *     <td>-1</td></tr>
 * <tr><td>target=agent</td>
 *     <td>ping target address, which can't be the local agent</td>
 *     <td><i>none</i></td></tr>
 * <tr><td>target<i>N</i>=agent</td>
 *     <td>additional targets (N &gt;= 2)</td>
 *     <td><i>none</i></td></tr>
 * </table>
 */
public class PingAdderPlugin
extends ComponentPlugin 
{
  private static final String PROP_START_MILLIS = "startMillis";
  private static final long DEFAULT_START_MILLIS = 0;

  private static final UnaryPredicate STATE_PRED =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof MyStartTime);
      }
    };

  private long startTime;
  private Map props;
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

    // get parameters
    List params = (List) getParameters();
    props = new HashMap();
    for (int i = 0; i < params.size(); i++) {
      String s = (String) params.get(i);
      String name;
      String value;
      int sep = s.indexOf('=');
      if (sep >= 0) {
        name = s.substring(0, sep);
        value = s.substring(sep+1);
      } else {
        // backwards-compatibility
        switch (i) {
          case 0: name = PROP_START_MILLIS; break;
          case 1: name = PingImpl.PROP_TIMEOUT_MILLIS; break;
          case 2: name = PingImpl.PROP_LIMIT; break;
          case 3: name = "target"; break;
          default: name = "target"+(i-2); break;
        }
        value = s;
      }
      props.put(name, value);
    }

    String s = (String) props.get(PROP_START_MILLIS);
    boolean b;
    if (s == null) {
      b = true;
      startTime = DEFAULT_START_MILLIS;
    } else {
      b = s.startsWith("+");
      if (b) {
        s = s.substring(1);
      }
      startTime = Long.parseLong(s);
    }
    long nowTime = System.currentTimeMillis();
    if (b) {
      startTime += nowTime;
    }
    if (startTime < nowTime) {
      startTime = nowTime;
    }

    s = (String) props.get("target");
    int i = 1;
    while (s != null) {
      MessageAddress a = MessageAddress.getMessageAddress(s);
      if (agentId.equals(a)) {
        throw new IllegalArgumentException(
            "Agent "+agentId+" matches target["+i+"] "+a);
      }
      if (targetIds == null) {
        targetIds = Collections.singleton(a);
      } else {
        if (targetIds.size() == 1) {
          Set tmp = new HashSet();
          tmp.addAll(targetIds);
          targetIds = tmp;
        }
        targetIds.add(a);
      }
      ++i;
      s = (String) props.get("target"+i);
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
        Ping ping = new PingImpl(uid, agentId, ai, props);
        blackboard.publishAdd(ping);

        if (log.isDebugEnabled()) {
          log.debug("Created ping "+i+" of "+n+":\n"+ping);
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
        {
          org.cougaar.core.service.BlackboardService bbs = getBlackboardService();
          if (bbs != null) bbs.signalClientActivity();
        }
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

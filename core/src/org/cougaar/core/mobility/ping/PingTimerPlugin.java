/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
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
 * Plugin that periodically checks for timed-out pings
 * sent by its agent.
 * <p>
 * This plugin requires a single parameter: a <i>long</i>
 * indicating the time in milliseconds between timeout 
 * checks.
 */
public class PingTimerPlugin
extends ComponentPlugin 
{

  public static final long MIN_WAKE_MILLIS = 500;

  private long wakeMillis;

  private MessageAddress agentId;

  private LoggingService log;

  private IncrementalSubscription pingSub;

  private WakeAlarm wakeAlarm;

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

    // parse parameters
    List params = (List) getParameters();
    try {
      wakeMillis = Long.parseLong((String) params.get(0));
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Expected parameters:"+
          " (wakeMillis),"+
          " not "+params);
    }

    if (wakeMillis < MIN_WAKE_MILLIS) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Wake interval reset from from specified "+
            wakeMillis+" to minimum accepted value: "+
            MIN_WAKE_MILLIS);
      }
      wakeMillis = MIN_WAKE_MILLIS;
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
    if (!(pingSub.isEmpty())) {
      setAlarm();
    }
  }

  public void unload() {
    if ((log != null) && (log != LoggingService.NULL)) {
      getServiceBroker().releaseService(
          this, LoggingService.class, log);
      log = LoggingService.NULL;
    }
    super.unload();
  }

  protected void setupSubscriptions() {

    // subscribe to pings
    UnaryPredicate pingPred = createPingPredicate(agentId);
    pingSub = (IncrementalSubscription) 
      blackboard.subscribe(pingPred);

    if (!(pingSub.isEmpty())) {
      setAlarm();
    }
  }

  protected void execute() {

    // wait for timer
    if ((wakeAlarm != null) &&
        (!(wakeAlarm.hasExpired()))) {
      return;
    }
    wakeAlarm = null;

    boolean needAlarm = false;

    if (pingSub.hasChanged()) {
      Enumeration en = pingSub.getAddedList();
      if (en.hasMoreElements()) {
        needAlarm = true;
      }
    }

    Collection c = pingSub.getCollection();
    if (!(c.isEmpty())) {
      long nowTime = System.currentTimeMillis();
      int n = c.size();
      Iterator iter = c.iterator();
      for (int i = 0; i < n; i++) {
        Ping ping = (Ping) iter.next();
        if (checkPing(ping, nowTime)) {
          needAlarm = true;
        }
      }
    }

    if (needAlarm) {
      setAlarm();
    }
  }


  private boolean checkPing(Ping ping, long nowTime) {
    // check limit
    int limit = ping.getLimit();
    if ((limit > 0) &&
        (ping.getSendCount() >= limit)) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Ping "+ping.getUID()+
            " at send count "+
            ping.getSendCount()+
            " which exceeds limit "+limit);
      }
      return false;
    }
    // check error
    if (ping.getError() != null) {
      if (log.isErrorEnabled()) {
        log.error(
            "Ping "+ping.getUID()+
            " from "+agentId+" to "+
            ping.getTarget()+" failed: "+
            ping.getError());
      }
      pingSub.remove(ping);
      return false;
    }
    // check for no-reply timeout
    long timeoutMillis = ping.getTimeoutMillis();
    long sendTime = ping.getSendTime();
    long replyTime = ping.getReplyTime();
    if (replyTime < 0) {
      // no reply yet
      if ((timeoutMillis > 0) &&
          ((nowTime - sendTime) > timeoutMillis)) {
        // no response
        ping.setError(
            "Timeout (no response) after "+
            (nowTime - sendTime)+
            " milliseconds > max "+timeoutMillis);
        blackboard.publishChange(ping);
        return false;
      }
      // keep waiting
      if (log.isDebugEnabled()) {
        log.debug(
            "Keep waiting for response on ping "+
            ping.getUID()+
            ((timeoutMillis > 0) ?
             (" for at most another "+
              (nowTime - sendTime - timeoutMillis)+
              " milliseconds") :
             " forever"));
      }
      return true;
    }
    // check for late-reply timeout
    if ((timeoutMillis > 0) &&
        ((replyTime - sendTime) > timeoutMillis)) {
      // late response
      ping.setError(
          "Timeout (late response) after "+
          (replyTime - sendTime)+
          " milliseconds > max "+timeoutMillis);
      blackboard.publishChange(ping);
      return false;
    }
    // ping is okay
    if (log.isInfoEnabled()) {
      log.info(
          "Completed ping "+ping.getSendCount()+
          ((limit > 0) ? (" of "+limit) : "")+
          ", uid "+ping.getUID()+
          ", from "+agentId+" to "+ping.getTarget()+
          ", in "+
          (ping.getReplyTime() - ping.getSendTime())+
          " milliseconds");
    }
    // check for max pings
    if ((limit > 0) &&
        (ping.getSendCount() >= limit)) {
      // done
      if (log.isInfoEnabled()) {
        log.info(
            "Successfully completed all "+limit+
            " pings, removing ping "+ping.getUID());
      }
      pingSub.remove(ping);
      return false;
    }
    // reissue ping
    if (log.isInfoEnabled()) {
      log.info(
          "Send next ping ["+
          (1 + ping.getSendCount())+
          " / "+
          ((limit > 0) ? 
           Integer.toString(limit) : 
           "inf")+
          "] from "+
          agentId+" to "+ping.getTarget()+
          ", uid "+ping.getUID());
    }
    ping.recycle();
    blackboard.publishChange(ping);
    // must check ping later
    return true;
  }

  private void setAlarm() {
    // reissue the master alarm
    long t = System.currentTimeMillis() + wakeMillis;
    if (log.isDebugEnabled()) {
      log.debug(
          "Will check pending pings in "+wakeMillis+
          " milliseconds ("+t+")");
    }
    wakeAlarm = new WakeAlarm(t);
    alarmService.addRealTimeAlarm(wakeAlarm);
  }

  private static UnaryPredicate createPingPredicate(
      final MessageAddress agentId) {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Ping) {
          MessageAddress s = ((Ping) o).getSource();
          return agentId.equals(s);
        }
        return false;
      }
    };
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
        "for "+PingTimerPlugin.this.toString();
    }
  }

}

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

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.UnaryPredicate;

/**
 * Plugin that periodically checks for timed-out pings
 * sent by its agent.
 * <p>
 * This plugin requires a single parameter:<pre>
 *    wakeMillis=millis
 * </pre>
 * which is the time between periodic checks for ping
 * timeouts and delayed pings.  The minimum supported
 * value is 500 milliseconds.
 */
public class PingTimerPlugin
extends ComponentPlugin 
{
  public static final long MIN_WAKE_MILLIS = 500;

  private long wakeMillis;

  private MessageAddress agentId;

  private LoggingService log;
  private EventService eventService;

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

    // get event service
    eventService = (EventService)
      getServiceBroker().getService(
          this, EventService.class, null);
    if (eventService == null) {
      throw new RuntimeException(
          "Unable to obtain event service");
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
    Map props = new HashMap();
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
        name = "wakeMillis";
        value = s;
      }
      props.put(name, value);
    }

    String s = (String) props.get("wakeMillis");
    wakeMillis = (s == null ? -1 : Long.parseLong(s));

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
    if (eventService != null) {
      getServiceBroker().releaseService(
          this, EventService.class, eventService);
      eventService = null;
    }
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

    boolean needAlarm = false;
    long nowTime = System.currentTimeMillis();

    if (pingSub.hasChanged()) {
      for (Enumeration en = pingSub.getAddedList();
          en.hasMoreElements();
          ) {
        Ping p = (Ping) en.nextElement();
        if (log.isDetailEnabled()) {
          log.detail("observed ping ADD: \n"+p);
        }
        if (checkPing(p, nowTime)) {
          needAlarm = true;
        }
      }
      for (Enumeration en = pingSub.getChangedList();
          en.hasMoreElements();
          ) {
        Ping p = (Ping) en.nextElement();
        if (log.isDetailEnabled()) {
          log.detail("observed ping CHANGE: \n"+p);
        }
        if (checkPing(p, nowTime)) {
          needAlarm = true;
        }
      }
    }

    if (wakeAlarm == null) {
      // see above needAlarm
    } else if (wakeAlarm.hasExpired()) {
      if (log.isDetailEnabled()) {
        log.detail("alarm fired");
      }
      wakeAlarm = null;
      Collection c = pingSub.getCollection();
      if (!(c.isEmpty())) {
        int n = c.size();
        Iterator iter = c.iterator();
        for (int i = 0; i < n; i++) {
          Ping p = (Ping) iter.next();
          if (checkPing(p, nowTime)) {
            needAlarm = true;
          }
        }
      }
    } else {
      // we already have an alarm pending
      needAlarm = false;
    }

    if (needAlarm) {
      setAlarm();
    }
  }

  private boolean checkPing(Ping ping, long nowTime) {
    // check limit
    long sendCount = ping.getSendCount();
    int limit = ping.getLimit();
    if ((limit > 0) &&
        (sendCount >= limit)) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Ping "+ping.getUID()+" at send count "+sendCount+
            " which exceeds limit "+limit);
      }
      return false;
    }
    // check error
    if (ping.getError() != null) {
      if (log.isErrorEnabled()) {
        log.error(
            "Ping "+ping.getUID()+" from "+agentId+" to "+
            ping.getTarget()+" failed: "+ping.getError());
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
          (sendTime + timeoutMillis < nowTime)) {
        // no response
        ping.setError(
            "Ping "+ping.getUID()+" timeout (no response) after "+
            (nowTime - sendTime)+" milliseconds > max "+
            timeoutMillis);
        blackboard.publishChange(ping);
        return false;
      }
      // keep waiting
      if (log.isDebugEnabled()) {
        log.debug(
            "Ping "+ping.getUID()+" still waiting for a response"+
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
        (sendTime + timeoutMillis < replyTime)) {
      // late response
      ping.setError(
          "Ping "+ping.getUID()+" timeout (late response) after "+
          (replyTime - sendTime)+" milliseconds > max "+
          timeoutMillis);
      blackboard.publishChange(ping);
      return false;
    }
    // ping is okay
    //
    // check for max pings
    if ((limit > 0) &&
        (sendCount >= limit)) {
      // done
      if (log.isInfoEnabled()) {
        log.info(
            "Ping "+ping.getUID()+" successfully completed all "+
            limit+" pings from "+agentId+" to "+ping.getTarget()+
            ", most recent round-trip-time is "+
            (replyTime - sendTime)+" milliseconds");
      }
      pingSub.remove(ping);
      return false;
    }
    // check for minimum delay between pings
    long delayMillis = ping.getDelayMillis();
    if ((delayMillis > 0) &&
        (nowTime < replyTime + delayMillis)) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Ping "+ping.getUID()+" delaying for at least another "+
            (replyTime + delayMillis - nowTime)+" milliseconds");
      }
      return true;
    }
    // reissue ping
    if (log.isInfoEnabled()) {
      log.info(
          "Ping "+ping.getUID()+" completed ["+sendCount+" / "+
          ((limit > 0) ? Integer.toString(limit) : "inf")+
          "] from "+agentId+" to "+ping.getTarget()+
          " in "+(replyTime - sendTime)+" milliseconds");
      if (log.isDetailEnabled()) {
        log.detail("ping["+(sendCount+1)+"]: \n"+ping);
      }
    }
    boolean doEvent = false;
    long eventCount = ping.getEventCount();
    if (eventCount > 0 &&
        ((sendCount % eventCount) == 1)) {
      doEvent = true;
    }
    long eventMillis = ping.getEventMillis();
    if (eventMillis > 0 &&
        (eventMillis < nowTime - ping.getEventTime())) {
      doEvent = true;
    }
    if (doEvent) {
      if (eventService.isEventEnabled()) {
        long deltaTime = nowTime - ping.getEventTime();
        eventService.event(
            "Ping uid="+ping.getUID()+
            ", from="+agentId+
            ", to="+ping.getTarget()+
            ", count="+sendCount+
            ", limit="+limit+
            ", minRTT="+ping.getStatMinRTT()+
            ", maxRTT="+ping.getStatMaxRTT()+
            ", meanRTT="+ping.getStatMeanRTT()+
            ", stddevRTT="+ping.getStatStdDevRTT()+
            ", deltaTime="+deltaTime+
            ", deltaCount="+ping.getStatCount()+
	    ", sumSumSqrRTT="+ping.getStatSumSqrRTT()+",");
      }
      ping.setEventTime(nowTime);
      ping.resetStats();
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
        "for "+PingTimerPlugin.this.toString();
    }
  }

}

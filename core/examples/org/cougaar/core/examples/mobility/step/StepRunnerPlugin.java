/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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
package org.cougaar.core.examples.mobility.step;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.examples.mobility.ldm.Step;
import org.cougaar.core.examples.mobility.ldm.StepOptions;
import org.cougaar.core.examples.mobility.ldm.StepStatus;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mobility.ldm.MoveAgent;
import org.cougaar.core.mobility.ldm.MoveAgent$Status;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.Application;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin executes Steps where the step option's 
 * target is the local agent.
 * <p>
 * MoveAgent objects are created by this plugin.
 */
public class StepRunnerPlugin 
extends ComponentPlugin 
{

  private static final Application TOPOLOGY = 
    Application.getApplication("topology");

  private MessageAddress todd;
  private MessageAddress agentId;
  private MessageAddress nodeId;

  private IncrementalSubscription stepSub;
  private IncrementalSubscription moveSub;

  private LoggingService log;
  private DomainService domain;
  private WhitePagesService wps;

  private MobilityFactory mobilityFactory;

  // Non-persisted cache of blackboard objects, for quick access. 
  // On rehydration it is fully reconstructed from the blackboard.
  private Map idToEntry = new HashMap(13);

  // pending alarms
  //
  // FIXME could optimize to use treeset, keep fewer alarms, etc
  // for now this is workable
  private List pendingAlarms = new ArrayList(13);

  public void load() {
    super.load();

    // get the logger
    log = (LoggingService) 
      getServiceBroker().getService(
          this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    // get the agentId
    AgentIdentificationService agentIdService = 
      (AgentIdentificationService)
      getServiceBroker().getService(
          this,
          AgentIdentificationService.class,
          null);
    if (agentIdService == null) {
      throw new RuntimeException(
          "Unable to obtain agent-id service");
    }
    this.agentId = agentIdService.getMessageAddress();
    getServiceBroker().releaseService(
        this, AgentIdentificationService.class, agentIdService);
    if (agentId == null) {
      throw new RuntimeException(
          "Unable to obtain agent id");
    }
    todd=agentId;

    // get the nodeId
    NodeIdentificationService nodeIdService = 
      (NodeIdentificationService)
      getServiceBroker().getService(
          this,
          NodeIdentificationService.class,
          null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to obtain node-id service");
    }
    this.nodeId = nodeIdService.getMessageAddress();
    getServiceBroker().releaseService(
        this, NodeIdentificationService.class, nodeIdService);
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node id");
    }

    // get the mobility domain and factory
    this.domain = (DomainService)
      getServiceBroker().getService(
          this,
          DomainService.class,
          null);
    if (domain == null) {
      throw new RuntimeException(
          "Unable to obtain domain service");
    }
    this.mobilityFactory = 
      (MobilityFactory) domain.getFactory("mobility");
    if (mobilityFactory == null) {
      throw new RuntimeException(
          "Mobility factory (and domain) not enabled");
    }

    // get the white pages service
    this.wps = (WhitePagesService)
      getServiceBroker().getService(
          this,
          WhitePagesService.class,
          null);
    if (wps == null) {
      throw new RuntimeException(
          "Unable to obtain white pages service");
    }

    if (log.isDebugEnabled()) {
      log.debug(todd+"Loaded");
    }
  }

  public void unload() {
    if (wps != null) {
      getServiceBroker().releaseService(
          this, WhitePagesService.class, wps);
      wps = null;
    }
    if (domain != null) {
      getServiceBroker().releaseService(
          this, DomainService.class, domain);
      domain = null;
    }
    if ((log != null) &&
        (log != LoggingService.NULL)) {
      getServiceBroker().releaseService(
          this, LoggingService.class, log);
      log = LoggingService.NULL;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
    // subscribe to steps with a target matching this agent
    UnaryPredicate stepPred = createStepPredicate(agentId);
    stepSub = (IncrementalSubscription)
      blackboard.subscribe(stepPred);

    // subscribe to our own move requests
    UnaryPredicate movePred = createMovePredicate(agentId);
    moveSub = (IncrementalSubscription)
      blackboard.subscribe(movePred);

    if (blackboard.didRehydrate()) {
      // recreate cache from blackboard
      recreateEntries();
    } else {
      // create new cache
    }
  }

  protected void execute() {
    if (log.isDebugEnabled()) {
      log.debug(todd+"Execute");
    }

    // watch steps
    if (stepSub.hasChanged()) {
      //   added steps
      Enumeration en = stepSub.getAddedList();
      while (en.hasMoreElements()) {
        Step step = (Step) en.nextElement();
        StepStatus status = step.getStatus();
        // validate step status
        if (status.getState() != StepStatus.UNSEEN) {
          if (log.isErrorEnabled()) {
            log.error(todd+
                "Newly added step "+step.getUID()+
                " with prior status: "+status);
          }
        }
        addStep(step);
      }
      //   ignore changes: only this plugin can modify steps
      //   removed steps
      en = stepSub.getRemovedList();
      while (en.hasMoreElements()) {
        Step s = (Step) en.nextElement();
        removeStep(s);
      }
    }

    // watch move-reqs
    if (moveSub.hasChanged()) {
      // ignore adds: this plugin created them
      // changes fill in step details
      Enumeration en = moveSub.getChangedList();
      while (en.hasMoreElements()) {
        MoveAgent ma = (MoveAgent) en.nextElement();
        updateMove(ma);
      }
      // ignore removes: this plugin did it
    }

    // check alarms
    List l = getDueTicketIds();
    if (l != null) {
      handleDueTicketIds(l);
    }
  }

  private void recreateEntries() {
    // these should be empty
    idToEntry.clear();
    cancelAlarms();

    // recreate from blackboard contents
    Collection c = stepSub.getCollection();
    if (!(c.isEmpty())) {
      int n = c.size();
      Iterator iter = c.iterator();
      for (int i = 0; i < n; i++) {
        Step step = (Step) iter.next();
        switch (step.getStatus().getState()) {
          case StepStatus.UNSEEN:
          case StepStatus.PAUSED: 
          case StepStatus.RUNNING:
            addStep(step);
            break;
          case StepStatus.SUCCESS:
          case StepStatus.FAILURE:
          case StepStatus.TIMEOUT: 
            {
              // completed, but we still need it in the
              // table for later remote removal
              StepOptions options = step.getOptions();
              Ticket ticket = options.getTicket();
              Object id = ticket.getIdentifier();
              MoveAgent ma = findMove(id);
              Entry entry = new Entry(ticket, null, step);
              entry.moveAgent = ma;
              idToEntry.put(id, entry);
              if (log.isDebugEnabled()) {
                log.debug(
                    "Re-added entry "+id+" with move "+
                    ((ma != null) ? ma.getUID().toString() : "null"));
              }
            }
            break;
        }
      }
    }

    // remove unreferenced MoveAgent entries?
  }

  private void addStep(Step step) {

    // step status is typically UNSEEN
    // on restart it can be PAUSED or RUNNING

    StepOptions options = step.getOptions();
    Ticket ticket = options.getTicket();
    Object id = ticket.getIdentifier();

    StepStatus status = step.getStatus();

    // check for immediate timeout
    long nowTime = System.currentTimeMillis();
    long pauseTime = options.getPauseTime();
    long timeoutTime = options.getTimeoutTime();
    if (((timeoutTime > 0) &&
          (timeoutTime <= nowTime)) ||
        ((pauseTime > 0) &&
         (pauseTime <= nowTime) &&
         (status.getState() != StepStatus.RUNNING))) {

      if (status.getState() == StepStatus.RUNNING) {
        // restart from RUNNING
        // alarms are not persisted
        // remove possible MoveAgent object
        MoveAgent ma = findMove(id);
        if (ma != null) {
          // timeout, don't care if it succeeded!
          blackboard.publishRemove(ma);
        }
      }

      status = new StepStatus(
          StepStatus.TIMEOUT,
          ((status.getState() == StepStatus.RUNNING) ?
           status.getStartTime() :
           nowTime),
          nowTime,
          null);
      step.setStatus(status);
      blackboard.publishChange(step);

      if (log.isDebugEnabled()) {
        log.debug(todd+
            "Step "+step.getUID()+
            " added, but already timed out: "+
            " now ("+nowTime+"), "+
            " pause ("+pauseTime+"), "+
            " timeout ("+timeoutTime+")");
      }
      return;
    }

    MessageAddress mobileAgent = ticket.getMobileAgent();
    if (mobileAgent == null) {
      mobileAgent = agentId;
    }

    // check white pages.  Okay if agent is not listed.
    WPInfo origWPI = 
      lookupInWP(
          mobileAgent,
          30000); // max wait
    if (log.isDebugEnabled()) {
      log.debug(todd+
          "Step "+step.getUID()+
          " added, wp entry for agent "+
          mobileAgent+" is "+origWPI);
    }

    // create new Entry for this ticket
    // add step to table
    Entry entry = 
      new Entry(ticket, origWPI, step);
    idToEntry.put(id, entry);

    if ((pauseTime > 0) &&
        (status.getState() != StepStatus.RUNNING)) {

      // set step status to PAUSED
      if (status.getState() != StepStatus.PAUSED) {
        status = new StepStatus(
            StepStatus.PAUSED, -1, -1, null);
        step.setStatus(status);
        blackboard.publishChange(step);
      }

      // create wakeup alarm
      MyAlarm alarm = addAlarm(pauseTime, id);
      entry.alarm = alarm;

      if (log.isDebugEnabled()) {
        log.debug(todd+
            "Step "+step.getUID()+
            " pausing for "+
            (pauseTime - nowTime)+
            " millis ("+
            pauseTime+
            " - "+
            nowTime+
            ")");
      }
      return;
    }

    startEntry(entry);
  }

  private void startEntry(Entry entry) {
    // step already listed in table, and pause-time has passed
    Step step = entry.step;

    StepOptions options = step.getOptions();
    StepStatus status = step.getStatus();

    Ticket ticket = options.getTicket();
    Object id = ticket.getIdentifier();

    if (log.isDebugEnabled()) {
      long nowTime = System.currentTimeMillis();
      log.debug(todd+
          "Start step "+step.getUID());
    }

    // don't check for a step in progress for this agent; it's
    // something we want to test.

    if (status.getState() != StepStatus.RUNNING) {
      // create new move request
      MoveAgent ma = mobilityFactory.createMoveAgent(ticket);
      blackboard.publishAdd(ma);
      entry.moveAgent = ma;

      if (log.isDebugEnabled()) {
        log.debug(todd+
            "Created new MoveAgent object "+ma.getUID()+
            " for step "+step.getUID());
      }

      // transition step status from (UNSEEN|PAUSED) to RUNNING
      status = new StepStatus(
          StepStatus.RUNNING, 
          System.currentTimeMillis(), 
          -1,
          null);
      step.setStatus(status);
      blackboard.publishChange(step);

      if (log.isDebugEnabled()) {
        log.debug(todd+
            "Step "+step.getUID()+" is now RUNNING");
      }
    } else {
      // find existing move request
      MoveAgent ma = findMove(id);
      if (ma == null) {
        if (log.isErrorEnabled()) {
          log.error(
              "Recreated step "+step.getUID()+
              " but unable to find matching MoveAgent object"+
              " with ticket id "+id);
        }
      } else {
        entry.moveAgent = ma;
        if (log.isDebugEnabled()) {
          log.debug(
              "Recreated step "+step.getUID()+
              " found matching MoveAgent object "+
              ma.getUID());
        }
        // need to updateMove now?
      }

      if (log.isDebugEnabled()) {
        log.debug(todd+
            "Step "+step.getUID()+" resumed RUNNING");
      }
    }

    // start timer
    long timeoutTime = options.getTimeoutTime();
    if (timeoutTime > 0) {
      MyAlarm alarm = addAlarm(timeoutTime, id);
      entry.alarm = alarm;

      if (log.isDebugEnabled()) {
        long nowTime = System.currentTimeMillis();
        log.debug(todd+
            "Started step "+step.getUID()+
            " with timeout in "+
            (timeoutTime - nowTime)+
            " millis ("+timeoutTime+
            " - "+nowTime+")");
      }
    } else {
      if (log.isDebugEnabled()) {
        log.debug(todd+
            "No timeout for step "+step.getUID());
      }
    }
  }

  private void timeoutEntry(Entry entry) {
    // entry has been removed from table

    Step step = entry.step;
    StepStatus status = step.getStatus();

    // transition step status from RUNNING to TIMEOUT
    long nowTime = System.currentTimeMillis();
    status = new StepStatus(
        StepStatus.TIMEOUT, 
        status.getStartTime(), 
        nowTime,
        null);
    step.setStatus(status);
    blackboard.publishChange(step);

    // alarm should be expired
    MyAlarm alarm = entry.alarm;
    if (alarm == null) {
      if (step.getOptions().getTimeoutTime() > 0) {
        if (log.isErrorEnabled()) {
          log.error(todd+"Timeout with no alarm: "+step);
        }
      }
    } else if (!(alarm.hasExpired())) {
      if (log.isErrorEnabled()) {
        log.error(todd+"Timeout with non-expired alarm: "+alarm);
      }
      alarm.cancel();
    }

    // remove move-agent object
    MoveAgent ma = entry.moveAgent;
    if (ma != null) {
      if (ma.getStatus() != null) {
        if (log.isErrorEnabled()) {
          log.error(todd+"Timeout with move-agent status: "+ma);
        }
      }
      if (log.isInfoEnabled()) {
        log.info(todd+
            "Removing MoveAgent object "+ma.getUID()+
            " after move timeout.  If the move later completes, the"+
            " MoveAgentPlugin may complain.");
      }
      blackboard.publishRemove(ma);
    }

    if (log.isDebugEnabled()) {
      log.debug(todd+
          "Timed out on step "+step.getUID()+
          " at time "+nowTime);
    }
  }

  private void handleDueTicketIds(List ids) {
    // lookup entry in table
    for (int i = 0, n = ids.size(); i < n; i++) {
      Object id = ids.get(i);
      Entry entry = (Entry) idToEntry.get(id);
      if (entry == null) {
        // removed, or already finished
        if (log.isDebugEnabled()) {
          log.debug(todd+
              "Ignoring alarm for unknown ticket id "+id);
        }
        continue;
      }
      Step step = entry.step;
      int state = step.getStatus().getState();
      if (state == StepStatus.PAUSED) {
        // verify that pause time has passed
        long nowTime = System.currentTimeMillis();
        long pauseTime = step.getOptions().getPauseTime();
        if (nowTime < pauseTime) {
          // interrupted?
          if (log.isWarnEnabled()) {
            log.warn(todd+
                "Alarm pause resume at ("+nowTime+")"+
                " is less than pause time ("+pauseTime+"),"+
                " will proceed anyways");
          }
          // proceed anyways
        }
        // start the entry
        startEntry(entry);
      } else if (state == StepStatus.RUNNING) {
        // timeout now
        idToEntry.remove(id);
        timeoutEntry(entry);
      } else {
        // ignore, already completed
      }
    }
  }

  private void removeStep(Step step) {
    Ticket ticket = step.getOptions().getTicket();
    Object id = ticket.getIdentifier();
    // lookup entry in table
    Entry entry = (Entry) idToEntry.remove(id);
    if (entry == null) {
      // already completed, removed, or not known
      if (log.isDebugEnabled()) {
        log.debug(todd+
            "Remove step "+step.getUID()+
            " didn't find the step in the table");
      }
      return;
    }

    // step no longer in blackboard

    // alarm may be running
    MyAlarm alarm = entry.alarm;
    if ((alarm != null) &&
        (!(alarm.hasExpired()))) {
      alarm.cancel();
    }

    // remove move-agent object
    MoveAgent ma = entry.moveAgent;
    if (ma != null) {
      blackboard.publishRemove(ma);
    }

    if (log.isDebugEnabled()) {
      log.debug(todd+
          "Removed step "+step.getUID());
    }
  }

  private void updateMove(MoveAgent ma) {
    // get entry
    Ticket ticket = ma.getTicket();
    Object id = ticket.getIdentifier();
    Entry entry = (Entry) idToEntry.get(id);
    if (entry == null) {
      if (log.isErrorEnabled()) {
        log.error(todd+
            "Move updated, but step no longer exists: "+ma);
      }
      // maybe not our move!
      //blackboard.publishRemove(ma);
      return;
    }
    // check entry
    if (ma != entry.moveAgent) {
      if (log.isErrorEnabled()) {
        log.error(todd+
            "Move updated, but given move-agent object "+
            ma+
            " doesn't == the entry move-agent object "+
            entry.moveAgent);
      }
      return;
    }

    Step step = entry.step;
    StepStatus status = step.getStatus();
    int state = status.getState();
    if (state != StepStatus.RUNNING) {
      if (log.isErrorEnabled()) {
        log.error(todd+
            "Move updated, but step status is ("+
            state+") instead of RUNNING");
      }
      return;
    }
    // check status
    MoveAgent.Status mstat = ma.getStatus();
    if (mstat == null) {
      // still in progress?
      if (log.isDebugEnabled()) {
        log.debug(todd+
            "Step "+step.getUID()+
            " ignoring null move-agent "+ma.getUID()+
            " status");
      }
      return;
    }

    int newState = StepStatus.FAILURE;
    if (mstat.getCode() != MoveAgent.Status.OKAY) {
      // move itself failed
      if (log.isErrorEnabled()) {
        log.error(todd+
            "Step "+step.getUID()+
            " finished with non-OKAY status "+
            mstat.getCodeAsString());
      }
    } else {
      // check wp.  The "entry.wpi" may be null, but
      // the current wpi should not be null.
      MessageAddress mobileAgent = ticket.getMobileAgent();
      if (mobileAgent == null) {
        mobileAgent = agentId;
      }
      MessageAddress destNode = ticket.getDestinationNode();
      WPInfo origWPI = entry.wpi;

      // the wp update may take several seconds, so we
      // do a limited backoff-n-retry in case the entry looks stale.
      //
      // FIXME: ideally we would release the plugin execute thread
      // and set an alarm, but for now we'll simply sleep.
      WPInfo newWPI;
      for (int i = 0, maxi = 5; ; i++) {
        newWPI = lookupInWP(mobileAgent, 30000);
        if (newWPI == null) {
          // not listed in wp?
          if (i == 0) {
            if (log.isInfoEnabled()) {
              log.info(
                  "Will re-examine "+mobileAgent+
                  "'s null wp entry (step: "+
                  step.getUID()+")");
            }
          } else if (i >= maxi) {
            if (log.isErrorEnabled()) {
              log.error(
                  "Step "+step.getUID()+
                  " failed due to null wp"+
                  " entry for agent "+mobileAgent);
            }
            break;
          }
        } else if (
            (destNode != null) &&
            (!(destNode.getAddress().equals(newWPI.node)))) {
          // at the wrong node!
          if (i == 0) {
            if (log.isInfoEnabled()) {
              log.info(
                  "Will re-examine "+mobileAgent+
                  "'s wp entry, which indicates that"+
                  " the agent is at node "+newWPI.node+
                  " instead of the move destination node "+
                  destNode+" (step: "+step.getUID()+")");
            }
          } else if (i >= maxi) {
            if (log.isErrorEnabled()) {
              log.error(
                  "Step "+step.getUID()+
                  " failed due to wp listing of agent "+
                  mobileAgent+
                  " at node "+
                  newWPI.node+
                  " instead of ticket's destination node "+
                  destNode+
                  ", wp entry is "+newWPI);
            }
            newWPI = null;
            break;
          }
        } else {
          // seems okay
          break;
        }

        // FIXME release thread & use alarm!!!
        try {
          Thread.sleep(i*2000);
        } catch (Exception e) {
          log.error("Cancel sleep", e);
        }
      }

      if (newWPI == null) {
        // already logged our error above
      } else if (
          (destNode == null) &&
          (!(origWPI.node.equals(newWPI.node)))) {
        // didn't restart in place!
        if (log.isErrorEnabled()) {
          log.error(
              "Step "+step.getUID()+
              " finished restart-in-place of agent "+
              mobileAgent+
              " started at node "+
              origWPI.node+
              " and ended up at a different node "+
              newWPI.node+
              ", wp entry is "+newWPI);
        }
      } else if (newWPI.inc != origWPI.inc) {
        // incarnation number is wrong!  maybe crashed during move.
        if (log.isErrorEnabled()) {
          log.error(
              "Step "+step.getUID()+
              " finished with incarnation number "+
              newWPI.inc+
              " that doesn't match the prior incarnation number "+
              origWPI.inc);
        }
      } else {
        // success!
        newState = StepStatus.SUCCESS;
      }
    }

    // update step
    status = new StepStatus(
        newState, 
        status.getStartTime(),
        System.currentTimeMillis(),
        mstat);
    step.setStatus(status);
    blackboard.publishChange(step);

    if (log.isDebugEnabled()) {
      log.debug(todd+
          "Step "+step.getUID()+
          " is now in status "+
          status.getStateAsString());
    }

    // cancel alarm
    MyAlarm alarm = entry.alarm;
    if (alarm != null) {
      entry.alarm = null;
      if (!(alarm.hasExpired())) {
        alarm.cancel();
        if (log.isDebugEnabled()) {
          log.debug(todd+
              "Cancelled alarm for step "+step.getUID());
        }
      }
    }

    // keep entry for later removal

    if (log.isDebugEnabled()) {
      log.debug(todd+
          "Step "+step.getUID()+
          " completed move with status "+mstat);
    }
  }

  private MyAlarm addAlarm(long time, Object id) {
    MyAlarm alarm = new MyAlarm(time, id);
    getAlarmService().addRealTimeAlarm(alarm);
    pendingAlarms.add(alarm);
    return alarm;
  }

  private void cancelAlarms() {
    // FIXME optimize
    for (int i = 0, n = pendingAlarms.size(); i < n; i++) {
      MyAlarm ai = (MyAlarm) pendingAlarms.get(i);
      if (!(ai.hasExpired())) {
        ai.cancel();
      }
    }
    pendingAlarms.clear();
  }

  private List getDueTicketIds() {
    // FIXME optimize
    List l = null;
    for (int i = 0, n = pendingAlarms.size(); i < n; i++) {
      MyAlarm ai = (MyAlarm) pendingAlarms.get(i);
      if (ai.hasExpired()) {
        if (l == null) {
          l = new ArrayList(Math.min((n-i), 5));
        }
        l.add(ai.getId());
        pendingAlarms.remove(i);
        --i;
        --n;
      }
    }
    return l;
  }

  private WPInfo lookupInWP(
      MessageAddress agent,
      long timeout) {
    try {
      // get "node://host/node"
      AddressEntry nodeAE = wps.get(
          agent.getAddress(),
          TOPOLOGY,
          "node",
          timeout);
      if (nodeAE == null) {
        if (log.isInfoEnabled()) {
          log.info("Missing \"node://\" WP entry for "+agent);
        }
        return null;
      }
      URI nodeURI = nodeAE.getAddress();
      String host = nodeURI.getHost();
      String node = nodeURI.getPath().substring(1);
      // get "version:///incarnation/moveId"
      AddressEntry versionAE = wps.get(
          agent.getAddress(),
          TOPOLOGY,
          "version",
          timeout);
      if (versionAE == null) {
        if (log.isInfoEnabled()) {
          log.info("Missing \"version://\" WP entry for "+agent);
        }
        return null;
      }
      URI versionURI = versionAE.getAddress();
      String tmp = versionURI.getPath();
      int sep = tmp.indexOf('/', 1);
      long inc = Long.parseLong(tmp.substring(1, sep));
      long moveId = Long.parseLong(tmp.substring(sep+1));
      return new WPInfo(
          agent.getAddress(),
          node,
          host,
          inc,
          moveId);
    } catch (Exception e) {
      if (log.isInfoEnabled()) {
        log.info(
            "Failed WP lookup("+agent+")", e);
      }
      return null;
    }
  }

  // find MoveAgent with matching ticket-id
  private MoveAgent findMove(Object id) {
    Collection c = moveSub.getCollection();
    int n = c.size();
    if (n > 0) {
      Iterator iter = c.iterator();
      for (int i = 0; i < n; i++) {
        Object o = iter.next();
        if (o instanceof MoveAgent) {
          MoveAgent ma = (MoveAgent) o;
          MessageAddress a = ma.getSource();
          if (agentId.equals(a)) {
            Ticket ticket = ma.getTicket();
            Object tid = ticket.getIdentifier();
            if (id.equals(tid)) {
              return ma;
            }
          }
        }
      }
    }
    return null;
  }

  private static UnaryPredicate createStepPredicate(
      final MessageAddress agentId) {
    return 
      new UnaryPredicate() {
        public boolean execute(Object o) {
          if (o instanceof Step) {
            Step step = (Step) o;
            MessageAddress target = step.getOptions().getTarget();
            return agentId.equals(target);
          }
          return false;
        }
      };
  }

  private static UnaryPredicate createMovePredicate(
      final MessageAddress agentId) {
    return 
      new UnaryPredicate() {
        public boolean execute(Object o) {
          if (o instanceof MoveAgent) {
            MoveAgent ma = (MoveAgent) o;
            MessageAddress a = ma.getSource();
            return agentId.equals(a);
          }
          return false;
        }
      };
  }

  private static UniqueObject query(
      CollectionSubscription sub,
      UID uid) {
    Collection real = sub.getCollection();
    int n = real.size();
    if (n > 0) {
      for (Iterator iter = real.iterator(); iter.hasNext(); ) {
        Object o = iter.next();
        if (o instanceof UniqueObject) {
          UniqueObject uo = (UniqueObject) o;
          UID x = uo.getUID();
          if (uid.equals(x)) {
            return uo;
          }
        }
      }
    }
    return null;
  }

  private static class WPInfo {
    public final String agent;
    public final String node;
    public final String host;
    public final long inc;
    public final long moveId;
    public WPInfo(
        String agent, String node, String host,
        long inc, long moveId) {
      this.agent = agent;
      this.node = node;
      this.host = host;
      this.inc = inc;
      this.moveId = moveId;
    }
    public String toString() { 
      return 
        "(agent="+agent+
        ", node="+node+
        ", host="+host+
        ", inc="+inc+
        ", moveId="+moveId+
        ")";
    }
  }

  private class MyAlarm implements Alarm, Comparable {
    private final long expirationTime;
    private boolean expired = false;

    private final Object id;

    public MyAlarm(
        long expirationTime, Object id) {
      this.expirationTime = expirationTime;
      this.id = id;
    }

    public Object getId() { return id; }
    public long getExpirationTime() {return expirationTime;}
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        if (log.isDebugEnabled()) {
          log.debug(todd+
              "Alarm for ticket "+id+" expired");
        }
        if (blackboard != null) {
          blackboard.signalClientActivity();
        } else {
          // bug 989?
        }
      }
    }
    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired=true;
      return was;
    }

    public boolean equals(Object o) {
      if (o instanceof MyAlarm) {
        long ot = ((MyAlarm) o).expirationTime;
        return (expirationTime == ot);
      }
      return false;
    }
    public int compareTo(Object o) {
      long ot = ((MyAlarm) o).expirationTime;
      return 
        (expirationTime < ot) ? -1 :
        (expirationTime > ot) ? 1 :
        0;
    }

    public String toString() {
      return 
        "Alarm {"+
        "\n  expirationTime: "+expirationTime+
        "\n  expired: "+expired+
        "\n  id: "+id+
        "\n}";
    }
  }

  private static class Entry {
    public final Ticket ticket;
    public final WPInfo wpi;
    public final Step step;
    public MyAlarm alarm;
    public MoveAgent moveAgent;

    public Entry(
        Ticket ticket, 
        WPInfo wpi,
        Step step) {
      this.ticket = ticket;
      this.wpi = wpi;
      this.step = step;
      if ((ticket == null) ||
          (step == null)) {
        throw new IllegalArgumentException(
            "null ticket/step");
      }
    }

    public String toString() {
      return 
        "Entry {"+
        "\n ticket:    "+ticket+
        "\n wp info:   "+wpi+
        "\n step:      "+step+
        "\n alarm:     "+alarm+
        "\n moveAgent: "+moveAgent+
        "\n}";
    }
  }

}

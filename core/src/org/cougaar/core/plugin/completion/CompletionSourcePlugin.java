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

package org.cougaar.core.plugin.completion;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.DemoControlService;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.service.UIDService;

/**
 * This plugin gathers and integrates completion information from
 * agents in a society to determin the "completion" of the current
 * tasks. In most agents, it gathers the information and forwards the
 * completion status of the agent to another agent. This process
 * continues through a hierarchy of such plugins until the plugin at
 * the root of the tree is reached. When the root determines that
 * completion has been acheived (or is never going to be achieved), it
 * advances the clock with the expectation that the advancement will
 * engender additional activity and waits for the completion of that
 * work.
 **/

public abstract class CompletionSourcePlugin extends CompletionPlugin {
  private static final double TASK_COMPLETION_THRESHOLD = 0.99;
  private static final double CPU_CONSUMPTION_THRESHOLD = 0.95;
  private static final long UPDATE_INTERVAL = 5000L;
  private static final long LONG_CHECK_TARGETS_INTERVAL = 120000L;
  private static final long SHORT_CHECK_TARGETS_INTERVAL = 15000L;
  private static final int SHORT_CHECK_TARGETS_MAX = 5;
  private static final Class[] requiredServices = {
    UIDService.class,
    TopologyReaderService.class,
    DemoControlService.class,
    AlarmService.class,
  };
  protected TopologyReaderService topologyReaderService;
  protected UIDService uidService;
  protected DemoControlService demoControlService;
  protected AlarmService alarmService;
  protected long now = System.currentTimeMillis();
  // The following are all times when we need to awaken
  private long nextCheckTargetsTime = 0L;       // Time to check the list of targets
  private long nextUpdateTime = now;		// Time to check for new laggards
  private int shortCheckTargetsCount = 0;
  private CompletionRelay relay;                // The relay we sent

  public CompletionSourcePlugin() {
    super(requiredServices);
  }

  public void suspend() {
    if (haveServices()) {
      ServiceBroker sb = getServiceBroker();
      sb.releaseService(this, UIDService.class, uidService);
      sb.releaseService(this, TopologyReaderService.class, topologyReaderService);
      sb.releaseService(this, DemoControlService.class, demoControlService);
      sb.releaseService(this, AlarmService.class, alarmService);
      uidService = null;
    }
    super.suspend();
  }

  private boolean haveServices() {
    if (uidService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      uidService = (UIDService)
        sb.getService(this, UIDService.class, null);
      topologyReaderService = (TopologyReaderService)
        sb.getService(this, TopologyReaderService.class, null);
      demoControlService = (DemoControlService)
        sb.getService(this, DemoControlService.class, null);
      alarmService = (AlarmService)
        sb.getService(this, AlarmService.class, null);
      return true;
    }
    return false;
  }

  public void setupSubscriptions() {
    if (haveServices()) {
      checkTargets();
      startTimer(SHORT_CHECK_TARGETS_INTERVAL);
    }
  }

  public void execute() {
    if (haveServices()) {
      if (timerExpired()) {
        cancelTimer();
        boolean needCheckLaggards = true;
        now = System.currentTimeMillis();
        if (now > nextCheckTargetsTime) {
          if (checkTargets()) {
            shortCheckTargetsCount = 0; // Reset and start over
            nextCheckTargetsTime = now + SHORT_CHECK_TARGETS_INTERVAL;
            needCheckLaggards = false;
          } else if (shortCheckTargetsCount < SHORT_CHECK_TARGETS_MAX) {
            shortCheckTargetsCount++;
            nextCheckTargetsTime = now + SHORT_CHECK_TARGETS_INTERVAL;
          } else {              // Keep using the short interval
            nextCheckTargetsTime = now + LONG_CHECK_TARGETS_INTERVAL;
          }
        }
        if (needCheckLaggards) {
          checkLaggards();
        }
        startTimer(UPDATE_INTERVAL);
      }
    }
  }

  /**
   * Check if a new relay needs to be published due to a change in
   * targets. We check the topology service for the current set of
   * registered agents and compare to the set of agents that are
   * targes of the current relay. If a difference is detected, the old
   * relay is removed and a new one with the new agent set is
   * published.
   * @return true if a new relay was published (suppresses laggard checking)
   **/
  private boolean checkTargets() {
    ClusterIdentifier me = getClusterIdentifier();
    Set names = getTargetNames();
    Set targets = new HashSet(names.size());
    for (Iterator i = names.iterator(); i.hasNext(); ) {
      ClusterIdentifier cid = new ClusterIdentifier((String) i.next());
      if (!cid.equals(me)) targets.add(cid);
    }
    if (relay != null && !targets.equals(relay.getTargets())) {
      blackboard.publishRemove(relay);
      relay = null;
    }
    if (relay == null) {
      relay = new CompletionRelay(null, targets, TASK_COMPLETION_THRESHOLD, CPU_CONSUMPTION_THRESHOLD);
      relay.setUID(uidService.nextUID());
      if (logger.isInfoEnabled()) logger.info("New relay for " + targets);
      blackboard.publishAdd(relay);
      return true;
    } else {
      if (logger.isDebugEnabled()) logger.debug("Same relay for " + targets);
    }
    return false;
  }

  private void checkLaggards() {
    SortedSet laggards = relay.getLaggards();
    if (laggards.size() > 0) {
      Laggard newLaggard = (Laggard) laggards.first();
      if (logger.isDebugEnabled()) {
        logger.debug(getClusterIdentifier() + ": checkLaggards " + newLaggard);
      }
      handleNewLaggard(newLaggard);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Waiting for relay responses");
      }
    }
  }

  protected abstract Set getTargetNames();

  protected abstract void handleNewLaggard(Laggard worstLaggard);
}
      

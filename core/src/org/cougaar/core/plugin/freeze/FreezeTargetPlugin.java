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

package org.cougaar.core.plugin.freeze;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.persist.PersistenceNotEnabledException;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.plugin.PluginAdapter;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.ThreadListener;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.util.Collectors;
import org.cougaar.util.EmptyIterator;
import org.cougaar.util.Thunk;
import org.cougaar.util.UnaryPredicate;

public class FreezeTargetPlugin extends FreezePlugin implements ThreadListener {
  private static class BadGuy {
    private Thread thread;
    private Schedulable schedulable;
    int hc;
    public BadGuy(Schedulable s, Thread t) {
      thread = t;
      schedulable = s;
      hc = System.identityHashCode(t) + System.identityHashCode(s);
    }
    public int hashCode() {
      return hc;
    }
    public boolean equals(Object o) {
      if (o == this) return true;
      if (o instanceof BadGuy) {
        BadGuy that = (BadGuy) o;
        return this.thread == that.thread && this.schedulable == that.schedulable;
      }
      return false;
    }
    public String toString() {
      return schedulable.getState() + ": " + schedulable.getConsumer().toString();
    }
  }
  private IncrementalSubscription relaySubscription;
  // True if we have frozen this agent.
  private boolean isFrozen = false;
  private boolean isFreezing = false;
  private ThreadListenerService threadListenerService;
  private ThreadControlService threadControlService;
  private Set goodClasses = new HashSet();
  private Set badGuys = new HashSet(); // Records the bad guys we have
                                       // seen enter the run state
                                       // that have not left the run
                                       // state

  public void unload() {
    if (threadControlService != null) {
      ServiceBroker sb = getServiceBroker();
      sb.releaseService(this, ThreadListenerService.class, threadListenerService);
      sb.releaseService(this, ThreadControlService.class, threadControlService);
    }
    super.unload();
  }

  private UnaryPredicate myThreadQualifier =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        Schedulable schedulable = (Schedulable) o;
        Object consumer = schedulable.getConsumer();
        return isGoodGuy(consumer);
      }
    };

  // Thread control logic. Threads are classified as good or bad. When
  // frozen, we regulate the max running thread count to be no more
  // than the number of goodguys that are on the runnable queue. The
  // number of goodguys is the total of the known good guys (in the
  // goodGuys set) and the anonymous ones. We have to assume that any
  // thread we have never seen is a good guy. If an anonymous good guy
  // steps off the stage we will recognize him and reduce the
  // anonymousGoodGuys count.
  public synchronized void threadQueued(Schedulable schedulable, Object consumer) {}
  public synchronized void threadDequeued(Schedulable schedulable, Object consumer) {}
  public synchronized void threadStarted(Schedulable schedulable, Object consumer) {
//     if (logger.isDebugEnabled()) logger.debug("threadStarted: " + consumer);
    if (!isGoodGuy(consumer)) {
      badGuys.add(new BadGuy(schedulable, Thread.currentThread()));
    }
  }
  public synchronized void threadStopped(Schedulable schedulable, Object consumer) {
//     if (logger.isDebugEnabled()) logger.debug("threadStopped: " + consumer);
    if (!isGoodGuy(consumer)) {
      Thread currentThread = Thread.currentThread();
      badGuys.remove(new BadGuy(schedulable, currentThread));
    }
  }
  public void rightGiven(String consumer) {}
  public void rightReturned(String consumer) {}

  private void setThreadLimit() {
    threadControlService.setQualifier(myThreadQualifier);
  }

  private void unsetThreadLimit() {
    threadControlService.setQualifier(null);
  }

  private boolean isGoodGuy(Object consumer) {
    if (goodClasses.contains(consumer.getClass())) return true;
    if (consumer instanceof ComponentPlugin) return false;
    if (consumer instanceof PluginAdapter) return false;
    return true;
  }

  public void setupSubscriptions() {
    super.setupSubscriptions();
    Collection params = getParameters();
    for (Iterator i = params.iterator(); i.hasNext(); ) {
      String className = (String) i.next();
      try {
        goodClasses.add(Class.forName(className));
      } catch (Exception e) {
        logger.error("Bad parameter: " + className, e);
      }
    }
    goodClasses.add(FreezeTargetPlugin.class);
    goodClasses.add(FreezeNodePlugin.class);
    goodClasses.add(FreezeSocietyPlugin.class);
    ServiceBroker sb = getServiceBroker();
    threadControlService = (ThreadControlService)
      sb.getService(this, ThreadControlService.class, null);
    threadListenerService = (ThreadListenerService)
      sb.getService(this, ThreadListenerService.class, null);
    threadListenerService.addListener(this);
    relaySubscription = (IncrementalSubscription)
      blackboard.subscribe(targetRelayPredicate);
  }

  public void execute() {
    if (timerExpired()) {
      cancelTimer();
      if (isFreezing) checkStopped();
    }
    if (relaySubscription.hasChanged()) {
      if (relaySubscription.isEmpty()) {
        if (logger.isDebugEnabled()) {
          logger.debug(relaySubscription.getRemovedCollection().size() + " removes");
        }
        if (isFrozen) {
          unsetThreadLimit();       // Unset thread limit
          isFrozen = false;
        }
      } else {
        if (!isFrozen) {
          if (logger.isDebugEnabled()) logger.debug("freeze");
          setThreadLimit();
          isFrozen = true;
          isFreezing = true;
          checkStopped();
        }
      }
    }
  }

  private void checkStopped() {
    int stillRunning = badGuys.size();
    Set unfrozenAgents;
    if (stillRunning <= 0) {
      if (logger.isDebugEnabled()) {
        logger.debug("Frozen");
        isFreezing = false;
      }
      unfrozenAgents = Collections.EMPTY_SET;
    } else {
      if (logger.isDebugEnabled()) {
        Set consumerSet = new HashSet();
        for (Iterator i = badGuys.iterator(); i.hasNext(); ) {
          BadGuy bg = (BadGuy) i.next();
          consumerSet.add(bg.toString());
        }
        logger.debug("Still running: " + consumerSet);
      }
      unfrozenAgents = Collections.singleton(getAgentIdentifier());
      startTimer(5000);
    }
    for (Iterator i = relaySubscription.iterator(); i.hasNext(); ) {
      FreezeRelayTarget relay = (FreezeRelayTarget) i.next();
      relay.setUnfrozenAgents(unfrozenAgents);
      blackboard.publishChange(relay);
    }
  }
}    

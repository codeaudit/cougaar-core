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
 */

package org.cougaar.core.plugin;

import org.cougaar.core.domain.*;

import java.util.Enumeration;
import org.cougaar.core.blackboard.SubscriptionWatcher;
import org.cougaar.util.StateModelException;
import org.cougaar.core.plugin.util.PlugInHelper;
import org.cougaar.core.agent.service.alarm.Alarm;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.core.domain.RootFactory;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Expansion;


/** SimplePlugIn is a replacement for SimplifiedPlugIn and
 * SimplifiedFatPlugin.
 * Call setThreadingChoice(SINGLE_THREAD) in constructor or before this
 * load is invoked to get the equivalent of SimplifiedFatPlugin.
 **/

public abstract class SimplePlugIn extends PlugInAdapter {

  private long minDelay = 0L;
  private long maxDelay = 0L;
  private Alarm minTimer = null;
  private Alarm maxTimer = null;

  public final void load(Object object) throws StateModelException {
    if (getThreadingChoice() == UNSPECIFIED_THREAD)
      setThreadingChoice(SHARED_THREAD);

    super.load(object);
  }

  public final void initialize() throws StateModelException {
    super.initialize();
  }
  public final void start() throws StateModelException {
    super.start();
  }
  public final void suspend() throws StateModelException {
    super.suspend();
  }
  public final void resume() throws StateModelException {
    super.resume();
  }
  public final void stop() throws StateModelException {
    super.stop();
  }

  protected void setExecutionDelay(long minDelay, long maxDelay) {
    this.minDelay = minDelay;
    this.maxDelay = Math.max(minDelay, maxDelay);
  }

  /** call initialize within an open transaction. **/
  protected final void prerun() {
    try {
      openTransaction();
      setupSubscriptions();
    } catch (Exception e) {
      synchronized (System.err) {
        System.err.println(getClusterIdentifier().toString()+"/"+this+" caught "+e);
        e.printStackTrace();
      }
    } finally {
      closeTransaction(false);
    }
  }

  /** Called during initialization to set up subscriptions.
   * More precisely, called in the plugin's Thread of execution
   * inside of a transaction before execute will ever be called.
   **/
  protected abstract void setupSubscriptions();

  /** Call execute in the right context.
   * Note that this transaction boundary does NOT reset
   * any subscription changes.
   * @See execute() documentation for details
   **/
  protected final void cycle() {
    boolean doExecute = false; // Synonymous with resetTransaction
    try {
      openTransaction();
      if (wasAwakened() || (getBlackboardService().haveCollectionsChanged())) {
        if (minTimer != null) {
          if (minTimer.hasExpired() || maxTimer.hasExpired()) {
            minTimer.cancel();
            maxTimer.cancel();
            minTimer = null;
            maxTimer = null;
            doExecute = true;
          } else {
            minTimer.cancel();
            minTimer = wakeAfterRealTime(minDelay);
          }
        } else if (minDelay > 0) {
          minTimer = wakeAfterRealTime(minDelay);
          maxTimer = wakeAfterRealTime(maxDelay);
        } else {
          doExecute = true;
        }
      }
      if (doExecute) {
        execute();
      }
    } catch (Exception e) {
      synchronized (System.err) {
        System.err.println(getClusterIdentifier().toString()+"/"+this+" caught "+e);
        e.printStackTrace();
      }
      doExecute = true;
    } finally {
      closeTransaction(doExecute);
    }
  }


  /**
   * Called inside of an open transaction whenever the plugin was
   * explicitly told to run or when there are changes to any of
   * our subscriptions.
   **/
  protected abstract void execute();

  //
  // Utility methods go here
  //

  /**
   * Returns an AllocationResult with specified <confidenceRating> and <success>
   * based on the preferences of Task <t>. If <t> has no preferences, returns
   * a null AllocationResult.
   */
  public AllocationResult createEstimatedAllocationResult(Task t, double confidenceRating, boolean success) {
    RootFactory rootFactory = getFactory();
    return PlugInHelper.createEstimatedAllocationResult(t, rootFactory, confidenceRating, success);
  }

  /**
   * Creates a subtask from <parent> with same Verb, PrepositionalPhrases,
   * DirectObject, Plan, Preferences, and Context as <parent>. Creates
   * an expansion containing the subtask with null estimated allocation
   * result. Publishes both subtask and expansion.
   */
  public void createPublishExpansion(Task parent) {
    RootFactory rootFactory = getFactory();
    NewTask subtask = PlugInHelper.makeSubtask(parent, rootFactory);
    //at this point, could change some of the prepositional phrases or other
    //properties of the subtask
    //could also make a vector of subtasks instead of just one
    Expansion expansion = PlugInHelper.wireExpansion(parent, subtask, rootFactory);
    //alternatively, use a different wireExpansion method to pass in an estimated
    //allocation result or to use vectors of subtasks
    // publish the Expansion and the workflow's subtasks.
    PlugInHelper.publishAddExpansion(getBlackboardService(), expansion);
  }

}

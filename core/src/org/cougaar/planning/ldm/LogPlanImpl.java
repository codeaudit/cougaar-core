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

package org.cougaar.planning.ldm;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.domain.*;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.util.UnaryPredicate;

/**
 * Implementation of "planning" LogPlan.
 */
public class LogPlanImpl
implements LogPlan, XPlan
{
  private Blackboard blackboard;

  static final UnaryPredicate planElementP = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof PlanElement);
    }
  };

  /** is this a task object? **/
  private static final UnaryPredicate taskP = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Task);
    }
  };

  /** is this an asset? **/
  private static final UnaryPredicate assetP = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof Asset);
    }
  };

  /**
   * Private container for PlanElements only.  Supports fast lookup of
   * Task->PlanElement.
   **/
  PlanElementSet planElementSet = new PlanElementSet();
  private CollectionSubscription planElementCollection;

  UniqueObjectSet taskSet = new UniqueObjectSet();
  private CollectionSubscription taskCollection;

  AssetSet assetSet = new AssetSet();
  private CollectionSubscription assetCollection;

  public void setupSubscriptions(Blackboard blackboard) {
    this.blackboard = blackboard;
    planElementCollection = new CollectionSubscription(planElementP, planElementSet);
    blackboard.subscribe(planElementCollection);

    taskCollection = new CollectionSubscription(taskP, taskSet);
    blackboard.subscribe(taskCollection);

    assetCollection = new CollectionSubscription(assetP, assetSet);
    blackboard.subscribe(assetCollection);
  }

  public PlanElement findPlanElement(Task task) {
    return planElementSet.findPlanElement(task);
  }

  /** @deprecated Use findPlanElement(UID uid) instead. **/
  public PlanElement findPlanElement(String id) {
    return planElementSet.findPlanElement(UID.toUID(id));
  }

  public PlanElement findPlanElement(UID uid) {
    return planElementSet.findPlanElement(uid);
  }

  public Task findTask(Task task) {
    return (Task) taskSet.findUniqueObject(task.getUID());
  }

  /** @deprecated Use findTask(UID uid) instead. **/
  public Task findTask(String id) {
    return findTask(UID.toUID(id));
  }

  public Task findTask(UID uid) {
    return (Task) taskSet.findUniqueObject(uid);
  }

  public Asset findAsset(Asset asset) {
    return assetSet.findAsset(asset);
  }

  public Asset findAsset(String id) {
    return assetSet.findAsset(id);
  }

  /** Counters for different types of logplan objects for metrics **/
  private int planelemCnt = 0;
  private int workflowCnt = 0;
  private int taskCnt = 0;
  private int assetCnt = 0;

  // Accessors for metrics counts
  public int getLogPlanCount() {
    return assetCnt + taskCnt + workflowCnt + planelemCnt;
  }

  public int getAssetCount() {
    return assetSet.size();
  }

  public int getTaskCount() {
    return taskSet.size();
  }

  public int getPlanElementCount() {
    return planElementSet.size();
  }

  private static UnaryPredicate workflowPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof Workflow;
    }
  };

  public int getWorkflowCount() {
    // no subscription for workflows?
    return blackboard.countBlackboard(workflowPredicate);
  }

  // Increment counts by given amount
  public void incAssetCount(int inc) {
      assetCnt += inc;
  }

  public void incTaskCount(int inc) {
      taskCnt += inc;
  }

  public void incPlanElementCount(int inc) {
      planelemCnt += inc;
  }

  public void incWorkflowCount(int inc) {
      workflowCnt += inc;
  }
}

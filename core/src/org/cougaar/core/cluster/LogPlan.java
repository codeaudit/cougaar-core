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

package org.cougaar.core.cluster;
import org.cougaar.core.cluster.*;

import java.util.*;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.domain.planning.ldm.plan.Workflow;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.asset.Asset;

import org.cougaar.core.society.UniqueObject;
import org.cougaar.core.society.UID;

public class LogPlan
  implements LogPlanServesLogicProvider, XPlanServesBlackboard, SupportsDelayedLPActions
{
  private Blackboard blackboard;      // Delegate ALPPLanServesLogicProvider methods to this

  static final UnaryPredicate planElementP = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o instanceof PlanElement);
    }
  };

  /** is this a UniqueObject? **/
  private static final UnaryPredicate uniqueObjectP = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof UniqueObject) {
        UniqueObject uo = (UniqueObject) o;
        return (uo.getUID() != null);
      }
      return false;
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

  UniqueObjectSet uniqueObjectSet = new UniqueObjectSet();
  private CollectionSubscription uniqueObjectCollection;

  UniqueObjectSet taskSet = new UniqueObjectSet();
  private CollectionSubscription taskCollection;

  AssetSet assetSet = new AssetSet();
  private CollectionSubscription assetCollection;

  public void setupSubscriptions(Blackboard blackboard) {
    this.blackboard = blackboard;
    planElementCollection = new CollectionSubscription(planElementP, planElementSet);
    blackboard.subscribe(planElementCollection);

    uniqueObjectCollection = new CollectionSubscription(uniqueObjectP, uniqueObjectSet);
    blackboard.subscribe(uniqueObjectCollection);

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

  public UniqueObject findUniqueObject(UID uid) {
    return uniqueObjectSet.findUniqueObject(uid);
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

  public Enumeration searchLogPlan(UnaryPredicate predicate) {
    return searchBlackboard(predicate);
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
    return countBlackboard(workflowPredicate);
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

  // Implementation of BlackboardServesLogProvider

  /**
   * Apply predicate against the entire "Blackboard".
   * User provided predicate
   **/
  public Enumeration searchBlackboard(UnaryPredicate predicate) {
    return blackboard.searchBlackboard(predicate);
  }

  public int countBlackboard(UnaryPredicate predicate) {
    return blackboard.countBlackboard(predicate);
  }

  /** Add Object to the LogPlan Collection
   * (All subscribers will be notified)
   **/
  public void add(Object o) {
    blackboard.add(o);
  }

  /** Removed Object to the LogPlan Collection
   * (All subscribers will be notified)
   **/
  public void remove(Object o) {
    blackboard.remove(o);
  }

  /** Change Object to the LogPlan Collection
   * (All subscribers will be notified)
   **/
  public void change(Object o) {
    blackboard.change(o, null);
  }

  /** Change Object to the LogPlan Collection
   * (All subscribers will be notified)
   **/
  public void change(Object o, Collection changes) {
    blackboard.change(o, changes);
  }

  /**
   * Alias for sendDirective(Directive, null);
   **/
  public void sendDirective(Directive dir) {
    blackboard.sendDirective(dir, null);
  }

  /**
   * Reliably send a directive. Take pains to retransmit this message
   * until it is acknowledged even if clusters crash.
   **/
  public void sendDirective(Directive dir, Collection changes) {
    blackboard.sendDirective(dir, changes);
  }

  public PublishHistory getHistory() {
    return blackboard.getHistory();
  }

  //
  // DelayedLPAction support
  //
  
  private Object dlpLock = new Object();
  private HashMap dlpas = new HashMap(11);
  private HashMap dlpas1 = new HashMap(11);

  public void executeDelayedLPActions() {
    synchronized (dlpLock) {
      // loop in case we get cascades somehow (we don't seem to)
      while (dlpas.size() > 0) {
        // flip the map
        HashMap pending = dlpas;
        dlpas = dlpas1;
        dlpas1 = pending;

        // scan the pending map
        for (Iterator i = pending.values().iterator(); i.hasNext(); ) {
          DelayedLPAction dla = (DelayedLPAction) i.next();
          try {
            dla.execute(this);
          } catch (RuntimeException re) {
            System.err.println("DelayedLPAction "+dla+" threw: "+re);
            re.printStackTrace();
          }
        }

        // clear the pending queue before iterating.
        pending.clear();
      }
    }
  }
  
  public void delayLPAction(DelayedLPAction dla) {
    synchronized (dlpLock) {
      DelayedLPAction old = (DelayedLPAction) dlpas.get(dla);
      if (old != null) {
        old.merge(dla);
      } else {
        dlpas.put(dla,dla);
      }
    }
  }


}

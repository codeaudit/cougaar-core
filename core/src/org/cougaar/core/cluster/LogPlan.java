/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
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

public class LogPlan implements LogPlanServesLogicProvider, XPlanServesALPPlan
{
  private ALPPlan alpPlan;      // Delegate ALPPLanServesLogicProvider methods to this

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

  public void setupSubscriptions(ALPPlan alpPlan) {
    this.alpPlan = alpPlan;
    planElementCollection = new CollectionSubscription(planElementP, planElementSet);
    alpPlan.subscribe(planElementCollection);

    uniqueObjectCollection = new CollectionSubscription(uniqueObjectP, uniqueObjectSet);
    alpPlan.subscribe(uniqueObjectCollection);

    taskCollection = new CollectionSubscription(taskP, taskSet);
    alpPlan.subscribe(taskCollection);

    assetCollection = new CollectionSubscription(assetP, assetSet);
    alpPlan.subscribe(assetCollection);
  }

  public PlanElement findPlanElement(Task task) {
    return planElementSet.findPlanElement(task);
  }

  /** @deprecated Use findPlanElement(UID uid) instead. **/
  public PlanElement findPlanElement(String id) {
    return planElementSet.findPlanElement(new UID(id));
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
    return findTask(new UID(id));
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
    return searchALPPlan(predicate);
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
    return countALPPlan(workflowPredicate);
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

  // Implementation of ALPPlanServesLogProvider

  /**
   * Apply predicate against the entire "ALPPlan".
   * User provided predicate
   **/
  public Enumeration searchALPPlan(UnaryPredicate predicate) {
    return alpPlan.searchALPPlan(predicate);
  }

  public int countALPPlan(UnaryPredicate predicate) {
    return alpPlan.countALPPlan(predicate);
  }

  /** Add Object to the LogPlan Collection
   * (All subscribers will be notified)
   **/
  public void add(Object o) {
    alpPlan.add(o);
  }

  /** Removed Object to the LogPlan Collection
   * (All subscribers will be notified)
   **/
  public void remove(Object o) {
    alpPlan.remove(o);
  }

  /** Change Object to the LogPlan Collection
   * (All subscribers will be notified)
   **/
  public void change(Object o) {
    alpPlan.change(o, null);
  }

  /** Change Object to the LogPlan Collection
   * (All subscribers will be notified)
   **/
  public void change(Object o, Collection changes) {
    alpPlan.change(o, changes);
  }

  /**
   * Alias for sendDirective(Directive, null);
   **/
  public void sendDirective(Directive dir) {
    alpPlan.sendDirective(dir, null);
  }

  /**
   * Reliably send a directive. Take pains to retransmit this message
   * until it is acknowledged even if clusters crash.
   **/
  public void sendDirective(Directive dir, Collection changes) {
    alpPlan.sendDirective(dir, changes);
  }
}

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

import org.cougaar.util.UnaryPredicate;

import java.util.Enumeration;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.core.society.UID;
import org.cougaar.core.society.UniqueObject;

public interface LogPlanServesLogicProvider extends ALPPlanServesLogicProvider
{
  /** Apply predicate against the entire "LogPlan".
   * User provided predicate
   **/
  Enumeration searchLogPlan(UnaryPredicate predicate);

  /** find the PlanElement associated with a task in the LogPlan.
   * This is an optimization of searchLogPlan, since it needs to be done
   * far more often than the general case.
   **/
  PlanElement findPlanElement(Task task);

  /** like findPlanElement(Task) but looks up based on task's proxiable ID **/
  PlanElement findPlanElement(String task);
  PlanElement findPlanElement(UID uid);

  /** Find objects that are UniqueObjects using the UID as key **/
  UniqueObject findUniqueObject(UID uid);

  /** find the LogPlan task matching Task.  This is normally the
   * identity operation, though it may be that (via serialization and
   * task proxies) two task instances may actually refer to the same task.
   **/
  Task findTask(Task task);

  /** like findTask(Task), but looks up via proxiable id **/
  Task findTask(String id);
  Task findTask(UID uid);

  /** Find the Asset in the logplan.  This will be an identity operation
   * modulo serialization and copying.
   **/
  Asset findAsset(Asset asset);

  /** find the Asset in the logplan by its itemIdentification.
   **/
  Asset findAsset(String id);

  // Necessary for metrics count updates
  void incAssetCount(int inc);
  void incPlanElementCount(int inc);
  void incTaskCount(int inc);
  void incWorkflowCount(int inc);
}

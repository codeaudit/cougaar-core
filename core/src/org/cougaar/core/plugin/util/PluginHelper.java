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
package org.cougaar.core.plugin.util;

import org.cougaar.core.domain.RootFactory;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Context;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewComposition;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.NewMPTask;
import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Preposition;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Workflow;

import org.cougaar.planning.ldm.asset.AbstractAsset;
import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.core.service.BlackboardService;

import org.cougaar.util.Enumerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.Set;


public class PluginHelper {

    /**
     * Retuns an AllocationResult based on the preferences of Task <t>
     * and specified confidence rating <confrating> and success <success>.
     * Results are estimated to be the "best" possible based on the
     * preference scoring function. AllocationResult is null if
     * <t> has no preferences.
     */
    //force everyone to specify the confidence rating and success
    public static AllocationResult createEstimatedAllocationResult(Task t,
                                                                   RootFactory ldmFactory,
                                                                   double confrating,
                                                                   boolean success)
    {
        return new AllocationResultHelper(t, null)
            .getAllocationResult(confrating, success);
    }

    /**
     * updatePlanElement looks for differences between the reported and
     * estimated allocation results. If they are not equal (== for now)
     * then the estimated value is set to the reported
     * value. Return true if <pe> has been changed, false otherwise.
     */
    public static boolean updatePlanElement ( PlanElement pe) {
	AllocationResult repar = pe.getReportedResult();
	if (repar != null) {
	    //compare the result objects.
	    // If they are NOT equal, re-set the estimated result, return true.
	    AllocationResult estar = pe.getEstimatedResult();
	    //eventually change second comparison from == to isEqual ?
	    if (!repar.isEqual(estar)) {
	      	pe.setEstimatedResult(repar);
		return true;
	    }
	}
	return false;
    }

    /**
     * For each PlanElement of <sub> which has changed, if the
     * estimated and reported results are different, change
     * estimated to reported and publish the PlanElement change.
     */
    public static void updateAllocationResult( IncrementalSubscription sub) {
        Enumeration changedPEs = sub.getChangedList();
        while ( changedPEs.hasMoreElements() ) {
            PlanElement pe = (PlanElement) changedPEs.nextElement();
            if (checkChangeReports(sub.getChangeReports(pe), PlanElement.ReportedResultChangeReport.class)) {
                if (updatePlanElement(pe)) {
                    sub.getSubscriber().publishChange(pe);
                }
            }
        }
    }

    /**
     * Check if a List of ChangeReports has an instance of a given class
     **/
    public static boolean checkChangeReports(Set reports, Class cls) {
        if (reports == null) return false;
        for (Iterator i = reports.iterator(); i.hasNext(); ) {
            if (cls.isInstance(i.next())) return true;
        }
        return false;
    }

    //4 different wireExpansion methods
    //2 for single subtask, 2 for vector of subtasks
    //2 with null estimate allocation result, 2 with specified  estimated allocation result

    /**
     * Returns an expansion based on <parent> and <subTask>,
     * with appropriate relations set. Specifically,
     * puts the subtask in a NewWorkflow, sets the Workflow's
     * parent task to <parent> and sets the subtask Workflow.
     * Sets the subtask to be removed if the Workflow is removed.
     * If <subTask> has no context, sets it to that of <parent>
     * Uses a null estimated AllocationResult for the expansion.
     */
    public static Expansion wireExpansion(Task parent, NewTask subTask,
                                          RootFactory ldmf)
    {
	//use a null estimated allocation result
	return wireExpansion(parent, subTask, ldmf, null);
    }

    /**
     * Same as wireExpansion(Task, NewTask, RootFactory) but uses the
     * specified AllocationResult for the expansion.
     */
    public static Expansion wireExpansion(Task parent, NewTask subTask,
                                          RootFactory ldmf, AllocationResult ar)
    {

	NewWorkflow wf = ldmf.newWorkflow();

	wf.setParentTask( parent );
	subTask.setWorkflow( wf );
        subTask.setParentTask(parent);
	wf.addTask( subTask );

        // Set the Context of the subTask to be that of the parent, unless it
        // has already been set
	if (subTask.getContext() == null) {
	    subTask.setContext(parent.getContext());
	}

	//End of creating NewWorkflow. Start creating an Expansion.
	Expansion exp = ldmf.createExpansion( parent.getPlan(), parent, wf, ar );

	return exp;
    }

    /**
     * Wire a new subtask into an existing expansion
     **/
    public static void wireExpansion(Expansion exp, NewTask subTask) {
        Task parent = exp.getTask();
        NewWorkflow wf = (NewWorkflow) exp.getWorkflow();
        subTask.setParentTask(parent);
	subTask.setWorkflow(wf);
	wf.addTask(subTask);

        // Set the Context of the subTask to be that of the parent,
        // unless it has already been set
	if ( subTask.getContext() == null) {
	    subTask.setContext(parent.getContext());
	}
    }

  /**
   * Same as wireExpansion(Task, NewTask, RootFactory) except that a Vector
   * of subtasks is used. All the subtasks in the Vector are added to the
   * Workflow.
   */
    public static Expansion wireExpansion(Task parentTask, Vector subTasks,
                                          RootFactory ldmf)
    {
	return wireExpansion( parentTask, subTasks, ldmf, null);
    }

    /**
     * Same as wireExpansion(Task, Vector, RootFactory) except uses
     * the specified AllocationResult
     */
    public static Expansion wireExpansion(Task parentTask, Vector subTasks,
                                          RootFactory ldmf,
                                          AllocationResult ar)
    {
	NewWorkflow wf = ldmf.newWorkflow();

	wf.setParentTask(parentTask);

	Context context = parentTask.getContext();
	for (Enumeration esubTasks = subTasks.elements();
             esubTasks.hasMoreElements(); ) {
	    NewTask myTask = (NewTask) esubTasks.nextElement();
	    myTask.setWorkflow(wf);
	    myTask.setParentTask(parentTask);
	    wf.addTask(myTask);
	    // Set the Context of the subtask if it hasn't already been set
	    if (myTask.getContext() == null) {
		myTask.setContext(context);
	    }
	}

	return ldmf.createExpansion(parentTask.getPlan(), parentTask, wf, ar);
    }

    /**
     * Returns a NewTask based on <task>. The NewTask
     * has identical Verb, DirectObject, Plan, Preferences,
     * Context, and PrepositionalPhrases as <task>.
     */
    public static NewTask makeSubtask(Task task, RootFactory rtFactory) {

	NewTask subtask = rtFactory.newTask();

	// Create copy of parent Task
	subtask.setParentTask(task);
	subtask.setDirectObject( task.getDirectObject() );
	subtask.setPrepositionalPhrases( task.getPrepositionalPhrases() );
	subtask.setVerb( task.getVerb() );
	subtask.setPlan( task.getPlan() );
	subtask.setPreferences( task.getPreferences() );
        subtask.setContext( task.getContext());

	return subtask;
    }

    /** Publish a new Expansion and its subtasks **/
    public static void publishAddExpansion(BlackboardService sub, Expansion exp) {
        sub.publishAdd(exp);

        for (Enumeration esubTasks = exp.getWorkflow().getTasks();
             esubTasks.hasMoreElements(); ) {
            Task myTask = (Task) esubTasks.nextElement();
            sub.publishAdd(myTask);
        }
    }

    // 2 wireaggregation methods -- one for a single parent and one for a
    // Collection of parents

    /**
     * Connect a parent task to an MPTask. If the MPTask does not have
     * a Composition one is created for it. The MPTask may already
     * have other Aggregations.
     * @return the Aggregation created. The caller is responsible for publishing
     * the new Aggregation.
     **/
    public static Aggregation wireAggregation(Task parent, NewMPTask mpTask,
                                              RootFactory ldmf,
                                              AllocationResult ar)
    {
        NewComposition composition = (NewComposition) mpTask.getComposition();
        if (composition == null) {
            composition = ldmf.newComposition();
            composition.setCombinedTask(mpTask);
            mpTask.setComposition(composition);
        }
        Aggregation agg =
            ldmf.createAggregation(parent.getPlan(), parent, composition, ar);
        composition.addAggregation(agg);
        mpTask.setParentTasks(new Enumerator(composition.getParentTasks()));
        return agg;
    }

    /**
     * Connect a Collection of parent tasks to an MPTask. If the
     * MPTask does not have a Composition one is created for it. The
     * MPTask may already have other Aggregations. An estimated
     * AllocationResult is created for all Aggregations having a
     * confidence rating and success flag as specified by the
     * arguments.
     * @param parents the parents to be wired to the MPTask
     * @param mpTask the MPTask of the aggregation
     * @param ldmf the factory
     * @param confrating the confidence rating of all the created Aggregations
     * @param success the "success" flag for all the created Aggregations
     * @return a Collection of the Aggregations created. These have _not_ been
     * published. The caller is responsible for publishing them.
     **/
    public static Collection wireAggregation(Collection parents, NewMPTask mpTask,
                                             RootFactory ldmf,
                                             double confrating, boolean success)
    {
        NewComposition composition = (NewComposition) mpTask.getComposition();
        if (composition == null) {
            composition = ldmf.newComposition();
            composition.setCombinedTask(mpTask);
            mpTask.setComposition(composition);
        }
        ArrayList result = new ArrayList(parents.size());
        for (Iterator i = parents.iterator(); i.hasNext(); ) {
            Task parent = (Task) i.next();
            AllocationResult ar =
                createEstimatedAllocationResult(parent, ldmf, confrating, success);
            Aggregation agg =
                ldmf.createAggregation(parent.getPlan(), parent, composition, ar);
            composition.addAggregation(agg);
            result.add(agg);
        }
        mpTask.setParentTasks(new Enumerator(composition.getParentTasks()));
        return result;
    }

    // TASK PREFERENCE UTILS (taken from glm/.../TaskUtils
    public static long getStartTime(Task task) {
        double startTime = getPreferenceBestValue(task, AspectType.START_TIME);
        if (Double.isNaN(startTime)) {
            throw new IllegalArgumentException("Task has no START_TIME preference");
        }
        return (long) startTime;
    }

    public static long getEndTime(Task task) {
        double endTime = getPreferenceBestValue(task, AspectType.END_TIME);
        if (Double.isNaN(endTime)) {
            throw new IllegalArgumentException("Task has no END_TIME preference");
        }
        return (long) endTime;
    }

    public static AspectValue getPreferenceBest(Task task, int aspect_type) {
      if (task == null) throw new IllegalArgumentException("task cannot be null");
        Preference task_pref = task.getPreference(aspect_type);
	if (task_pref == null) {
	    return null;
	}
        if (task_pref.getScoringFunction() == null) {
	    return null;
	}
        return task_pref.getScoringFunction().getBest().getAspectValue();
    }

    public static double getPreferenceBestValue(Task task, int aspect_type) {
	AspectValue best = getPreferenceBest(task, aspect_type);
	if (best == null) return Double.NaN;
        return best.getValue();
    }

  // AllocationResult utils (taken from glm/.../TaskUtils

    public static double getStartTime(AllocationResult ar) {
	return getARAspectValue(ar, AspectType.START_TIME);
    }

    public static double getEndTime(AllocationResult ar) {
	return getARAspectValue(ar, AspectType.END_TIME);
    }

    public static double getARAspectValue(AllocationResult ar, int type) {
	if (ar == null) return Double.NaN;
        AspectValue[] avs = ar.getAspectValueResults();
	for (int ii = 0; ii < avs.length; ii++) {
	    if (avs[ii].getAspectType() == type) {
		return avs[ii].getValue();
	    }
	}
	return Double.NaN;
    }
}

/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import org.cougaar.core.cluster.Alarm;
import org.cougaar.core.cluster.PlanElementSet;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.plugin.SimplePlugIn;
import org.cougaar.domain.planning.ldm.asset.ClusterPG;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.plan.Allocation;
import org.cougaar.domain.planning.ldm.plan.AllocationforCollections;
import org.cougaar.domain.planning.ldm.plan.AspectType;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Expansion;
import org.cougaar.domain.planning.ldm.plan.Workflow;
import org.cougaar.domain.planning.ldm.plan.NewWorkflow;
import org.cougaar.domain.planning.ldm.plan.NewConstraint;
import org.cougaar.domain.planning.ldm.plan.Constraint;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.NewTask;
import org.cougaar.domain.planning.ldm.plan.MPTask;
import org.cougaar.domain.planning.ldm.plan.Verb;
import org.cougaar.core.plugin.util.PlugInHelper;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.SingleElementEnumeration;
import org.cougaar.core.society.UID;

/**
 * DeletionPlugIn provides generic deletion services to a cluster.
 * These consist of:
 *
 * Identification of deletable Allocations to non-org assets
 * Identification of tasks having deletable dispositions (PlanElements)
 * Removal of deletable subtasks from Expansions
 * Identification of Aggregations to deletable tasks
 **/

public class DeletionPlugIn extends SimplePlugIn {
    private static final int DELETION_DELAY_PARAM  = 0;
    private static final int DELETION_PERIOD_PARAM = 1;

    // Default times are to check every week and delete tasks older than 15 days
    private static final long DEFAULT_DELETION_PERIOD =  7 * 86400000L;
    private static final long DEFAULT_DELETION_DELAY  = 15 * 86400000L;

    private static final long subscriptionExpirationTime = 10L * 60L * 1000L;

    private long deletionPeriod = DEFAULT_DELETION_PERIOD;
    private long deletionDelay = DEFAULT_DELETION_DELAY;
    private Alarm alarm;
    private int wakeCount = 0;
    private long now;           // The current execution time
    private UnaryPredicate deletablePlanElementsPredicate;

    private static java.io.PrintWriter logFile = null;
    private static final boolean DEBUG = false;

    static {
        try {
            logFile =
                new java.io.PrintWriter(new java.io.FileWriter("deletion.log"));
        } catch (java.io.IOException e) {
            System.err.println(e);
        }
    }

    private void debug(String s) {
        s = getClusterIdentifier() + ": " + s;
        if (logFile != null) {
            synchronized (logFile) {
                logFile.println(s);
            }
        }
        System.out.println(s);
    }

    private class DeletablePlanElementsPredicate implements UnaryPredicate {
        public boolean execute(Object o) {
            if (o instanceof PlanElement) {
                PlanElement pe = (PlanElement) o;
                if (isTimeToDelete(pe)) {
                    if (pe instanceof Allocation) {
                        Allocation alloc = (Allocation) pe;
                        Asset asset = alloc.getAsset();
                        ClusterPG cpg = asset.getClusterPG();
                        if (cpg == null) return true; // Can't be remote w/o ClusterPG
                        ClusterIdentifier destination = cpg.getClusterIdentifier();
                        if (destination == null) return true; // Can't be remote w null destination
                        Task remoteTask = ((AllocationforCollections) alloc).getAllocationTask();
                        return remoteTask.isDeleted(); // Can delete if remote task is deleted
                    }
                    if (pe instanceof Expansion) {
                        Expansion exp = (Expansion) pe;
                        return !(exp.getWorkflow().getTasks().hasMoreElements());
                    }
                    return false;
                }
            }
            return false;
        }
    }

    public DeletionPlugIn() {
        super();
    }

    /**
     * Setup subscriptions. We maintain no standing subscriptions, but
     * we do have parameters to initialize -- the period between
     * deletion activities and the deletion time margin.
     **/
    protected void setupSubscriptions() {
        List params = getParameters();
        switch (params.size()) {
        default:
        case 2:
            deletionPeriod = parseInterval((String) params.get(DELETION_PERIOD_PARAM));
        case 1:
            deletionDelay = parseInterval((String) params.get(DELETION_DELAY_PARAM));
        case 0:
        }
        alarm = wakeAfter(deletionPeriod);
        getSubscriber().setShouldBePersisted(false); // All subscriptions are created as needed
        deletablePlanElementsPredicate = new DeletablePlanElementsPredicate();
    }

    private static final UnaryPredicate planElementPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return o instanceof PlanElement;
        }
    };

    private class PESet {
        private Alarm planElementAlarm;
        private PlanElementSet planElementSet;
        private Subscription planElementSubscription;
        private long planElementTime;
        public PlanElement findPlanElement(UID uid) {
            if (planElementSet == null) {
                planElementSet = new PlanElementSet();
                planElementSubscription = DeletionPlugIn.this.subscribe(planElementPredicate, planElementSet, false);
                setPlanElementAlarm(subscriptionExpirationTime);
            }
            planElementTime = System.currentTimeMillis() + subscriptionExpirationTime;
            return planElementSet.findPlanElement(uid);
        }

        public void checkAlarm() {
            if (planElementAlarm != null && planElementAlarm.hasExpired()) {
                long timeLeft = planElementTime - System.currentTimeMillis();
                if (timeLeft <= 10000L) {
                    DeletionPlugIn.this.unsubscribe(planElementSubscription);
                    planElementSubscription = null;
                    planElementSet = null;
                    planElementAlarm = null;
                } else {
                    setPlanElementAlarm(timeLeft);
                }
            }
        }

        private void setPlanElementAlarm(long interval) {
            planElementAlarm = DeletionPlugIn.this.wakeAfterRealTime(interval);
        }
    }

    private PESet peSet = new PESet();

    /**
     * Runs only when the alarm expires.
     *
     * The procedure is:
     * Find new allocations for tasks that deletable and mark the allocations
     * Find tasks with deletable dispositions and mark them
     * Find deletable tasks that are subtasks of an expansion and
     * remove them from the expansion and remove them from the
     * logplan.
     **/
    public void execute() {
        now = currentTimeMillis();
        if (alarm.hasExpired()) { // Time to make the donuts
            checkDeletablePlanElements();
            alarm = wakeAfter(deletionPeriod);
            if (DEBUG) {
                if (++wakeCount > 4) {
                    wakeCount = 0;
                    printAllPEs();
                }
            }
        }
        peSet.checkAlarm();
    }

    /**
     * Check all plan elements that are superficially deletable (as
     * determined by the deletable plan elements predicate). Starting
     * from each such plan element, we work backward, toward the root
     * tasks, looking for a deletable tasks. All the methods beginning
     * with "check" work back toward the roots. The methods beginning
     * with "delete" actually delete the objects.
     **/
    private void checkDeletablePlanElements() {
        Collection c = query(deletablePlanElementsPredicate);
        if (c.size() > 0) {
            if (DEBUG) debug("Found " + c.size() + " deletable PlanElements");
            for (Iterator i = c.iterator(); i.hasNext(); ) {
                checkPlanElement((PlanElement) i.next());
            }
        }
    }

    private void printAllPEs() {
        Collection c = query(new UnaryPredicate() {
            public boolean execute(Object o) {
                return o instanceof PlanElement;
            }
        });
        if (!c.isEmpty()) {
            debug("Undeletable Tasks");
            for (Iterator i = c.iterator(); i.hasNext(); ) {
                PlanElement pe = (PlanElement) i.next();
                String reason = canDelete(pe);
                if (reason == null) {
                    debug(pe.getTask().getUID() + " " + pe.getTask().getVerb() + ": Deletable");
                } else {
                    debug(pe.getTask().getUID() + " " + pe.getTask().getVerb() + ": " + reason);
                }
            }
        }
    }

    private String canDelete(PlanElement pe) {
        if (!isTimeToDelete(pe)) return "Not time to delete";
        if (pe instanceof Allocation) {
            Allocation alloc = (Allocation) pe;
            Asset asset = alloc.getAsset();
            ClusterPG cpg = asset.getClusterPG();
            if (cpg != null) {
                ClusterIdentifier destination = cpg.getClusterIdentifier();
                if (destination != null) {
                    Task remoteTask = ((AllocationforCollections) alloc).getAllocationTask();
                    if (remoteTask == null) return "Awaiting remote task creation";
                    if (!remoteTask.isDeleted()) return "Remote task not deleted";
                }
            }
        }
        if (pe instanceof Expansion) {
            Expansion exp = (Expansion) pe;
            if (exp.getWorkflow().getTasks().hasMoreElements()) {
                return "Expands to non-empty workflow";
            }
        }
        Task task = pe.getTask();
        if (task instanceof MPTask) {
            MPTask mpTask = (MPTask) task;
            for (Enumeration e = mpTask.getParentTasks(); e.hasMoreElements(); ) {
                Task parent = (Task) e.nextElement();
                PlanElement ppe = parent.getPlanElement(); // This is always an Aggregation
                String parentReason = canDelete(ppe);
                if (parentReason != null) return "Has undeletable parent: " + parentReason;
            }
        } else {
            UID ptuid = task.getParentTaskUID();
            if (ptuid != null) {
                PlanElement ppe = peSet.findPlanElement(ptuid);
                if (ppe != null) {
                    if (!(ppe instanceof Expansion)) {
                        String parentReason = canDelete(ppe);
                        if (parentReason != null) return "Has undeletable parent: " + parentReason;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check one plan element. The plan is already superficially
     * deletable, but cannot actually be deleted unless its task
     * is deletable.
     **/
    private void checkPlanElement(PlanElement pe) {
        if (isDeleteAllowed(pe)) {
            delete(pe.getTask());
        }
    }

    /**
     * A plan element is ready to delete if is time to delete the plan
     * element and if its task can be deleted without messing things
     * up. Its task can be deleted if it has no parent, is a subtask
     * of an expansion, or if its parent can be deleted.
     **/
    private boolean isDeleteAllowed(PlanElement pe) {
        if (pe == null) return true; // Hmmmmm, can this happen?
        Task task = pe.getTask();
        if (task instanceof MPTask) {
            MPTask mpTask = (MPTask) task;
            for (Enumeration e = mpTask.getParentTasks(); e.hasMoreElements(); ) {
                Task parent = (Task) e.nextElement();
                PlanElement ppe = parent.getPlanElement(); // This is always an Aggregation
                if (!isDeleteAllowed(ppe)) return false;
            }
            return true;
        } else {
            UID ptuid = task.getParentTaskUID();
            if (ptuid == null) return true; // Can always delete a root task
            PlanElement ppe = peSet.findPlanElement(ptuid);
            if (ppe == null) {  // Parent is in another cluster
                return true;    // It's ok to delete it
            }
            if (ppe instanceof Expansion) return true; // Can always delete a subtask
            return isDeleteAllowed(pe); // Otherwise, can only delete if the pe can be deleted
        }
    }

//      private void delete(PlanElement pe) {
//          delete(pe.getTask());
//      }

    private void delete(Task task) {
        if (DEBUG) debug("Deleting " + task);
        ((NewTask) task).setDeleted(true);  // Prevent LP from propagating deletion
        if (task instanceof MPTask) {
            // Delete multiple parent tasks
            MPTask mpTask =(MPTask) task;
            if (DEBUG) debug("Task is MPTask, deleting parents");
            for (Enumeration e = mpTask.getParentTasks(); e.hasMoreElements(); ) {
                Task parent = (Task) e.nextElement();
                delete(parent);    // ppe is always an Aggregation
            }
            if (DEBUG) debug("All parents deleted");
        } else {
            if (DEBUG) debug("Checking parent");
            UID ptuid = task.getParentTaskUID();
            if (ptuid == null) {
                if (DEBUG) debug("Deleting root");
                deleteRootTask(task);
            } else {
                PlanElement ppe = peSet.findPlanElement(ptuid);
                if (ppe == null) { // Parent is in another cluster
                                // Nothing further to do
                    if (DEBUG) debug("Parent " + ptuid + " not found");
                    deleteRootTask(task);
                } else {
                    if (ppe instanceof Expansion) {
                        if (DEBUG) debug("Parent is expansion, deleting subtask");
                        deleteSubtask((Expansion) ppe, task);
                    } else {
                        if (DEBUG) debug("Parent is expansion, deleting subtask");
                        if (DEBUG) debug("Parent is other, propagating");
                        delete(ppe.getTask()); // Not sure this is possible, but parallels "isDeleteAllowed"
                    }
                }
            }
        }
    }

    private void deleteRootTask(Task task) {
        publishRemove(task);
    }

    /**
     * Delete a subtask of an expansion. Find all constraints where
     * the subtask is the constraining task and replace the constraint
     * with an absolute constraint against the constraining value.
     **/
    private void deleteSubtask(Expansion exp, Task subtask) {
        NewWorkflow wf = (NewWorkflow) exp.getWorkflow();
        List constraintsToRemove = new ArrayList();
        for (Enumeration e = wf.getTaskConstraints(subtask); e.hasMoreElements(); ) {
            NewConstraint constraint = (NewConstraint) e.nextElement();
            if (constraint.getConstrainingTask() == subtask) {
                double value = constraint.computeValidConstrainedValue();
                constraint.setConstrainingTask(null);
                constraint.setAbsoluteConstrainingValue(value);
            } else if (constraint.getConstrainedTask() == subtask) {
                constraintsToRemove.add(constraint);
            }
        }
        wf.removeTask(subtask);
        for (Iterator i = constraintsToRemove.iterator(); i.hasNext(); ) {
            wf.removeConstraint((Constraint) i.next());
        }
        if (!wf.getTasks().hasMoreElements() && isTimeToDelete(exp)) {
            checkPlanElement(exp); // Ready to be deleted.
        }
        publishRemove(subtask);
    }

    private boolean isTimeToDelete(PlanElement pe) {
        long et = 0L;
        if (et == 0L) et = computeExpirationTime(pe);
//  	if (DEBUG) debug("Expiration time is " + new java.util.Date(et));
        boolean result = et == 0L || et < now - deletionDelay;
//          if (result) {
//              if (DEBUG) debug("isTimeToDelete: " + new java.util.Date(et));
//          }
        return result;
    }

    private long computeExpirationTime(PlanElement pe) {
        double et;
        et = PlugInHelper.getEndTime(pe.getEstimatedResult());
        if (!Double.isNaN(et)) return (long) et;
        et = PlugInHelper.getEndTime(pe.getTask());
        if (!Double.isNaN(et)) return (long) et;
        et = PlugInHelper.getStartTime(pe.getEstimatedResult());
        if (!Double.isNaN(et)) return (long) et;
        et = PlugInHelper.getStartTime(pe.getTask());
        if (!Double.isNaN(et)) return (long) et;
        return 0L;
    }
}

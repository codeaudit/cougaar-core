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

package org.cougaar.domain.planning.ldm;

import org.cougaar.domain.planning.ldm.policy.Policy;
import org.cougaar.domain.planning.ldm.policy.RuleParameter;
import org.cougaar.domain.planning.ldm.policy.PredicateRuleParameter;
import org.cougaar.domain.planning.ldm.policy.IntegerRuleParameter;
import org.cougaar.domain.planning.ldm.policy.LongRuleParameter;
import org.cougaar.domain.planning.ldm.policy.RuleParameterIllegalValueException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.cougaar.core.cluster.Alarm;
import org.cougaar.core.cluster.PlanElementSet;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.CollectionSubscription;
import org.cougaar.core.cluster.persist.PersistenceNotEnabledException;
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
    private static final SimpleDateFormat deletionTimeFormat;
    static {
        deletionTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        deletionTimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    /**
     * Inner class specifies the policy for when deletions should
     * occur. This consists of a periodic element plus ad hoc
     * elements. The policy may be modified to add or remove ad hoc
     * times as well as altering the periodic schedule. Don't forget
     * to publishChange the policy after making modifications.
     **/
    public static class DeletionSchedulePolicy extends Policy {
        public static final String DELETION_PERIOD_KEY = "deletionPeriod";
        public static final String DELETION_PHASE_KEY = "deletionPhase";
        public static final String DELETION_TIME_KEY_PREFIX = "deletionTime_";

        private SortedSet deletionTimes = new TreeSet();

        public synchronized void setPeriodicSchedule(long period, long phase)
            throws RuleParameterIllegalValueException
        {
            Add(new LongRuleParameter(DELETION_PERIOD_KEY, 0L, Long.MAX_VALUE,
                                      period));
            Add(new LongRuleParameter(DELETION_PHASE_KEY, 0L, Long.MAX_VALUE,
                                      phase));
        }

        public void Add(RuleParameter param) {
            String key = param.getName();
            if (key.startsWith(DELETION_TIME_KEY_PREFIX)) {
                deletionTimes.add(new Long(((LongRuleParameter) param).longValue()));
            }
            super.Add(param);
        }

        public void Remove(String key) {
            if (key.startsWith(DELETION_TIME_KEY_PREFIX)) {
                Long value = new Long(((LongRuleParameter) Lookup(key)).longValue());
                deletionTimes.remove((Long) value);
            }
            super.Remove(key);
        }

        public long getDeletionPhase() {
            return ((LongRuleParameter) Lookup(DELETION_PHASE_KEY)).longValue();
        }

        public long getDeletionPeriod() {
            return ((LongRuleParameter) Lookup(DELETION_PERIOD_KEY)).longValue();
        }

        private String getKey(long time) {
            return DELETION_TIME_KEY_PREFIX + deletionTimeFormat.format(new Date(time));
        }

        public synchronized void addDeletionTime(long time)
            throws RuleParameterIllegalValueException
        {
            String key = getKey(time);
            Add(new LongRuleParameter(key, 0L, Long.MAX_VALUE, time));
        }

        public synchronized void removeDeletionTime(long time) {
            String key = getKey(time);
            Remove(key);
        }

        synchronized long getNextDeletionTime(long now) {
            long deletionPhase = getDeletionPhase();
            long deletionPeriod = getDeletionPeriod();
            long ivn = (now - deletionPhase) / deletionPeriod;
            long nextAlarm = (ivn + 1) * deletionPeriod + deletionPhase;
            SortedSet oldTimes = deletionTimes.headSet(new Long(now));
            deletionTimes.removeAll(oldTimes);
            if (!deletionTimes.isEmpty()) {
                Long first = (Long) deletionTimes.first();
                long adHoc = first.longValue();
                if (adHoc < nextAlarm) {
                    nextAlarm = adHoc;
                    deletionTimes.remove(first);
                }
            }
            return nextAlarm;
        }
    }

    /**
       Inner class for specifying deletion policy. A deletion policy
       has one IntegerRuleParameter call "deletionDays" specifying the
       number of days after the end time of a Task before deletion is
       allowed. Qualified tasks are specified by the predicate of the
       policy. A DeletionPolicy also has a priority and policies are
       applied in descending priority order. Integer.MIN_VALUE is the
       lowest priority and Integer.MAX_VALUE is the highest. Usually,
       priority doesn't matter and priority should be zero for that
       case,
     **/
    public static class DeletionPolicy extends Policy {
        // Cached parameter values
        private UnaryPredicate thePredicate;
        private long theDeletionDelay;
        private int thePriority;
        public static final String PREDICATE_KEY = "predicate";
        public static final String DELETION_DELAY_KEY = "deletionDelay";
        public static final String PRIORITY_KEY = "priority";

        protected static final long NO_DELETION_DELAY = Long.MIN_VALUE;
        protected static final int NO_PRIORITY = Integer.MIN_VALUE;
        protected static final int MIN_PRIORITY = Integer.MIN_VALUE + 1;
        protected static final int MAX_PRIORITY = Integer.MAX_VALUE;

        public DeletionPolicy() {
        }
        public void init(UnaryPredicate aPredicate, long deletionDelay) {
            init(null, aPredicate, deletionDelay, 0);
        }
        public void init(UnaryPredicate aPredicate, long deletionDelay, int priority) {
            init(null, aPredicate, deletionDelay, priority);
        }
        public void init(String aName, UnaryPredicate aPredicate, long deletionDelay) {
            init(aName, aPredicate, deletionDelay, 0);
        }
        /**
         * Initialize this policy.
         * @param aName a name for this policy used in printouts and for debugging
         * @param aPredicate A predicate for selecting Tasks for which this
         * policy applies
         * @param deletionDelay The age the tasks must reach before
         * being deleted
         * @param priority When multiple policies apply to a task, the
         * highest priority policy wins and the rest are ignored.
         **/
        public void init(String aName, UnaryPredicate aPredicate,
                         long deletionDelay, int priority)
        {
            setName(aName);
            try {
                Add(new PredicateRuleParameter(PREDICATE_KEY, aPredicate));
                Add(new IntegerRuleParameter(PRIORITY_KEY,
                                             MIN_PRIORITY, MAX_PRIORITY, priority));
                Add(new LongRuleParameter(DELETION_DELAY_KEY,
                                          0L, Long.MAX_VALUE, deletionDelay));
            } catch (RuleParameterIllegalValueException e) {
                // No way this should happen because x <= x <= x is never false;
                e.printStackTrace();
            }
        }
        public void Add(RuleParameter param) {
            clearCache();
            super.Add(param);
        }
        public void clearCache() {
            thePredicate = null;
            theDeletionDelay = NO_DELETION_DELAY;
            thePriority = NO_PRIORITY;
        }
        public UnaryPredicate getPredicate() {
            if (thePredicate == null) {
                PredicateRuleParameter prp = (PredicateRuleParameter) Lookup(PREDICATE_KEY);
                thePredicate = prp.getPredicate();
            }
            return thePredicate;
        }
        public void setDeletionDelay(long deletionDelay) {
            try {
                theDeletionDelay = deletionDelay;
                Replace(new LongRuleParameter(DELETION_DELAY_KEY,
                                              deletionDelay, deletionDelay, deletionDelay));
            } catch (RuleParameterIllegalValueException e) {
                // No way this should happen because x <= x <= x is never false;
                e.printStackTrace();
            }
        }
        public long getDeletionDelay() {
            if (theDeletionDelay == NO_DELETION_DELAY) {
                theDeletionDelay = ((LongRuleParameter) Lookup(DELETION_DELAY_KEY)).longValue();
            }
            return theDeletionDelay;
        }
        public int getPriority() {
            if (thePriority == NO_PRIORITY) {
                thePriority = ((IntegerRuleParameter) Lookup(PRIORITY_KEY)).intValue();
            }
            return thePriority;
        }
    }

    private static UnaryPredicate truePredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return true;
        }
    };

    public static class DefaultDeletionPolicy extends DeletionPolicy {
        public DefaultDeletionPolicy() {
            init("Default Deletion Policy", truePredicate, DEFAULT_DELETION_DELAY,
                  MIN_PRIORITY);
        }
    }

    private static final int DELETION_DELAY_PARAM  = 0;
    private static final int DELETION_PERIOD_PARAM = 1;
    private static final int DELETION_PHASE_PARAM = 2;
    private static final int ARCHIVING_ENABLED_PARAM = 3;
    private static final int DEBUG_PARAM = 4;

    // Default times are to check every week and delete tasks older
    // than 15 days. The checks occur at times that are zero modulo
    // the check interval
    
    private static final long DEFAULT_DELETION_PERIOD =  7 * 86400000L;
    private static final long DEFAULT_DELETION_DELAY  =  15 * 86400000L;
    private static final long DEFAULT_DELETION_PHASE = 0L;
    private static final boolean DEFAULT_ARCHIVING_ENABLED = true;
    private static final boolean DEFAULT_DEBUG = false;

    private static final long subscriptionExpirationTime = 10L * 60L * 1000L;

    private boolean archivingEnabled = true;
    private Alarm alarm;
    private int wakeCount = 0;
    private long now;           // The current execution time
    private DeletionSchedulePolicy theDeletionSchedulePolicy;
    private UnaryPredicate deletablePlanElementsPredicate;
    private UnaryPredicate deletionPolicyPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return o instanceof DeletionPolicy;
        }
    };
    private UnaryPredicate deletionSchedulePolicyPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return o instanceof DeletionSchedulePolicy;
        }
    };
    SortedSet deletionPolicySet = new TreeSet(new Comparator() {
        public int compare(Object o1, Object o2) {
            DeletionPolicy p1 = (DeletionPolicy) o1;
            DeletionPolicy p2 = (DeletionPolicy) o2;
            int diff = p1.getPriority() - p2.getPriority();
            if (diff != 0) return diff;
            String n1 = p1.getName();
            String n2= p2.getName();
            if (n1 != n2) {
                if (n1 == null) return -1;
                if (n2 == null) return +1;
                diff = n1.compareTo(n2);
                if (diff != 0) return diff;
            }
            return o1.hashCode() - o2.hashCode();
        }
    });

    private CollectionSubscription deletionPolicies;
    private CollectionSubscription deletionSchedulePolicies;

    private static java.io.PrintWriter logFile = null;
    private static boolean DEBUG = false;

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
        long deletionDelay = DEFAULT_DELETION_DELAY;
        long deletionPeriod = DEFAULT_DELETION_PERIOD;
        long deletionPhase = DEFAULT_DELETION_PHASE;
        archivingEnabled = DEFAULT_ARCHIVING_ENABLED;
        DEBUG = DEFAULT_DEBUG;
        List params = getParameters();
        switch (params.size()) {
        default:
            DEBUG = ((String) params.get(DEBUG_PARAM)).equals("true");
        case DEBUG_PARAM:
            archivingEnabled = ((String) params.get(ARCHIVING_ENABLED_PARAM)).equals("true");
        case ARCHIVING_ENABLED_PARAM:
            deletionPhase = parseInterval((String) params.get(DELETION_PHASE_PARAM));
        case DELETION_PHASE_PARAM:
            deletionPeriod = parseInterval((String) params.get(DELETION_PERIOD_PARAM));
        case DELETION_PERIOD_PARAM:
            deletionDelay = parseInterval((String) params.get(DELETION_DELAY_PARAM));
        case DELETION_DELAY_PARAM:
        }
        deletionPolicies =
            (CollectionSubscription) subscribe(deletionPolicyPredicate, false);
        checkDeletionPolicies(deletionDelay);
        deletionSchedulePolicies =
            (CollectionSubscription) subscribe(deletionSchedulePolicyPredicate, false);
        checkDeletionSchedulePolicies(deletionPeriod, deletionPhase);
        setAlarm();
        getBlackboardService().setShouldBePersisted(false); // All subscriptions are created as needed
        deletablePlanElementsPredicate = new DeletablePlanElementsPredicate();
    }

    private static final UnaryPredicate planElementPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
            return o instanceof PlanElement;
        }
    };

    private Subscription doSubscribe(UnaryPredicate p, PlanElementSet c, boolean b) {
        return subscribe(p, c, b);
    }

    private void doUnsubscribe(Subscription s) {
        unsubscribe(s);
    }

    private class PESet {
        private Alarm planElementAlarm;
        private PlanElementSet planElementSet;
        private Subscription planElementSubscription;
        private long planElementTime;
        public PlanElement findPlanElement(UID uid) {
            if (planElementSet == null) {
                planElementSet = new PlanElementSet();
                planElementSubscription = doSubscribe(planElementPredicate, planElementSet, false);
                setPlanElementAlarm(subscriptionExpirationTime);
            }
            planElementTime = System.currentTimeMillis() + subscriptionExpirationTime;
            return planElementSet.findPlanElement(uid);
        }

        public void checkAlarm() {
            if (planElementAlarm != null && planElementAlarm.hasExpired()) {
                long timeLeft = planElementTime - System.currentTimeMillis();
                if (timeLeft <= 10000L) {
                    doUnsubscribe(planElementSubscription);
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
//              System.out.println("Time to make the donuts");
            if (archivingEnabled) {
                try {
                    getBlackboardService().persistNow(); // Record our state
                } catch (PersistenceNotEnabledException pnee) {
                    pnee.printStackTrace();
                    System.err.println("Archiving disabled");
                    archivingEnabled = false;
                }
            }
            checkDeletablePlanElements();
            setAlarm();
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
     * Set the alarm so that it expires when the time is next
     * congruent to the deletionPhase modulo the deletionPeriod
     **/
    private void setAlarm() {
        long now = currentTimeMillis();
        long nextAlarm = theDeletionSchedulePolicy.getNextDeletionTime(now);
        long delay = nextAlarm - now;
//          System.out.println("Make the donuts in " + delay + "msec.");
        alarm = wakeAfter(delay);
    }

    /**
       Check to see if the default policy is present and matches the
       current deletionDelay, If a DefaultDeletionPolicy is found for
       which the deletionDelay does not match the current
       deletionDelay, it is removed. If no DefaultDeletionPolicy
       having the correct deletionDelay is found, a new one created
       and added.
     **/
    private void checkDeletionPolicies(long deletionDelay) {
        for (Iterator i = deletionPolicies.iterator(); i.hasNext(); ) {
            DeletionPolicy policy = (DeletionPolicy) i.next();
            if (policy instanceof DefaultDeletionPolicy) {
                if (policy.getDeletionDelay() == deletionDelay) return; // ok
                publishRemove(policy);
            }
        }
        DefaultDeletionPolicy policy =
            (DefaultDeletionPolicy) theLDMF.newPolicy(DefaultDeletionPolicy.class.getName());
        policy.setDeletionDelay(deletionDelay);
        publishAdd(policy);
    }

    /**
       Check to see if the default schedule policy is present. If a
       DeletionSchedulePolicy is found then its periodic deletion
       parameters are set. If no DeletionSchedulePolicy is found, a
       new one created and added. If multiple schedule policies are
       found, all but one is deleted.
     **/
    private void checkDeletionSchedulePolicies(long deletionPeriod, long deletionPhase) {
        for (Iterator i = deletionSchedulePolicies.iterator(); i.hasNext(); ) {
            DeletionSchedulePolicy policy = (DeletionSchedulePolicy) i.next();
            if (theDeletionSchedulePolicy == null) {
                try {
                    policy.setPeriodicSchedule(deletionPeriod, deletionPhase);
                } catch (RuleParameterIllegalValueException e) {
                    e.printStackTrace();
                }
                theDeletionSchedulePolicy = policy;
            } else {
                publishRemove(policy);
            }
        }
        if (theDeletionSchedulePolicy == null) {
            DeletionSchedulePolicy policy = (DeletionSchedulePolicy)
                theLDMF.newPolicy(DeletionSchedulePolicy.class.getName());
            try {
                policy.setPeriodicSchedule(deletionPeriod, deletionPhase);
            } catch (RuleParameterIllegalValueException e) {
                e.printStackTrace();
            }
            publishAdd(policy);
            theDeletionSchedulePolicy = policy;
        }
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
        long et = computeExpirationTime(pe);
//  	if (DEBUG) debug("Expiration time is " + new java.util.Date(et));
        boolean result = et == 0L || et < now;
//          if (result) {
//              if (DEBUG) debug("isTimeToDelete: " + new java.util.Date(et));
//          }
        return result;
    }

    private long computeExpirationTime(PlanElement pe) {
        double et;
        Task task = pe.getTask();
        et = PlugInHelper.getEndTime(pe.getEstimatedResult());
        if (Double.isNaN(et))
            try {
                et = PlugInHelper.getEndTime(task);
            } catch (RuntimeException re) {
                et = Double.NaN;
            }
        if (Double.isNaN(et)) et = PlugInHelper.getStartTime(pe.getEstimatedResult());
        if (Double.isNaN(et))
            try {
                et = PlugInHelper.getStartTime(task);
            } catch (RuntimeException re) {
                et = Double.NaN;
            }
        if (Double.isNaN(et)) return 0L;
        for (Iterator i = deletionPolicies.iterator(); i.hasNext(); ) {
            DeletionPolicy policy = (DeletionPolicy) i.next();
            if (policy.getPredicate().execute(task)) {
                return ((long) et) + policy.getDeletionDelay();
            }
        }
        return 0L; // Should not get here; DefaultDeletionPolicy should always apply
    }
}

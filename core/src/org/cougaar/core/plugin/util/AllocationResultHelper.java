/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.plugin.util;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import org.cougaar.domain.planning.ldm.plan.AllocationResult;
import org.cougaar.domain.planning.ldm.plan.AspectType;
import org.cougaar.domain.planning.ldm.plan.AspectValue;
import org.cougaar.domain.planning.ldm.plan.TimeAspectValue;
import org.cougaar.domain.planning.ldm.plan.AspectRate;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Preference;
import org.cougaar.domain.planning.ldm.plan.ScoringFunction;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.measure.*;

/**
 * Manages AllocationResults having phased results representing
 * varying quantities and rates over time.
 **/
public class AllocationResultHelper {
    public class Phase {
        AspectValue[] result;
        private Phase(int ix) {
            result = (AspectValue[]) phasedResults.get(ix);
        }

        public AspectValue getAspectValue(int type) {
            return result[getTypeIndex(type)];
        }

        public long getStartTime() {
            if (startix < 0) throw new RuntimeException("No START_TIME for " + task);
            return (long) result[startix].getValue();
        }

        public long getEndTime() {
            if (endix < 0) throw new RuntimeException("No END_TIME for " + task);
            return (long) result[endix].getValue();
        }

        public double getQuantity() {
            if (qtyix < 0) throw new RuntimeException("No QUANTITY for " + task);
            return result[qtyix].getValue();
        }
    }

    /** The Task whose disposition we are computing a result. **/
    private Task task;

    /** The index into the arrays of the START_TIME aspect **/
    private int startix = -1;

    /** The index into the arrays of the END_TIME aspect **/
    private int endix = -1;

    /** The index into the arrays of the QUANTITY aspect **/
    private int qtyix = -1;

    /** The new rollupResult **/
    private AspectValue[] rollupResult;

    /** The new phased results **/
    private List phasedResults = new ArrayList();

    /** The AspectType map **/
    private int[] types;

    /** Has this allocation result been changed **/
    private boolean isChanged = false;

    public AllocationResultHelper(Task task, PlanElement pe) {
        this.task = task;
        AllocationResult ar = null;
        if (pe != null) {
            ar = pe.getEstimatedResult();
        }
        if (ar != null) {
            setAllocationResult(ar);
        } else {
            setPerfectResult();
        }
    }

    public AllocationResult getAllocationResult() {
        return getAllocationResult(1.0, getScore() <= 0.0);
    }
    public AllocationResult getAllocationResult(double confrating,
                                                boolean isSuccess)
    {
        return new AllocationResult(confrating, isSuccess,
                                    rollupResult, phasedResults);
    }

    private double getScore() {
        double totalWeight = 0.0;
        double totalScore = 0.0;
        int ix = 0;
        for (Enumeration e = task.getPreferences(); e.hasMoreElements(); ix++) {
            Preference pref = (Preference) e.nextElement();
            ScoringFunction sf = pref.getScoringFunction();
            double thisWeight = pref.getWeight();
            AspectValue av = rollupResult[ix];
            double thisScore = sf.getScore(av);
            totalScore += thisScore * thisWeight;
            totalWeight += thisWeight;
        }
        double score = totalScore / totalWeight;
        return score;
    }

    /**
     * Set from an existing AllocationResult. The information is
     * copied out into variable for convenience during subsequent
     * operations.
     **/
    private void setAllocationResult(AllocationResult ar) {
        rollupResult = ar.getAspectValueResults();
        if (ar.isPhased()) {
            phasedResults = ar.getPhasedAspectValueResults();
        } else {
            phasedResults = new ArrayList(1);
            phasedResults.add(rollupResult);
        }
        setTypeIndexes();
    }

    /**
     * Make the results perfectly conform to the task's preferences.
     * The results variables are set to perfectly match the best
     * values of all the preferences.
     **/
    public void setTask(Task task) {
        this.task = task;
        setPerfectResult();
    }

    private void setPerfectResult() {
        List avs = new ArrayList();
        for (Enumeration e = task.getPreferences(); e.hasMoreElements(); ) {
            Preference pref = (Preference) e.nextElement();
            AspectValue best = pref.getScoringFunction().getBest().getAspectValue();
            avs.add(best.clone());
        }
        rollupResult = (AspectValue[]) avs.toArray(new AspectValue[avs.size()]);
        setTypeIndexes();
        phasedResults.clear();
        phasedResults.add(rollupResult);
        isChanged = true;
    }

    public boolean isChanged() {
        return isChanged;
    }

    public int getPhaseCount() {
        return phasedResults.size();
    }

    public Phase getPhase(int i) {
        return new Phase(i);
    }

    /**
     * Set a successful value over a period of time
     **/
    public void setBest(int type, long startTime, long endTime) {
        int ix = getTypeIndex(type);
        Preference pref = task.getPreference(type);
        AspectValue av = pref.getScoringFunction().getBest().getAspectValue();
        set(ix, av, startTime, endTime);
    }

    /**
     * Set a vailed value over a period of time. New phased results
     * are edited into the results as needed.
     **/
    public void setFailed(int type, long startTime, long endTime) {
        int ix = getTypeIndex(type);
        AspectValue av = (AspectValue) rollupResult[ix].clone();
        av.setValue(0.0);
        set(ix, av, startTime, endTime);
    }

    private int getTypeIndex(int type) {
        switch (type) {
            case AspectType.QUANTITY:   return qtyix  ;
            case AspectType.START_TIME: return startix;
            case AspectType.END_TIME:   return endix  ;
        }
        for (int i = 0; i < rollupResult.length; i++) {
            if (rollupResult[i].getAspectType() == type) return i;
        }
        throw new IllegalArgumentException("Type " + type + " not found");
    }

    /**
     * Edit the exiting results to reflect a particular value of the
     * indicated aspect over a given time period. Find existing
     * segments with different values that overlap the new segment and
     * adjust their times to not overlap. Then try to combine the new
     * segment with existing results having the same value and
     * adjacent or overlapping times. Finally, if the new segment
     * cannot be combined with any existing segment, add a new
     * segment. This does not fix the rollup result since that depends
     * on what aspect is edited.
     * @param valueix the index in the arrays of the aspect to change
     * @param value the new value for the time period
     * @param startTime the time when the value starts to apply
     * @param endTime the time when the value no longer applies.
     * @return true if a change was made.
     **/
    private void set(int valueix, AspectValue av, long startTime, long endTime) {
        List newResults = new ArrayList(phasedResults.size() + 2); // At most two new results
        boolean covered = false;
        boolean thisChanged = false;
        AspectValue[] newResult;
        long minTime = getStartTime(rollupResult);
        long maxTime = getEndTime(rollupResult);
        if (minTime < maxTime) {
            /* Only process if there is overlap between the arguments and
               the start/end time aspects of the rollupResult **/
            if (startTime >= maxTime) {
                return; // Does not apply
            }
            endTime = Math.min(endTime, maxTime);
            if (endTime <= minTime) {
                return; // Does not apply
            }
            startTime = Math.max(startTime, minTime);

            for (Iterator i = phasedResults.iterator(); i.hasNext(); ) {
                AspectValue[] oneResult = (AspectValue[]) i.next();
                long thisStart = getStartTime(oneResult);
                long thisEnd   = getEndTime(oneResult);
                AspectValue thisValue = oneResult[valueix];
                if (thisValue.equals(av)) { // Maybe combine these
                    newResult = (AspectValue[]) oneResult.clone();
                    if (startTime <= thisEnd && endTime >= thisStart) { // Overlaps
                        if (thisStart < startTime) startTime = thisStart;
                        if (thisEnd > endTime) endTime = thisEnd;
                        thisChanged = true;
                        continue;
                    } else {
                        newResults.add(newResult);
                    }
                } else {
                    if (startTime < thisEnd && endTime > thisStart) { // Overlaps
                        if (startTime > thisStart) { // Initial portion exists
                            newResult = (AspectValue[]) oneResult.clone();
                            newResult[endix] = new TimeAspectValue(AspectType.END_TIME, startTime);
                            newResults.add(newResult);
                        }
                        if (endTime < thisEnd) { // Final portion exists
                            newResult = (AspectValue[]) oneResult.clone();
                            newResult[startix] = new TimeAspectValue(AspectType.START_TIME, endTime);
                            newResults.add(newResult);
                        }
                        thisChanged = true;
                    } else {
                        newResult = (AspectValue[]) oneResult.clone();
                        newResults.add(newResult);
                    }
                }
            }
        } else {
            if (startTime > minTime || endTime <= minTime) return;
            if (rollupResult[valueix].equals(av)) return;
            newResult = (AspectValue[]) rollupResult.clone();
            newResult[valueix] = av;
            newResults.add(newResult);
            thisChanged = true;
            covered = true;
        }
        if (!covered) {
            newResult = (AspectValue[]) rollupResult.clone();
            newResult[startix] = new TimeAspectValue(AspectType.START_TIME, startTime);
            newResult[endix]   = new TimeAspectValue(AspectType.END_TIME, endTime);
            newResult[valueix] = av;
            newResults.add(newResult);
            thisChanged = true;
        }
        if (!thisChanged) {
            return; // No changes were made
        }
        isChanged = true;
        phasedResults = newResults;

        double[] sums = new double[rollupResult.length];
        double[] divisor = new double[rollupResult.length];
        boolean first = true;
        Arrays.fill(sums, 0.0);
        Arrays.fill(divisor, 1.0);
        for (Iterator iter = phasedResults.iterator(); iter.hasNext(); ) {
            AspectValue[] oneResult = (AspectValue[]) iter.next();
            for (int i = 0; i < oneResult.length; i++) {
                double v = oneResult[i].getValue();
                if (first) {
                    sums[i] = v;
                } else {
                    switch (oneResult[i].getAspectType()) {
                    default:
                        if (av instanceof AspectRate) {
                            divisor[i] += 1.0;
                        }
                        sums[i] += v;
                        break;
                    case AspectType.START_TIME:
                        sums[i] = Math.min(sums[i], v);
                        break;
                    case AspectType.END_TIME:
                        sums[i] = Math.max(sums[i], v);
                        break;
                    }
                }
                first = false;
            }
        }
        for (int i = 0; i < rollupResult.length; i++) {
            rollupResult[i].setValue(sums[i] / divisor[i]);
        }
    }

    private long getStartTime(AspectValue[] avs) {
        return (long) avs[(startix >= 0) ? startix : endix].getValue();
    }

    private long getEndTime(AspectValue[] avs) {
        return (long) avs[(endix >= 0) ? endix : startix].getValue();
    }

    private void setTypeIndexes() {
        for (int i = 0; i < rollupResult.length; i++) {
            switch (rollupResult[i].getAspectType()) {
            case AspectType.QUANTITY:         qtyix   = i; break;
            case AspectType.START_TIME:       startix = i; break;
            case AspectType.END_TIME:         endix   = i; break;
            default: break;
            }
        }
    }
}

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


package org.cougaar.planning.ldm.lps;

import org.cougaar.core.blackboard.*;
import org.cougaar.core.agent.*;

import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.RoleScheduleConflicts;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.RoleSchedule;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.asset.Asset;

import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

/** RoleScheduleConflictLP checks an Asset's roleschedule for potential
  * schedule conflicts.  Each time a new allocation or assettransfer is published or
  * a change is published for an allocation or assettransfer, the allocation's/assettransfer's estimated
  * allocation result schedule will be checked against the rest of the 
  * roleschedule.    If there is a conflict, the conflicting
  * allocation's/assettransfer's potentialconflict flags will be set.
  * The schedule will also be checked for dates outside
  * of the available schedule.  If an available conflict occurs, the allocation's/assettransfer's
  * assetavailabilityconflict flag will be set.
  **/

public class RoleScheduleConflictLP extends LogPlanLogicProvider implements EnvelopeLogicProvider {
  public RoleScheduleConflictLP(LogPlanServesLogicProvider logplan,
                        ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }

  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    // We don't need to test on Envelope Contents/Action again...
    
    if (!((obj instanceof Allocation) || (obj instanceof AssetTransfer))) return;

    if (!(o.isAdd() || 
          (o.isChange() && ((RoleScheduleConflicts)obj).checkConflicts())))
      return;

    PlanElement pe = (PlanElement) obj;
    // re-set the checkconflict flag
    ((RoleScheduleConflicts)pe).setCheckConflicts(false);
    
    Asset theasset = null;
    boolean currentpc = false;
    boolean currentaac = false;
    if (pe instanceof Allocation) {
      Allocation alloc = (Allocation) pe;
      theasset = alloc.getAsset();
      currentpc = alloc.isPotentialConflict();
      currentaac = alloc.isAssetAvailabilityConflict();
    } else if ( pe instanceof AssetTransfer ) {
      AssetTransfer at = (AssetTransfer) pe;
      theasset = at.getAsset();
      currentpc = at.isPotentialConflict();
      currentaac = at.isAssetAvailabilityConflict();
    }
    RoleSchedule rs = theasset.getRoleSchedule();
    
    boolean withinAvailSched = checkAvailableSchedule(pe, rs);
    List conflicts = checkRoleSchedule(pe, rs);
    
    if ( ! conflicts.isEmpty() ) {
      for ( ListIterator lit = conflicts.listIterator(); lit.hasNext(); ) {
        PlanElement tomark = (PlanElement) lit.next();
        // make sure this pe's potentialconflict flag isn't already true before marking it
        boolean othercurrentpc = false;
        if ( tomark instanceof Allocation) {
          othercurrentpc = ((Allocation)tomark).isPotentialConflict();
        } else if (tomark instanceof AssetTransfer) {
          othercurrentpc = ((AssetTransfer)tomark).isPotentialConflict();
        }
        if ( ! othercurrentpc ) {
          ((RoleScheduleConflicts)tomark).setPotentialConflict(true);
          logplan.change(tomark, changes);
        }
      }
      // now mark ourselves if our potentialconflict flag isn't already true
      if ( ! currentpc ) {
        ((RoleScheduleConflicts)pe).setPotentialConflict(true);
        logplan.change(pe, changes);
      }
    } else {
      // If there are no conflicts, check to see if our conflict flag was
      // previously set to true.  If it was, re-set it to false.
      if ( currentpc ) {
          ((RoleScheduleConflicts)pe).setPotentialConflict(false);
          logplan.change(pe, changes);
      } 
    }
      
    // if its not within the available schedule and the asset availability conflict
    // flag isn't already true, set it!      
    if ( (! withinAvailSched) && (! currentaac) ) {
      // if there was a conflict here set our asset availability conflict flag
        ((RoleScheduleConflicts)pe).setAssetAvailabilityConflict(true);
        logplan.change(pe, changes);
    } else {
      // if there was no conflict, check to see if our conflict flag was
      // previously set to true.  If it was, re-set if to false.
      if ( currentaac ) {
        ((RoleScheduleConflicts)pe).setAssetAvailabilityConflict(false);
        logplan.change(pe, changes);
      }
    }
  }
       
    
  private boolean checkAvailableSchedule(PlanElement thepe, RoleSchedule thers) {
    // if the available schedule is null, return true
    if ( thers.getAvailableSchedule() == null ) {
      return true;
    }
    AllocationResult estar = thepe.getEstimatedResult();
    // make sure that the start time and end time aspects are defined.
    // if they aren't return true 
    //(this could happen with a propagating failed allocation result).
    if ( (estar == null) ||
	 (! (estar.isDefined(AspectType.START_TIME)) ) || 
	 (! (estar.isDefined(AspectType.END_TIME)) ) ) {
      return true;
    }
    double sdate = estar.getValue(AspectType.START_TIME);
    double edate = estar.getValue(AspectType.END_TIME);
    // get the available schedule, remember there may be more than one available time segment.
    Enumeration availsched = thers.getAvailableSchedule().getAllScheduleElements();
    while (availsched.hasMoreElements()) {
      ScheduleElement se = (ScheduleElement) availsched.nextElement();
      long availsdate = se.getStartDate().getTime();
      long availedate = se.getEndDate().getTime();
      if ( (sdate >= availsdate) && (edate <= availedate) ) {
        // if the dates fall into atleast one of the time segments return true.
        return true;
      }
    }
    // if we are here it didn't fall into any available schedule
    return false;
  }
  
  private List checkRoleSchedule(PlanElement thepe, RoleSchedule thers) {
    List conflictlist = new ArrayList();
    AllocationResult estar = thepe.getEstimatedResult();
    
    // make sure that the start time and end time aspects are defined.
    // if they aren't return the empty conflictlist
    // (this could happen with a propagating failed allocation result).
    if ((estar != null) && 
	(estar.isDefined(AspectType.START_TIME) ) && 
	(estar.isDefined(AspectType.END_TIME) ) ) {
      
      long stime = ((long)estar.getValue(AspectType.START_TIME));
      long etime = ((long)estar.getValue(AspectType.END_TIME));
    
      // check for encapsulating schedules of other plan elements.
      Collection encap = thers.getEncapsulatedRoleSchedule(stime, etime);
      for (Iterator ec = encap.iterator(); ec.hasNext(); ) {
        PlanElement conflictpe = (PlanElement) ec.next();
        // make sure its not our pe.
        if ( !(conflictpe == thepe) ) {
          conflictlist.add(conflictpe);
        }
      }
    
      // check for ovelapping schedules of other plan elements.
      Collection overlap = thers.getOverlappingRoleSchedule(stime, etime);
      for (Iterator oc = overlap.iterator(); oc.hasNext(); ) {
        PlanElement overconflictpe = (PlanElement) oc.next();
        // once again, make sure its not our pe.
        if ( !(overconflictpe == thepe) ) {
          conflictlist.add(overconflictpe);
        }
      }
    }
    
    return conflictlist;
  }
}

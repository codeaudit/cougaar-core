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

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.agent.ClusterServesLogicProvider;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.LogPlanServesLogicProvider;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogPlanLogicProvider;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ClusterPG;

import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
import org.cougaar.planning.ldm.plan.AssetRescind;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.RoleSchedule;
import org.cougaar.planning.ldm.plan.RoleScheduleImpl;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TaskRescind;

import org.cougaar.core.util.UID;

import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

import java.util.*;

/** RescindLogicProvider class provides the logic to capture 
 * rescinded PlanElements (removed from collection)
 *
 * Attempts to do a complete LogPlan rescind walk, not depending on
 * being re-called to do the "next" level of rescind.
 **/

public class RescindLP extends LogPlanLogicProvider implements EnvelopeLogicProvider {
  public static class DeferredRescind implements java.io.Serializable {
    public TaskRescind tr;
    public int tryCount = 0;
    public DeferredRescind(TaskRescind tr) {
      this.tr = tr;
    }
  }
    
  //private List conflictlist = new ArrayList();

  public RescindLP(LogPlanServesLogicProvider logplan,
		   ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }

  /**
   *  @param Object  Envelope.Tuple
   *             where Envelope.Tuple.object is an ADDED PlanElement which contains
   *                             an Allocation to a clustered asset.
   * Do something if the test returned true i.e. it was a PlanElement being removed
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    // drop changes
    Object obj = o.getObject();
    if (o.isRemove()) {
      if (obj instanceof Task) {  // task
        taskRemoved((Task) obj);
      }else  if (obj instanceof PlanElement) {                    // PE
        planElementRemoved((PlanElement) obj);
      }
    } else if (o.isAdd()) {
      if (obj instanceof DeferredRescind) {
        processDeferredRescind((DeferredRescind) obj);
      } else if (obj instanceof PlanElement) {
        planElementAdded((PlanElement) obj);
      }
    }
  }

  private void planElementAdded(PlanElement pe) {
    Task task = pe.getTask();
    if (logplan.findTask(task) == null) {
//        System.out.println("Removing added planelement [task not found in the logplan] for " + task + " as " + pe);
      removePlanElement(pe, true);
    }
  }

  private void processDeferredRescind(DeferredRescind deferredRescind) {
    UID rtuid = deferredRescind.tr.getTaskUID();
    Task t = logplan.findTask(rtuid);
    if (t != null) {
      removeTask(t);
      logplan.remove(deferredRescind);
    } else {
      logplan.remove(deferredRescind);
    }
  }

  /** remove PE and any cascade objects */
  private void removePlanElement(PlanElement pe, boolean force) {
     if (pe != null) {
       if (force || logplan.findPlanElement(pe.getTask()) != null) {
         logplan.remove(pe);
//      planElementRemoved(pe);
       }
     }
   }

  /** rescind the cascade of any PE (does not remove the PE) */
  private void planElementRemoved(PlanElement pe) {
    //System.err.print("p");
    if (pe instanceof Allocation) {
      // remove planelement from the asset's roleschedule
      //removePERS(pe);
      allocationRemoved((Allocation) pe);
    } else if (pe instanceof Expansion) {
      // Do nothing
    } else if (pe instanceof AssetTransfer) {
      // remove planelement from the asset's roleschedule
      //removePERS(pe);
      assetTransferRemoved((AssetTransfer) pe);
    } else if (pe instanceof Aggregation) {
      // Do nothing
    } else if (pe instanceof Disposition) {
      // do nothing since its the end of the line
    } else {
      System.err.println("Unknown planelement "+pe);
      Thread.dumpStack();
    }
  }

  /** rescind the cascade of an allocation */
  private void allocationRemoved(Allocation all) {
    //System.err.print("a");
    Asset a = all.getAsset();
    ClusterPG cpg = a.getClusterPG();
    if (cpg != null) {
      ClusterIdentifier cid = cpg.getClusterIdentifier();
      if (cid != null) {
        Task rt = ((AllocationforCollections) all).getAllocationTask();
        if (rt != null) {
          if (rt.isDeleted()) return; // Already deleted
          TaskRescind trm = ldmf.newTaskRescind(rt, cid);
          ((AllocationforCollections) all).setAllocationTask(null);
          logplan.sendDirective((Directive) trm);
        }
      }
    }
  }

  /** remove a task and any PE addressing it */
  private void removeTask(Task task) {
    if (task != null) {
      logplan.remove(task);
    }
  }

  /** remove the PE associated with a task (does not remove the task) */
  private void taskRemoved(Task task) {
    // get the planelement with this task
    PlanElement taskpe = logplan.findPlanElement(task);
    // rescind (or remove) this planelement from the collection
    if (taskpe != null) {
      removePlanElement(taskpe, false);
    }
  }

  /** remove the cascade associated with an AssetTransfer **/
  private void assetTransferRemoved(AssetTransfer at) {
    // create an AssetRescind message
    Schedule rescindSchedule;


    //Remove info from local assets
    Asset localAsset = logplan.findAsset(at.getAsset());
    if (localAsset == null) {
      System.err.println("RescindLP: rescinded transferred asset - " + 
                         at.getAsset() + " - not found in logplan.");
      return;
    }


    if ((at.getAsset() instanceof HasRelationships) &&
        (at.getAssignee() instanceof HasRelationships)) {
      rescindSchedule = ldmf.newAssignedRelationshipSchedule();
      RelationshipSchedule transferSchedule = 
        ((HasRelationships)at.getAsset()).getRelationshipSchedule();

      for (Iterator iterator = new ArrayList(transferSchedule).iterator();
           iterator.hasNext();) {
        Relationship relationship = (Relationship)iterator.next();
        ((NewSchedule)rescindSchedule).add(ldmf.newAssignedRelationshipElement(relationship));
      }
      
      HasRelationships localAssignee = (HasRelationships)logplan.findAsset(at.getAssignee());
      if (localAssignee == null) {
        System.err.println("RescindLP: rescinded assignee - " + 
                           at.getAssignee() + " - not found in logplan.");
        return;
      }

      // Update local relationship schedules
      RelationshipSchedule localSchedule = 
        ((HasRelationships) localAsset).getRelationshipSchedule();
      localSchedule.removeAll(transferSchedule);        
      
      localSchedule = localAssignee.getRelationshipSchedule();
      localSchedule.removeAll(transferSchedule);
      
      // Update asset avail
      // Remove all current entries denoting asset avail to assignee
      // Will add in new entry based on the current relationship schedule
      NewSchedule assetAvailSchedule = 
        (NewSchedule) localAsset.getRoleSchedule().getAvailableSchedule();
      final Asset assignee = at.getAssignee();
      synchronized (assetAvailSchedule) {
        Collection remove = assetAvailSchedule.filter(new UnaryPredicate() {
          public boolean execute(Object o) {
            return ((o instanceof AssignedAvailabilityElement) &&
                    (((AssignedAvailabilityElement)o).getAssignee().equals(assignee)));
          }  
        });
        assetAvailSchedule.removeAll(remove);
      } // end sync block

      // Get all relationships with asset
      RelationshipSchedule relationshipSchedule = 
        (localAssignee).getRelationshipSchedule();
      Collection collection = 
        relationshipSchedule.getMatchingRelationships((HasRelationships) localAsset,
                                                      new MutableTimeSpan());
      
      // If any relationships, add a single avail element with the 
      // min start and max end
      if (collection.size() > 0) {
        Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
        
        // Add a new avail element
        synchronized (assetAvailSchedule) {
          assetAvailSchedule.add(ldmf.newAssignedAvailabilityElement((Asset)localAssignee,
                                                                     schedule.getStartTime(),
                                                                     schedule.getEndTime()));
        }
      }

      logplan.change(localAsset, null);
      logplan.change(localAssignee, null);
    } else {
      rescindSchedule = at.getSchedule();

      // Update asset avail - remove all current entries which match the rescind
      // schedule
      NewSchedule assetAvailSchedule = 
        (NewSchedule)((Asset)localAsset).getRoleSchedule().getAvailableSchedule();
      final Asset assignee = at.getAssignee();
      synchronized (assetAvailSchedule) {
        final Asset asset = (Asset)localAsset;
        Collection assignedAvailSchedule = assetAvailSchedule.filter(new UnaryPredicate() {
          public boolean execute(Object o) {
            return ((o instanceof AssignedAvailabilityElement) &&
                    (((AssignedAvailabilityElement)o).getAssignee().equals(assignee)));
          }  
        });
        
        //iterate over rescind schedule and remove matching avail elements
        for (Iterator iterator = rescindSchedule.iterator();
             iterator.hasNext();) {
          ScheduleElement rescind = (ScheduleElement)iterator.next();
    
          Iterator localIterator = assignedAvailSchedule.iterator();
      
          boolean found = false;
          while (localIterator.hasNext()) {
            ScheduleElement localAvailability = 
              (ScheduleElement)localIterator.next();

            if ((rescind.getStartTime() == localAvailability.getStartTime()) &&
                (rescind.getEndTime() == localAvailability.getEndTime())) {
              assignedAvailSchedule.remove(localAvailability);
              break;
            }
          }
        }
      }
      logplan.change(localAsset, null);
    }
   
    AssetRescind arm = ldmf.newAssetRescind(at.getAsset(), 
                                            at.getAssignee(),
                                            rescindSchedule);
    logplan.sendDirective((Directive)arm);
  }
  
  /** remove the plan element from the asset's roleschedule **/
  private void removePERS(PlanElement pe) {
    /*
    boolean conflict = false;
    */
    Asset rsasset = null;
    if (pe instanceof Allocation) {
      Allocation alloc = (Allocation) pe;
      rsasset = alloc.getAsset();
      /*
      if ( alloc.isPotentialConflict() ) {
        conflict = true;
      }
      */
    } else if (pe instanceof AssetTransfer) {
      AssetTransfer at = (AssetTransfer) pe;
      rsasset = at.getAsset();
      /*
      if ( at.isPotentialConflict() ) {
        conflict = true;
      }
      */
    }
    if (rsasset != null) {
      //System.err.println("\n RESCIND REMOVEPERS called for: " + rsasset);
      RoleScheduleImpl rsi = (RoleScheduleImpl) rsasset.getRoleSchedule();
      // if the pe had a conflict re-check the roleschedule
      //if (conflict) {
      //  checkConflictFlags(pe, rsi);
      //}
    } else {
      System.err.println("\n WARNING could not remove rescinded planelement");
    }
  }
  
  /*
  // if the rescinded pe had a potential conflict re-set the conflicting pe(s)
  private void checkConflictFlags(PlanElement pe, RoleSchedule rs) {
    // re-set any existing items in the conflict list.
    conflictlist.clear();
    AllocationResult estar = pe.getEstimatedResult();
    
    // make sure that the start time and end time aspects are defined.
    // if they aren't, don't check anything
    // (this could happen with a propagating failed allocation result).
    if ( (estar.isDefined(AspectType.START_TIME) ) && (estar.isDefined(AspectType.END_TIME) ) ) {
      Date sdate = new Date( ((long)estar.getValue(AspectType.START_TIME)) );
      Date edate = new Date( ((long)estar.getValue(AspectType.END_TIME)) );
    
      // check for encapsulating schedules of other plan elements.
      OrderedSet encap = rs.getEncapsulatedRoleSchedule(sdate, edate);
      Enumeration encapconflicts = encap.elements();
      while (encapconflicts.hasMoreElements()) {
        PlanElement conflictpe = (PlanElement) encapconflicts.nextElement();
        // make sure its not our pe.
        if ( !(conflictpe == pe) ) {
          conflictlist.add(conflictpe);
        }
      }
    
      // check for ovelapping schedules of other plan elements.
      OrderedSet overlap = rs.getOverlappingRoleSchedule(sdate, edate);
      Enumeration overlapconflicts = overlap.elements();
      while (overlapconflicts.hasMoreElements()) {
        PlanElement overconflictpe = (PlanElement) overlapconflicts.nextElement();
        // once again, make sure its not our pe.
        if ( !(overconflictpe == pe) ) {
          conflictlist.add(overconflictpe);
        }
      }
    }
    
    if ( ! conflictlist.isEmpty() ) {
      ListIterator lit = conflictlist.listIterator();
      while ( lit.hasNext() ) {
        RoleScheduleConflicts conpe = (RoleScheduleConflicts) lit.next();
        // re-set this pe's conflict flag to false
        conpe.setPotentialConflict(false);
        // set the check flag to true so that the RoleScheduleConflictLP will
        // run again on the publish change in case this pe had conflicts with
        // other pe's (besides the one that was just rescinded)
        conpe.setCheckConflicts(true);
        logplan.change(conpe);
      }
    }
  }
  */
}




/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.lps;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.ClusterServesLogicProvider;
import org.cougaar.core.cluster.EnvelopeLogicProvider;
import org.cougaar.core.cluster.EnvelopeTuple;
import org.cougaar.core.cluster.LogPlanLogicProvider;
import org.cougaar.core.cluster.LogPlanServesLogicProvider;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.ClusterPG;

import org.cougaar.domain.planning.ldm.plan.Aggregation;
import org.cougaar.domain.planning.ldm.plan.Allocation;
import org.cougaar.domain.planning.ldm.plan.AllocationforCollections;
import org.cougaar.domain.planning.ldm.plan.AssetRescind;
import org.cougaar.domain.planning.ldm.plan.AssetTransfer;
import org.cougaar.domain.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.Disposition;
import org.cougaar.domain.planning.ldm.plan.Expansion;
import org.cougaar.domain.planning.ldm.plan.HasRelationships;
import org.cougaar.domain.planning.ldm.plan.NewSchedule;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Relationship;
import org.cougaar.domain.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.domain.planning.ldm.plan.RoleSchedule;
import org.cougaar.domain.planning.ldm.plan.RoleScheduleImpl;
import org.cougaar.domain.planning.ldm.plan.Schedule;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.TaskRescind;

import org.cougaar.core.society.UID;

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
      System.out.println("Removing added planelement [task not found in the logplan] for " + task + " as " + pe);
      removePlanElement(pe);
    }
  }

  private void processDeferredRescind(DeferredRescind deferredRescind) {
    UID rtuid = deferredRescind.tr.getTaskUID();
    Task t = logplan.findTask(rtuid);
    if (t != null) {
      /*
      System.err.println("DeferredRescind Succeeded " +
                         deferredRescind.tryCount +
                         ": " + rt.getUID());
      */
      removeTask(t);
      logplan.remove(deferredRescind);
    } else {
      System.err.println("DeferredRescind failed [task not found in the logplan]: " + rtuid);
      logplan.remove(deferredRescind);
    }
  }

  /** remove PE and any cascade objects */
  private void removePlanElement(PlanElement pe) {
    if (pe != null) {
      logplan.remove(pe);
//        planElementRemoved(pe);
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
      removePlanElement(taskpe);
    }
  }

  /** remove the cascade associated with an AssetTransfer **/
  private void assetTransferRemoved(AssetTransfer at) {
    // create an AssetRescind message
    Schedule rescindSchedule;

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

      //Remove info from local assets
      HasRelationships localAsset = (HasRelationships)logplan.findAsset(at.getAsset());
      if (localAsset == null) {
        System.err.println("RescindLP: rescinded transferred asset - " + 
                           at.getAsset() + " - not found in logplan.");
        return;
      }
      
      HasRelationships localAssignee = (HasRelationships)logplan.findAsset(at.getAssignee());
      if (localAssignee == null) {
        System.err.println("RescindLP: rescinded assignee - " + 
                           at.getAssignee() + " - not found in logplan.");
        return;
      }

      // Update local relationship schedules
      RelationshipSchedule localSchedule = localAsset.getRelationshipSchedule();
      localSchedule.removeAll(transferSchedule);        
      
      localSchedule = localAssignee.getRelationshipSchedule();
      localSchedule.removeAll(transferSchedule);
      
      // Update assignee avail
      // Remove all current entries denoting asset avail to assignee
      NewSchedule assigneeAvailSchedule = 
        (NewSchedule)((Asset)localAssignee).getRoleSchedule().getAvailableSchedule();
      synchronized (assigneeAvailSchedule) {
        final Asset asset = (Asset)localAsset;
        Collection remove = assigneeAvailSchedule.filter(new UnaryPredicate() {
          public boolean execute(Object o) {
            return ((o instanceof AssignedAvailabilityElement) &&
                    (((AssignedAvailabilityElement)o).getAssignee().equals(asset)));
          }  
        });
        assigneeAvailSchedule.removeAll(remove);
      } // end sync block

      // Get all relationships with asset
      RelationshipSchedule relationshipSchedule = 
        (localAssignee).getRelationshipSchedule();
      Collection collection = 
        relationshipSchedule.getMatchingRelationships(localAsset,
                                                      new MutableTimeSpan());
      
      // If any relationships, add a single avail element with the 
      // min start and max end
      if (collection.size() > 0) {
        Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
        
        // Add a new avail element
        synchronized (assigneeAvailSchedule) {
          assigneeAvailSchedule.add(ldmf.newAssignedAvailabilityElement((Asset)localAsset,
                                                                        schedule.getStartTime(),
                                                                        schedule.getEndTime()));
        }
      }

      logplan.change(localAsset, null);
      logplan.change(localAssignee, null);
    } else {
      rescindSchedule = at.getSchedule();
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




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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.cougaar.core.cluster.*;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.PropertyGroup;

import org.cougaar.domain.planning.ldm.plan.AssetAssignment;
import org.cougaar.domain.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.domain.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.HasRelationships;
import org.cougaar.domain.planning.ldm.plan.NewRelationshipSchedule;
import org.cougaar.domain.planning.ldm.plan.NewRoleSchedule;
import org.cougaar.domain.planning.ldm.plan.NewSchedule;
import org.cougaar.domain.planning.ldm.plan.Relationship;
import org.cougaar.domain.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.domain.planning.ldm.plan.Role;
import org.cougaar.domain.planning.ldm.plan.RoleSchedule;
import org.cougaar.domain.planning.ldm.plan.Schedule;
import org.cougaar.domain.planning.ldm.plan.ScheduleElement;

import org.cougaar.util.Enumerator;
import org.cougaar.util.TimeSpan;


/**
  * Sample LogicProvider for use by ClusterDispatcher to
  * take an incoming AssetAssignment Directive and
  * add Asset to the LogPlan w/side-effect of also disseminating to
  * other subscribers.
  **/

public class ReceiveAssetLP extends LogPlanLogicProvider implements MessageLogicProvider
{
  public ReceiveAssetLP(LogPlanServesLogicProvider logplan,
                       ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }

  /**
   * Adds Assets  to LogPlan... Side-effect = other subscribers
   * also updated.
   **/
  public void execute(Directive dir, Collection changes)
  {
    if (dir instanceof AssetAssignment) {
      AssetAssignment aa  = (AssetAssignment)dir;
      receiveAssetAssignment(aa);
    }
  }

  private final static boolean related(Asset a) {
    return (a instanceof HasRelationships); 
  }

  private void receiveAssetAssignment(AssetAssignment aa) {
    // figure out the assignee
    Asset assigneeT = aa.getAssignee();// assignee from message
    Asset assigneeL = logplan.findAsset(assigneeT); // local assignee instance
    if (assigneeL == null) {
      System.err.println("ReceiveAssetLP: Unable to find receiving asset " + 
                         assigneeT + " in "+cluster);
      return;
    }
    Asset assignee = assigneeL;

    // figure out the asset being transferred
    Asset assetT = aa.getAsset();   // asset from message
    Asset assetL = logplan.findAsset(assetT);// local instance of asset

    Asset asset = (assetL == null) ?
      // Clone to ensure that we don't end up with cross cluster asset 
      // references
      ldmf.cloneInstance(assetT) :
      assetL;  

    boolean updateRelationships = (related(assetT) && related(assignee));

    //Only munge relationships pertinent to the transfer - requires that 
    //both receiving and transferring assets have relationship schedules
    if (updateRelationships) {
      HasRelationships dob = ((HasRelationships)asset);
      NewRelationshipSchedule rs_a;

      if (assetL == null) {
        // Serialized object does not include a relationship schedule
        rs_a = ldmf.newRelationshipSchedule(dob);
        dob.setRelationshipSchedule(rs_a);        
      } else {
        rs_a = (NewRelationshipSchedule)dob.getRelationshipSchedule();
      }

      HasRelationships iob = ((HasRelationships)assignee);
      NewRelationshipSchedule rs_assignee = 
        (NewRelationshipSchedule)iob.getRelationshipSchedule();

      if (aa.isUpdate() || aa.isRepeat()) {
        removeExistingRelationships(aa.getSchedule(), rs_a, rs_assignee);
      } 

      addRelationships(aa.getSchedule(), rs_a, rs_assignee);
      
      Collection changeReports = new ArrayList();
      changeReports.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
      logplan.change(assignee, changeReports);
    }


    if (assetL != null) {
      // If we already had a matching asset - update with property groups
      // from the asset transfer.
      Vector transferredPGs = assetT.fetchAllProperties();
      
      for (Iterator pgIterator = transferredPGs.iterator();
           pgIterator.hasNext();) {
        PropertyGroup transferredPG = (PropertyGroup) pgIterator.next();

        assetL.addOtherPropertyGroup(transferredPG);
      }
    } else {
      if (aa.isUpdate() || aa.isRepeat()) {
        System.err.println("Received Update Asset Transfer, but cannot find original "+
                           aa);
      } 
    }

    // fix up the available schedule
    fixAvailSchedule(aa, asset, assignee);

    // publish the add or change
    if (assetL == null) {            // add it if it wasn't already there
      logplan.add(asset);
    } else {
      if (updateRelationships) {
        Collection changeReports = new ArrayList();
        changeReports.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
        logplan.change(asset, changeReports);
      } else {
        logplan.change(asset, null);
      }
    }


  }

  protected void addRelationships(Schedule aaSchedule,
                                  RelationshipSchedule transferringSchedule,
                                  RelationshipSchedule receivingSchedule) {
    Asset transferring = (Asset)transferringSchedule.getHasRelationships();
    String transferringID = transferring.getItemIdentificationPG().getItemIdentification();

    Asset receiving = (Asset)receivingSchedule.getHasRelationships();
    String receivingID = receiving.getItemIdentificationPG().getItemIdentification();
    
    synchronized (aaSchedule) {
      Iterator iterator = aaSchedule.iterator();
      
      while (iterator.hasNext()) {
        AssignedRelationshipElement aaRelationship = 
          (AssignedRelationshipElement)iterator.next();
        
        Relationship localRelationship = ldmf.newRelationship(aaRelationship,
                                                              transferring,
                                                              receiving);
        transferringSchedule.add(localRelationship);
        
        localRelationship = ldmf.newRelationship(aaRelationship,
                                                 transferring,
                                                 receiving);
        receivingSchedule.add(localRelationship);
      }
    }
  }

  private void removeExistingRelationships(Schedule aaSchedule,
                                           NewRelationshipSchedule transferringSchedule,
                                           NewRelationshipSchedule receivingSchedule) {
    HasRelationships transferringAsset = 
      transferringSchedule.getHasRelationships();
    String transferringID = 
      ((Asset) transferringAsset).getItemIdentificationPG().getItemIdentification();

    HasRelationships receivingAsset = 
      receivingSchedule.getHasRelationships();
    String receivingID = 
      ((Asset) receivingAsset).getItemIdentificationPG().getItemIdentification();

    Iterator aaIterator = aaSchedule.iterator();

    while (aaIterator.hasNext()) {
      AssignedRelationshipElement aaRelationship = 
        (AssignedRelationshipElement) aaIterator.next();
      
      Role role = (aaRelationship.getItemIDA().equals(receivingID)) ?
        aaRelationship.getRoleA() : aaRelationship.getRoleB();
                                                                            
      Collection remove = 
        transferringSchedule.getMatchingRelationships(role,
                                                      receivingAsset,
                                                      TimeSpan.MIN_VALUE,
                                                      TimeSpan.MAX_VALUE);
      transferringSchedule.removeAll(remove);

      role = (aaRelationship.getItemIDA().equals(transferringID)) ?
        aaRelationship.getRoleA() : aaRelationship.getRoleB();
      remove = 
        receivingSchedule.getMatchingRelationships(role,
                                                   transferringAsset,
                                                   TimeSpan.MIN_VALUE,
                                                   TimeSpan.MAX_VALUE);
      receivingSchedule.removeAll(remove);
    }
  }

  private void fixAvailSchedule(AssetAssignment aa, Asset asset,
                                Asset assignee) {
    NewSchedule availSchedule = 
      (NewSchedule)asset.getRoleSchedule().getAvailableSchedule();

    if (availSchedule == null) {
      availSchedule = ldmf.newAssignedAvailabilitySchedule();
      ((NewRoleSchedule)asset.getRoleSchedule()).setAvailableSchedule(availSchedule);
    }

    if ((aa.isUpdate() || aa.isRepeat()) || 
        (related(asset) && related(assignee))) {
      // Remove existing entries 
      Iterator iterator = availSchedule.iterator();
      while (iterator.hasNext()) {
        Object next = iterator.next();
        if ((next instanceof AssignedAvailabilityElement) &&
            (((AssignedAvailabilityElement)next).getAssignee().equals(assignee))) {
          iterator.remove();
        }
      }
    }
    
    //Add entries to the available schedule using the assignee
    if (related(asset) && related(assignee)) {
      //Construct aggregate avail info from the relationship schedule
      RelationshipSchedule relationshipSchedule = 
        ((HasRelationships)asset).getRelationshipSchedule();
      Collection collection = 
        relationshipSchedule.getMatchingRelationships((HasRelationships)assignee,
                                                      TimeSpan.MIN_VALUE,
                                                      TimeSpan.MAX_VALUE);

      // If any relationships, add a single avail element with the 
      // min start and max end
      if (collection.size() > 0) {
        Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
        availSchedule.add(ldmf.newAssignedAvailabilityElement(assignee,
                                                              schedule.getStartTime(),
                                                              schedule.getEndTime()));
      }
    } else {
      //Copy availability info directly from aa schedule
      Iterator iterator = aa.getSchedule().iterator();
      while (iterator.hasNext()) {
        ScheduleElement avail = (ScheduleElement)iterator.next();
        availSchedule.add(ldmf.newAssignedAvailabilityElement(assignee, 
                                                              avail.getStartTime(),
                                                              avail.getEndTime()));
      }
    }
  }
}









/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.lps;

import java.util.*;

import org.cougaar.core.cluster.*;

import org.cougaar.domain.planning.ldm.asset.Asset;

import org.cougaar.domain.planning.ldm.plan.AssetRescind;
import org.cougaar.domain.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.domain.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.HasRelationships;
import org.cougaar.domain.planning.ldm.plan.NewSchedule;
import org.cougaar.domain.planning.ldm.plan.Relationship;
import org.cougaar.domain.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.domain.planning.ldm.plan.Schedule;

import org.cougaar.core.society.UID;

import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;


/**
 * Catch assets so that we can relink the relationships properly.
 **/

public class ReceiveAssetRescindLP
  extends LogPlanLogicProvider implements MessageLogicProvider
{

  public ReceiveAssetRescindLP(LogPlanServesLogicProvider logplan,
                               ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }
  
  /**
   *  perform updates -- per Rescind ALGORITHM --
   *
   **/
  public void execute(Directive dir, Collection changes) {
    // drop changes
    if (dir instanceof AssetRescind) {
      receiveAssetRescind((AssetRescind)dir);
    }
  }

  private final static boolean related(Asset a) {
    return (a instanceof HasRelationships); 
  }

  private void receiveAssetRescind(AssetRescind ar) {
    Asset localAsset = logplan.findAsset(ar.getAsset());
    if (localAsset == null) {
      System.err.println("ReceiveAssetRescindLP: rescinded asset - " + 
                         ar.getAsset() + " - not found in logplan.");
      return;
    }

    Asset localAssignee = logplan.findAsset(ar.getRescindee());
    if (localAssignee == null) {
      System.err.println("ReceiveAssetRescindLP: assignee - " + 
                         ar.getRescindee() + " - not found in logplan.");
      return;
    }


    if (related(ar.getAsset()) &&
        related(ar.getRescindee())) {
      updateRelationshipSchedules(ar, localAsset, localAssignee);
    }

    updateAvailSchedule(ar, localAsset, localAssignee);
  
    logplan.change(localAsset, null);
    logplan.change(localAssignee, null);
  }

  private void updateRelationshipSchedules(AssetRescind ar,
                                           Asset asset,
                                           Asset assignee) {

    String assetID = 
      asset.getItemIdentificationPG().getItemIdentification();

    RelationshipSchedule assetRelationshipSchedule = 
      ((HasRelationships) asset).getRelationshipSchedule();

    RelationshipSchedule assigneeRelationshipSchedule = 
      ((HasRelationships) assignee).getRelationshipSchedule();


    // Remove matching relationships
    Collection rescinds = convertToRelationships(ar, asset, assignee);

    assetRelationshipSchedule.removeAll(rescinds);

    assigneeRelationshipSchedule.removeAll(rescinds);
  }

  // Update availability info for the asset (aka transferring asset)
  // AvailableSchedule reflects availablity within the current cluster
  private void updateAvailSchedule(AssetRescind ar,
                                   Asset asset,
                                   final Asset assignee) {

    NewSchedule assetAvailSchedule = 
      (NewSchedule)asset.getRoleSchedule().getAvailableSchedule();

    if (!related(asset)) {
    
      // Remove Matching Availabilities
      synchronized (assetAvailSchedule) {
        assetAvailSchedule.removeAll(ar.getSchedule());
      }

      // We're done
      return;
    }
    
    //For Assets with relationships, need to recompute the avail schedule
    //based on the relationship schedule

    // Remove all current entries denoting asset avail to assignee
    synchronized (assetAvailSchedule) {
      Collection remove = assetAvailSchedule.filter(new UnaryPredicate() {
        public boolean execute(Object o) {
          return ((o instanceof AssignedAvailabilityElement) &&
                  (((AssignedAvailabilityElement)o).getAssignee().equals(assignee)));
        }  
      });
      assetAvailSchedule.removeAll(remove);
      
      // Get all relationships between asset and assignee
      RelationshipSchedule relationshipSchedule = 
        ((HasRelationships) asset).getRelationshipSchedule();
      Collection collection = 
        relationshipSchedule.getMatchingRelationships((HasRelationships) assignee,
                                                      new MutableTimeSpan());
      
      // If any relationships, add a single avail element with the 
      // min start and max end
      if (collection.size() > 0) {
        Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
        
        // Add a new avail element
        assetAvailSchedule.add(ldmf.newAssignedAvailabilityElement(assignee,
                                                                   schedule.getStartTime(),
                                                                   schedule.getEndTime()));
      }
    } // end sync block
  }

  protected Collection convertToRelationships(AssetRescind ar,
                                              Asset asset,
                                              Asset assignee) {
    ArrayList relationships = new ArrayList(ar.getSchedule().size());

    // Safe because ar.getSchedule is an AssignedRelationshipScheduleImpl.
    // AssignedRelationshipImpl supports iterator. (Assumption is that
    // AssignedRelaionshipImpl is only used/processed by LPs.)
    for (Iterator iterator = ar.getSchedule().iterator(); 
         iterator.hasNext();) {
      AssignedRelationshipElement rescindElement = 
        (AssignedRelationshipElement)iterator.next();
      
      Relationship relationship = ldmf.newRelationship(rescindElement,
                                                       asset,
                                                       assignee);
      
      relationships.add(relationship);
    }

    return relationships;
  }
}
 







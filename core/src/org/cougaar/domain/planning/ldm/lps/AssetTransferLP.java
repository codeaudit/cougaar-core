/*
 * <Copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.lps;

import org.cougaar.core.cluster.*;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.ClusterPG;

import org.cougaar.domain.planning.ldm.plan.AssetAssignment;
import org.cougaar.domain.planning.ldm.plan.AssetTransfer;
import org.cougaar.domain.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.domain.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.domain.planning.ldm.plan.HasRelationships;
import org.cougaar.domain.planning.ldm.plan.NewAssetAssignment;
import org.cougaar.domain.planning.ldm.plan.NewAssetVerification;
import org.cougaar.domain.planning.ldm.plan.NewRelationshipSchedule;
import org.cougaar.domain.planning.ldm.plan.NewRoleSchedule;
import org.cougaar.domain.planning.ldm.plan.NewSchedule;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Relationship;
import org.cougaar.domain.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.domain.planning.ldm.plan.RelationshipScheduleImpl;
import org.cougaar.domain.planning.ldm.plan.Role;
import org.cougaar.domain.planning.ldm.plan.Schedule;
import org.cougaar.domain.planning.ldm.plan.ScheduleElement;

import org.cougaar.util.Debug;

import org.cougaar.util.Enumerator;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

import java.util.*;

/** AssetTransferLP is a "LogPlan Logic Provider":
  *
  * it provides the logic to capture
  * PlanElements that are AssetTransfers and send AssetAssignment tasks
  * to the proper remote cluster.
  **/

public class AssetTransferLP
  extends LogPlanLogicProvider
  implements EnvelopeLogicProvider, RestartLogicProvider
{

  public AssetTransferLP(LogPlanServesLogicProvider logplan,
                         ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }

  /**
   * @param Object Envelopetuple,
   *          where tuple.object
   *             == PlanElement with an Allocation to an cluster ADDED to LogPlan
   *
   * If the test returned true i.e. it was an AssetTransfer...
   * create an AssetAssignment task and send itto a remote Cluster 
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj;
    if ((o.isAdd() || o.isChange()) &&
        (obj = o.getObject()) != null &&
         obj instanceof AssetTransfer) {

      AssetTransfer at = (AssetTransfer) obj;
      AssetAssignment assetassign;
    
      // create an AssetAssignment task
      assetassign =
        createAssetAssignment(at, 
                              (o.isChange()) ? 
                              AssetAssignment.UPDATE : AssetAssignment.NEW);
      if (assetassign != null) {
        // Give the AssetAssignment to the logplan for transmission
        logplan.sendDirective(assetassign);
      }
    }
  }

  // RestartLogicProvider implementation

  /**
   * Cluster restart handler. Resend all our assets to the restarted
   * cluster marking them as "REPEAT". Also send AssetVerification
   * messages for all the assets we have received from the restarted
   * cluster. The restarted cluster will rescind them if they are no
   * longer valid.
   **/
  public void restart(final ClusterIdentifier cid) {
    System.out.println("Resending assets to " + cid);
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof AssetTransfer) {
          AssetTransfer at = (AssetTransfer) o;
          return cid.equals(at.getAssignee().getClusterPG().getClusterIdentifier());
        }
        return false;
      }
    };
    Enumeration enum = logplan.searchALPPlan(pred);
    while (enum.hasMoreElements()) {
      AssetTransfer at = (AssetTransfer) enum.nextElement();
      System.out.println("Resending " + at);
      logplan.sendDirective(createAssetAssignment(at, AssetAssignment.REPEAT));
    }
    pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Asset) {
          Asset asset = (Asset) o;
          ClusterPG clusterPG = asset.getClusterPG();
          if (clusterPG != null) {
            return (clusterPG.getClusterIdentifier().equals(cid));
          }
        }
        return false;
      }
    };
    for (enum = logplan.searchALPPlan(pred); enum.hasMoreElements(); ) {
      Asset asset = (Asset) enum.nextElement();
      
      if (related(asset)) {

        HashMap hash = new HashMap(3);

        RelationshipScheduleImpl relationshipSchedule = 
          (RelationshipScheduleImpl)((HasRelationships)asset).getRelationshipSchedule();

        for (Iterator iterator = relationshipSchedule.iterator();
             iterator.hasNext();) {
          Relationship relationship = (Relationship)iterator.next();

          Asset otherAsset = 
            (Asset)relationshipSchedule.getOther(relationship);

          NewSchedule verifySchedule = (NewSchedule)hash.get(otherAsset);
          if (verifySchedule == null) {
            verifySchedule = ldmf.newAssignedRelationshipSchedule();
            hash.put(otherAsset, verifySchedule);
          }
          
          verifySchedule.add(ldmf.newAssignedRelationshipElement(relationship));
        }
        
        for (Iterator iterator = hash.keySet().iterator();
             iterator.hasNext();) {
          Asset receivingAsset = (Asset)iterator.next();

          Schedule verifySchedule = 
            (RelationshipSchedule)hash.get(receivingAsset);
                                 
          NewAssetVerification nav = 
            ldmf.newAssetVerification(ldmf.cloneInstance(asset),
                                      ldmf.cloneInstance(receivingAsset),
                                      verifySchedule);
          nav.setSource(cluster.getClusterIdentifier());
          nav.setDestination(cid);
          System.out.println("Verifying " + asset + " with " + nav);
          logplan.sendDirective(nav);
        }
      } else {
        // BOZO - we have not tested transferring non-org assets
        System.err.println("AssetTransferLP - unable to verify transfer of " +
                           asset + "\n.");
      }
        
    }
    System.out.println("Verifying finished");
  }
  
  private final static boolean related(Asset a) {
    return (a instanceof HasRelationships); 
  }

  private AssetAssignment createAssetAssignment(AssetTransfer at, byte kind) {
    NewAssetAssignment naa = ldmf.newAssetAssignment();

    /* copy the asset so we don't share roleschedule across
     * cluster boundaries.
     */
    naa.setAsset(ldmf.cloneInstance(at.getAsset()));
    
    naa.setPlan(ldmf.getRealityPlan());
    
    naa.setAssignee(ldmf.cloneInstance(at.getAssignee()));

    naa.setSource(at.getAssignor());
    naa.setDestination(at.getAssignee().getClusterPG().getClusterIdentifier());

    naa.setKind(kind);

    Schedule s;

    if (related(naa.getAsset())) {
      s = makeAARelationshipSchedule(naa, at);

      if (!updateLocalAssets(at, kind)) {
        return null;
      }


      // Clear role and relationship schedules to ensure that there 
      // are no dangling references to other organizations.
      Asset asset = naa.getAsset();
      if (((HasRelationships )asset).getRelationshipSchedule() != 
          null) {
        ((HasRelationships )asset).getRelationshipSchedule().clear();
      }
      if (asset.getRoleSchedule() != null) {
        asset.getRoleSchedule().clear();
      } 
      if (asset.getRoleSchedule().getAvailableSchedule() != null) {
        asset.getRoleSchedule().getAvailableSchedule().clear();
      }

      Asset assignee = naa.getAssignee();
      if (((HasRelationships )assignee).getRelationshipSchedule() != 
          null) {
        ((HasRelationships )assignee).getRelationshipSchedule().clear();
      }
      if (assignee.getRoleSchedule() != null) {
        assignee.getRoleSchedule().clear();
      } 
      if (assignee.getRoleSchedule().getAvailableSchedule() != null) {
        assignee.getRoleSchedule().getAvailableSchedule().clear();
      }


    } else {
      s = ldmf.newSchedule(at.getSchedule().getAllScheduleElements());
    }

    naa.setSchedule(s);

    return naa;
  }
         
  private Schedule makeAARelationshipSchedule(NewAssetAssignment naa, 
                                              AssetTransfer at) { 
    
    // construct appropriate relationship schedule for the asset assignment
    Schedule aaAssetSchedule = 
      ldmf.newAssignedRelationshipSchedule();
    
    Iterator iterator = 
      ((HasRelationships)at.getAsset()).getRelationshipSchedule().iterator();
    while (iterator.hasNext()) {
      Relationship relationship = (Relationship)iterator.next();
      
      // Verify that all relationships are with the receiver
      if (!(relationship.getA().equals(at.getAssignee())) &&
          !(relationship.getB().equals(at.getAssignee()))) {
        System.err.println("AssetTransferLP: Relationships on the " + 
                           " AssetTransfer must be limited to the " + 
                           " transferring and receiving asset.\n" + 
                           "Dropping relationship " + relationship + 
                           " on transfer of " + at.getAsset() + " to " + 
                           at.getAssignee());
        continue;
      }
      
      Asset a = (relationship.getA().equals(naa.getAsset())) ?
        naa.getAsset() : naa.getAssignee();
      Asset b = (relationship.getB().equals(naa.getAsset())) ?
        naa.getAsset() : naa.getAssignee();
      AssignedRelationshipElement element = 
        ldmf.newAssignedRelationshipElement(a,
                                            relationship.getRoleA(),
                                            b,
                                            relationship.getStartTime(),
                                            relationship.getEndTime());
      aaAssetSchedule.add(element);
    }
    
    return aaAssetSchedule;
  }

  private boolean updateLocalAssets(AssetTransfer at, byte kind) {
    Asset localTransferringAsset = logplan.findAsset(at.getAsset());
    if (localTransferringAsset == null) {
      System.err.println("AssetTransferLP: unable to process AssetTransfer - " + 
                         at.getAsset() + " - transferring to " + 
                         at.getAssignee()+ " - is not local to this cluster.");
      return false;
    }
              
    Asset receivingAsset = at.getAssignee();
    Asset localReceivingAsset = logplan.findAsset(receivingAsset);

    if (localReceivingAsset == null) {
      receivingAsset = ldmf.cloneInstance(receivingAsset);
      ((HasRelationships)receivingAsset).setRelationshipSchedule(ldmf.newRelationshipSchedule((HasRelationships)receivingAsset));
    } else {
      receivingAsset = localReceivingAsset;

    }

    RelationshipSchedule receivingSchedule =
      ((HasRelationships)receivingAsset).getRelationshipSchedule();
    RelationshipSchedule transferringSchedule = 
      ((HasRelationships)localTransferringAsset).getRelationshipSchedule();
    RelationshipSchedule atSchedule = 
      ((HasRelationships)at.getAsset()).getRelationshipSchedule();

    if ((kind == AssetAssignment.UPDATE) ||
        (kind == AssetAssignment.REPEAT)) {
      //Remove existing relationships
      removeExistingRelationships(atSchedule, 
                                  transferringSchedule, 
                                  receivingSchedule);
    }

    // Add transfer relationships from transfer to local assets
    for (Iterator iterator = atSchedule.iterator(); 
         iterator.hasNext();) {
      Relationship atRelationship = (Relationship)iterator.next();

      Asset A = 
        (atRelationship.getA().equals(at.getAsset())) ?
         localTransferringAsset : receivingAsset;
      Asset B = 
        (atRelationship.getB().equals(at.getAsset())) ?
        localTransferringAsset : receivingAsset;
      Relationship localRelationship = 
        ldmf.newRelationship(atRelationship.getRoleA(),
                             (HasRelationships)A,
                             (HasRelationships)B,
                             atRelationship.getStartTime(),
                             atRelationship.getEndTime());
      transferringSchedule.add(localRelationship);

      localRelationship = 
        ldmf.newRelationship(atRelationship.getRoleA(),
                             (HasRelationships)A,
                             (HasRelationships)B,
                             atRelationship.getStartTime(),
                             atRelationship.getEndTime());
      receivingSchedule.add(localRelationship);
    }

    fixAvailSchedule(receivingAsset, localTransferringAsset);
    
    Collection changes = new ArrayList();
    changes.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
    logplan.change(localTransferringAsset, changes);

    if (localReceivingAsset == null) {
      logplan.add(receivingAsset);
    } else {
      changes.clear();
      changes.add(new RelationshipSchedule.RelationshipScheduleChangeReport());
      logplan.change(receivingAsset, changes);
    }
    
    return true;
  }


  private void fixAvailSchedule(Asset receivingAsset, Asset transferringAsset) {
    NewSchedule availSchedule = 
      (NewSchedule)receivingAsset.getRoleSchedule().getAvailableSchedule();

    if (availSchedule == null) {
      availSchedule = ldmf.newAssignedAvailabilitySchedule();
      ((NewRoleSchedule)receivingAsset.getRoleSchedule()).setAvailableSchedule(availSchedule);
    }

    Iterator iterator = availSchedule.iterator();
    while (iterator.hasNext()) {
      Object next = iterator.next();
      if ((next instanceof AssignedAvailabilityElement) &&
          (((AssignedAvailabilityElement)next).getAssignee().equals(transferringAsset))) {
        iterator.remove();
      }
    }
    
    //Construct aggregate avail info from the relationship schedule
    RelationshipSchedule relationshipSchedule = 
      ((HasRelationships)receivingAsset).getRelationshipSchedule();
    Collection collection = 
      relationshipSchedule.getMatchingRelationships((HasRelationships)transferringAsset,
                                                    TimeSpan.MIN_VALUE,
                                                    TimeSpan.MAX_VALUE);

    // If any relationships, add a single avail element with the 
    // min start and max end
    if (collection.size() > 0) {
      Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
      availSchedule.add(ldmf.newAssignedAvailabilityElement(transferringAsset,
                                                            schedule.getStartTime(),
                                                            schedule.getEndTime()));
    }
  }

  private void removeExistingRelationships(RelationshipSchedule atSchedule,
                                           RelationshipSchedule transferringSchedule,
                                           RelationshipSchedule receivingSchedule) {
    HasRelationships transferringAsset = 
      transferringSchedule.getHasRelationships();
    HasRelationships receivingAsset = 
      receivingSchedule.getHasRelationships();

    for (Iterator atIterator = atSchedule.iterator();
         atIterator.hasNext();) {
      Relationship relationship = (Relationship) atIterator.next();
      
      Role role = (relationship.getA().equals(receivingAsset)) ?
        relationship.getRoleA() : relationship.getRoleB();
                                                                            
      Collection remove = 
        transferringSchedule.getMatchingRelationships(role,
                                                      receivingAsset,
                                                      TimeSpan.MIN_VALUE,
                                                      TimeSpan.MAX_VALUE);
      transferringSchedule.removeAll(remove);

      role = (relationship.getA().equals(transferringAsset)) ?
       relationship.getRoleA() :relationship.getRoleB();
      remove = 
        receivingSchedule.getMatchingRelationships(role,
                                                   transferringAsset,
                                                   TimeSpan.MIN_VALUE,
                                                   TimeSpan.MAX_VALUE);
      receivingSchedule.removeAll(remove);
    }
  }
}










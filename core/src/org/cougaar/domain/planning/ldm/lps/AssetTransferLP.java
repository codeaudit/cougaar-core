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
import org.cougaar.domain.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.domain.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.domain.planning.ldm.plan.HasRelationships;
import org.cougaar.domain.planning.ldm.plan.NewAssetAssignment;
import org.cougaar.domain.planning.ldm.plan.NewAssetVerification;
import org.cougaar.domain.planning.ldm.plan.NewRelationshipSchedule;
import org.cougaar.domain.planning.ldm.plan.NewRoleSchedule;
import org.cougaar.domain.planning.ldm.plan.NewSchedule;
import org.cougaar.domain.planning.ldm.plan.Relationship;
import org.cougaar.domain.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.domain.planning.ldm.plan.RelationshipScheduleImpl;
import org.cougaar.domain.planning.ldm.plan.Role;
import org.cougaar.domain.planning.ldm.plan.Schedule;

import org.cougaar.util.Debug;
import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
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
  private static TimeSpan ETERNITY = new MutableTimeSpan();

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
      boolean sendRelationships = o.isAdd();
      if (!sendRelationships && changes != null) {
        for (Iterator i = changes.iterator(); i.hasNext(); ) {
          ChangeReport changeReport = (ChangeReport) i.next();
          if (changeReport instanceof RelationshipSchedule.RelationshipScheduleChangeReport) {
            sendRelationships = true;
            break;
          }
        }
      }
      assetassign =
        createAssetAssignment(at, 
                              (o.isChange()) ? 
                              AssetAssignment.UPDATE : AssetAssignment.NEW,
                              sendRelationships);
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
    Enumeration enum = logplan.searchBlackboard(pred);
    while (enum.hasMoreElements()) {
      AssetTransfer at = (AssetTransfer) enum.nextElement();
      System.out.println("Resending " + at);
      logplan.sendDirective(createAssetAssignment(at, AssetAssignment.REPEAT, true));
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
    for (enum = logplan.searchBlackboard(pred); enum.hasMoreElements(); ) {
      Asset asset = (Asset) enum.nextElement();
      
      if (related(asset)) {

        HashMap hash = new HashMap(3);

        RelationshipSchedule relationshipSchedule = 
          (RelationshipSchedule)((HasRelationships)asset).getRelationshipSchedule();

        Collection relationships = new ArrayList(relationshipSchedule);
        for (Iterator iterator = relationships.iterator();
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
          
          Schedule verifySchedule = (Schedule)hash.get(receivingAsset);
          
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

  private AssetAssignment createAssetAssignment(AssetTransfer at, byte kind,
                                                boolean sendRelationships)
  {
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

    Schedule s = null;          // Null if relationships not being sent

    Asset asset = naa.getAsset();
    Asset assignee = naa.getAssignee();

    // Only fuss with relationship schedules if both Asset & Assignee implement
    // HasRelationships
    if (related(asset) & related(assignee)) {
      if (sendRelationships) {
        s = makeAARelationshipSchedule(naa, at);
        if (!updateLocalAssets(at, kind)) {
          return null;
        }
      }
    } else {
      s = ldmf.newSchedule(at.getSchedule().getAllScheduleElements());
    }
    naa.setSchedule(s);

    // Clear asset and assignee relationship, role, and available schedules to ensure 
    // that there are no references to other organizations.
    clearSchedule(asset);
    clearSchedule(assignee);

    return naa;
  }
         
  private Schedule makeAARelationshipSchedule(NewAssetAssignment naa, 
                                              final AssetTransfer at) { 
    
    // construct appropriate relationship schedule for the asset assignment
    Schedule aaAssetSchedule = 
      ldmf.newAssignedRelationshipSchedule();

    RelationshipSchedule relationshipSchedule = 
      ((HasRelationships)at.getAsset()).getRelationshipSchedule();
    Collection relationships = relationshipSchedule.filter(new UnaryPredicate() {
      public boolean execute(Object o) {
        Relationship relationship = (Relationship)o;        

        // Verify that all relationships are with the receiver
        if (!(relationship.getA().equals(at.getAssignee())) &&
            !(relationship.getB().equals(at.getAssignee()))) {
          System.err.println("AssetTransferLP: Relationships on the " + 
                             " AssetTransfer must be limited to the " + 
                             " transferring and receiving asset.\n" + 
                             "Dropping relationship " + relationship + 
                             " on transfer of " + at.getAsset() + " to " + 
                             at.getAssignee());
          return false;
        } else {
          return true;
        }
      }
    });

    for (Iterator iterator = relationships.iterator();
         iterator.hasNext();) {
      Relationship relationship = (Relationship)iterator.next();
      
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
    } else if (localTransferringAsset == at.getAsset()) {
      System.err.println("AssetTransferLP: Assets in AssetTransfer must be " +
                         " clones. AssetTransfer - " + at.getUID() + 
                         " - references assets in the log plan.");
    }
    
    Asset receivingAsset = at.getAssignee();
    Asset localReceivingAsset = logplan.findAsset(receivingAsset);

    if (localReceivingAsset == null) {
      receivingAsset = ldmf.cloneInstance(receivingAsset);
      ((HasRelationships)receivingAsset).setRelationshipSchedule(ldmf.newRelationshipSchedule((HasRelationships)receivingAsset));
    } else {
      receivingAsset = localReceivingAsset;

      if (localReceivingAsset == at.getAssignee()) {
        System.err.println("AssetTransferLP: Assets in AssetTransfer must be " +
                           " clones. AssetTransfer - " + at.getUID() + 
                           " - references assets in the log plan.");
      }
    }


    if ((kind == AssetAssignment.UPDATE) ||
        (kind == AssetAssignment.REPEAT)) {
      //Remove existing relationships
      removeExistingRelationships(at, 
                                  (HasRelationships)localTransferringAsset,
                                  (HasRelationships)receivingAsset);
    }

    // Add transfer relationships to local assets
    Collection localRelationships = 
      convertToLocalRelationships(at,
                                  localTransferringAsset,
                                  receivingAsset);


    RelationshipSchedule transferringSchedule = 
      ((HasRelationships)localTransferringAsset).getRelationshipSchedule();
    transferringSchedule.addAll(localRelationships);

    RelationshipSchedule receivingSchedule =
      ((HasRelationships)receivingAsset).getRelationshipSchedule();
    receivingSchedule.addAll(localRelationships);
      

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

  // Update availability info for the receiving asset
  // AvailableSchedule reflects availablity within the current cluster
  private void fixAvailSchedule(final Asset receivingAsset, 
                                final Asset transferringAsset) {
    NewSchedule availSchedule = 
      (NewSchedule)receivingAsset.getRoleSchedule().getAvailableSchedule();

    if (availSchedule == null) {
      availSchedule = ldmf.newAssignedAvailabilitySchedule();
      ((NewRoleSchedule)receivingAsset.getRoleSchedule()).setAvailableSchedule(availSchedule);
    } else {
      // Remove an existing entrie which refer to the receiving asset
      synchronized (availSchedule) {
        Collection remove = availSchedule.filter(new UnaryPredicate() {
          public boolean execute(Object o) {
            return ((o instanceof AssignedAvailabilityElement) &&
                    (((AssignedAvailabilityElement)o).getAssignee().equals(transferringAsset)));
          }
        });
        availSchedule.removeAll(remove);
      } // end synchronization
    }
      
    //Construct aggregate avail info from the relationship schedule
    RelationshipSchedule relationshipSchedule = 
      ((HasRelationships)receivingAsset).getRelationshipSchedule();
    Collection collection =  
      relationshipSchedule.getMatchingRelationships((HasRelationships)transferringAsset,
                                                    ETERNITY);
    
    // If any relationships, add a single avail element with the 
    // min start and max end
    if (collection.size() > 0) {
      Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
      availSchedule.add(ldmf.newAssignedAvailabilityElement(transferringAsset,
                                                            schedule.getStartTime(),
                                                            schedule.getEndTime()));
    }
  }

  private void removeExistingRelationships(AssetTransfer at,
                                           HasRelationships transferringAsset,
                                           HasRelationships receivingAsset) {

    RelationshipSchedule receivingSchedule = 
      receivingAsset.getRelationshipSchedule();
    RelationshipSchedule transferringSchedule = 
      transferringAsset.getRelationshipSchedule();
    
    RelationshipSchedule atRelationshipSchedule = 
      ((HasRelationships)at.getAsset()).getRelationshipSchedule();
    Collection atRelationships = new ArrayList(atRelationshipSchedule);
    
    for (Iterator atIterator = atRelationships.iterator();
         atIterator.hasNext();) {
      Relationship relationship = (Relationship) atIterator.next();
      
      Role role = (relationship.getA().equals(receivingAsset)) ?
        relationship.getRoleA() : relationship.getRoleB();
      
      Collection remove = 
        transferringSchedule.getMatchingRelationships(role,
                                                      receivingAsset,
                                                      ETERNITY);
      transferringSchedule.removeAll(remove);
      
      role = (relationship.getA().equals(transferringAsset)) ?
        relationship.getRoleA() :relationship.getRoleB();
      remove = 
        receivingSchedule.getMatchingRelationships(role,
                                                   transferringAsset,
                                                   ETERNITY);
      receivingSchedule.removeAll(remove);
    } 
  }

  protected Collection convertToLocalRelationships(AssetTransfer at,
                                                   Asset localTransferringAsset,
                                                   Asset receivingAsset) {
    RelationshipSchedule atRelationshipSchedule = 
      ((HasRelationships)at.getAsset()).getRelationshipSchedule();
    Collection atRelationships = new ArrayList(atRelationshipSchedule);

    ArrayList localRelationships = new ArrayList(atRelationships.size());

    for (Iterator iterator = atRelationships.iterator(); 
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
      localRelationships.add(localRelationship);
    }

    return localRelationships;
  }

  // Clear relationship, role and availble schedules to ensure that there 
  // are no dangling references to other organizations.
  private void clearSchedule(Asset asset) {
    if (related(asset)) {
      RelationshipSchedule relationshipSchedule = 
        ((HasRelationships )asset).getRelationshipSchedule();
      relationshipSchedule = null;
    }

    if (asset.getRoleSchedule() != null) {
      asset.getRoleSchedule().clear();

      if (asset.getRoleSchedule().getAvailableSchedule() != null) {
        asset.getRoleSchedule().getAvailableSchedule().clear();
      }
    }
  }
}










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

import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogPlanLogicProvider;
import org.cougaar.core.domain.RestartLogicProvider;
import org.cougaar.core.domain.RestartLogicProviderHelper;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ClusterPG;

import org.cougaar.planning.ldm.plan.AssetAssignment;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.NewAssetAssignment;
import org.cougaar.planning.ldm.plan.NewAssetVerification;
import org.cougaar.planning.ldm.plan.NewRelationshipSchedule;
import org.cougaar.planning.ldm.plan.NewRoleSchedule;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.RelationshipScheduleImpl;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;

import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

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
  private static Logger logger = Logging.getLogger(AssetTransferLP.class);
  private static TimeSpan ETERNITY = new MutableTimeSpan();
  private final ClusterIdentifier self;

  public AssetTransferLP(LogPlanServesLogicProvider logplan,
                         ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
    self = cluster.getClusterIdentifier();
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
      if (!sendRelationships && 
          ((changes != AnonymousChangeReport.LIST) &&
           (changes != null))) {
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
        if (logger.isDebugEnabled()) logger.debug("Sending " + assetassign);
        logplan.sendDirective(assetassign);
      } else {
        if (logger.isDebugEnabled()) logger.debug("Not sending AssetAssignment for " + at);
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
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof AssetTransfer) {
          AssetTransfer at = (AssetTransfer) o;
          ClusterIdentifier assignee = 
            at.getAssignee().getClusterPG().getClusterIdentifier();
          return 
            RestartLogicProviderHelper.matchesRestart(
                self, cid, assignee);
        }
        return false;
      }
    };
    Enumeration enum = logplan.searchBlackboard(pred);
    while (enum.hasMoreElements()) {
      AssetTransfer at = (AssetTransfer) enum.nextElement();
      logplan.sendDirective(createAssetAssignment(at, AssetAssignment.REPEAT, true));
    }
    pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Asset) {
          Asset asset = (Asset) o;
          ClusterPG clusterPG = asset.getClusterPG();
          if (clusterPG != null) {
            ClusterIdentifier assetCID = clusterPG.getClusterIdentifier();
            return
              RestartLogicProviderHelper.matchesRestart(
                  self, cid, assetCID);
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
          nav.setSource(self);
          nav.setDestination(asset.getClusterPG().getClusterIdentifier());
          logplan.sendDirective(nav);
        }
      } else {
        // BOZO - we have not tested transferring non-org assets
        logger.error("AssetTransferLP - unable to verify transfer of " +
                           asset + "\n.");
      }
        
    }
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

      }
    } else {
      s = ldmf.newSchedule(at.getSchedule().getAllScheduleElements());
    }

    naa.setSchedule(s);

    // Ensure that local info reflects the transfer
    if (!updateLocalAssets(at, kind, s)) {
      return null;
    }

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
          logger.error("AssetTransferLP: Relationships on the " + 
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

  private boolean updateLocalAssets(AssetTransfer at, byte kind, Schedule aaSchedule) {
    Asset localTransferringAsset = logplan.findAsset(at.getAsset());
    if (localTransferringAsset == null) {
      logger.error("AssetTransferLP: unable to process AssetTransfer - " + 
                         at.getAsset() + " - transferring to " + 
                         at.getAssignee()+ " - is not local to this cluster.");
      return false;
    } else if (localTransferringAsset == at.getAsset()) {
      logger.error("AssetTransferLP: Assets in AssetTransfer must be " +
                         " clones. AssetTransfer - " + at.getUID() + 
                         " - references assets in the log plan.");
    }
    
    Asset receivingAsset = at.getAssignee();
    Asset localReceivingAsset = logplan.findAsset(receivingAsset);

    if (localReceivingAsset == null) {
      receivingAsset = ldmf.cloneInstance(receivingAsset);
      if (related(receivingAsset)){
        ((HasRelationships)receivingAsset).setRelationshipSchedule(ldmf.newRelationshipSchedule((HasRelationships)receivingAsset));
      }
    } else {
        receivingAsset = localReceivingAsset;

      if (localReceivingAsset == at.getAssignee()) {
        logger.error("AssetTransferLP: Assets in AssetTransfer must be " +
                           " clones. AssetTransfer - " + at.getUID() + 
                           " - references assets in the log plan.");
      }
    }

    if (related(localTransferringAsset) && related(receivingAsset)) {
      fixRelationshipSchedule(at, kind, localTransferringAsset, receivingAsset);
    } 


    fixAvailSchedule(receivingAsset, localTransferringAsset, kind, aaSchedule);
    
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
                                final Asset transferringAsset,
                                byte kind,
                                Schedule aaSchedule) {
    NewSchedule availSchedule = 
      (NewSchedule)transferringAsset.getRoleSchedule().getAvailableSchedule();

    if (availSchedule == null) {
      availSchedule = ldmf.newAssignedAvailabilitySchedule();
      ((NewRoleSchedule)transferringAsset.getRoleSchedule()).setAvailableSchedule(availSchedule);
    } 

    synchronized (availSchedule) {

    if ((kind == AssetAssignment.UPDATE) ||
        (kind == AssetAssignment.REPEAT) ||
        (related(transferringAsset) && related(receivingAsset))) {
        // Remove an existing entries which refer to the receiving asset
        Collection remove = availSchedule.filter(new UnaryPredicate() {
          public boolean execute(Object o) {
            return ((o instanceof AssignedAvailabilityElement) &&
                    (((AssignedAvailabilityElement)o).getAssignee().equals(receivingAsset)));
          }
        });
        availSchedule.removeAll(remove);
      } 

      if (related(transferringAsset) && related(receivingAsset)) {
        //Construct aggregate avail info from the relationship schedule
        RelationshipSchedule relationshipSchedule = 
          ((HasRelationships)transferringAsset).getRelationshipSchedule();
        Collection collection =  
          relationshipSchedule.getMatchingRelationships((HasRelationships)receivingAsset,
                                                        ETERNITY);
        
        // If any relationships, add a single avail element with the 
        // min start and max end
        if (collection.size() > 0) {
          Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
          availSchedule.add(ldmf.newAssignedAvailabilityElement(receivingAsset,
                                                                schedule.getStartTime(),
                                                                schedule.getEndTime()));
        }
      } else {
        //Copy availability info directly from aa schedule

        //Don't iterate over schedule directly because Schedule doesn't support
        //iterator().
        Iterator iterator = new ArrayList(aaSchedule).iterator();
        while (iterator.hasNext()) {
          ScheduleElement avail = (ScheduleElement)iterator.next();
          availSchedule.add(ldmf.newAssignedAvailabilityElement(receivingAsset, 
                                                                avail.getStartTime(),
                                                                avail.getEndTime()));
        }
      }
    } // end sync block
  }


  private void fixRelationshipSchedule(AssetTransfer at,
                                       int kind,
                                       Asset transferringAsset, 
                                       Asset receivingAsset) {
    if ((kind == AssetAssignment.UPDATE) ||
        (kind == AssetAssignment.REPEAT)) {
      //Remove existing relationships
      removeExistingRelationships(at, 
                                  (HasRelationships)transferringAsset,
                                  (HasRelationships)receivingAsset);
    }

    // Add transfer relationships to local assets
    Collection localRelationships = 
      convertToLocalRelationships(at,
                                  transferringAsset,
                                  receivingAsset);

    RelationshipSchedule transferringSchedule = 
      ((HasRelationships)transferringAsset).getRelationshipSchedule();
    transferringSchedule.addAll(localRelationships);

    RelationshipSchedule receivingSchedule =
      ((HasRelationships)receivingAsset).getRelationshipSchedule();
    receivingSchedule.addAll(localRelationships);
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
      ((HasRelationships ) asset).setRelationshipSchedule(null);
    }

    if (asset.getRoleSchedule() != null) {
      asset.getRoleSchedule().clear();

      if (asset.getRoleSchedule().getAvailableSchedule() != null) {
        asset.getRoleSchedule().getAvailableSchedule().clear();
      }
    }
  }

}










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


package org.cougaar.domain.planning.ldm.lps;

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.cluster.ClusterServesLogicProvider;
import org.cougaar.core.cluster.LogPlanLogicProvider;
import org.cougaar.core.cluster.LogPlanServesLogicProvider;
import org.cougaar.core.cluster.MessageLogicProvider;

import org.cougaar.domain.planning.ldm.asset.Asset;

import org.cougaar.domain.planning.ldm.plan.AssetVerification;
import org.cougaar.domain.planning.ldm.plan.AssetRescind;
import org.cougaar.domain.planning.ldm.plan.AssignedRelationshipElement;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.HasRelationships;
import org.cougaar.domain.planning.ldm.plan.NewSchedule;
import org.cougaar.domain.planning.ldm.plan.Relationship;
import org.cougaar.domain.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.domain.planning.ldm.plan.Schedule;

import org.cougaar.core.society.UID;

import org.cougaar.util.TimeSpan;

/**
  * Sample LogicProvider for use by ClusterDispatcher to
  * take an incoming AssetAssignment Directive and
  * add Asset to the LogPlan w/side-effect of also disseminating to
  * other subscribers.
  **/

public class ReceiveAssetVerificationLP
  extends LogPlanLogicProvider
  implements MessageLogicProvider
{
  public ReceiveAssetVerificationLP(LogPlanServesLogicProvider logplan,
                                    ClusterServesLogicProvider cluster) {
    super(logplan, cluster);
  }

  /**
   * Verifies that Assets are in the LogPlan. Sends rescinds if not.
   **/
  public void execute(Directive dir, Collection changes) {
    if (dir instanceof AssetVerification) {
      AssetVerification av  = (AssetVerification) dir;

      if (!(av.getAsset() instanceof HasRelationships) ||
          !(av.getAssignee() instanceof HasRelationships)) {
        System.err.println("ReceiveAssetVerificationLP: can't verify " + 
                           " assignment of asset - " + av.getAsset() + 
                           " to " + av.getAssignee() + 
                           " without relationships");
        return;
      } 

      // One of ourselves
      HasRelationships localAsset = 
        (HasRelationships) logplan.findAsset(av.getAsset());
      Schedule rescindSchedule = null;

      if (localAsset == null) {
        //Rescind everything
        rescindSchedule = av.getSchedule();
      } else {
        rescindSchedule = getUnmatched(av, localAsset);
      }
        
      if ((rescindSchedule != null) &&
          rescindSchedule.size() > 0) {
        AssetRescind arm = 
          ldmf.newAssetRescind(ldmf.cloneInstance(av.getAsset()), 
                               ldmf.cloneInstance(av.getAssignee()),
                               rescindSchedule);
        logplan.sendDirective((Directive) arm); 
      }
    } 
  }
  
  /** getUnmatched - returns a Schedule with all AssignedRelationshipElements which
   * are not matched by the local information
   *
   * @param av AssetVerification which schedule is verified
   * @param localAsset HasRelationships local copy of the asset being verified
   * @return Schedule of AssignedRelationshipElements which do not match local info
   */
  private Schedule getUnmatched(AssetVerification av, 
                                HasRelationships localAsset) {
    NewSchedule rescindSchedule = null;
    HasRelationships asset = (HasRelationships)av.getAsset();
    HasRelationships assignee = (HasRelationships)av.getAssignee();
    
    Collection localRelationships = 
      localAsset.getRelationshipSchedule()
      .getMatchingRelationships(assignee,
                                av.getSchedule().getStartTime(),
                                av.getSchedule().getEndTime());
    String assetID = av.getAsset().getItemIdentificationPG().getItemIdentification();
    String assigneeID = av.getAssignee().getItemIdentificationPG().getItemIdentification();
    
    //iterate over verification schedule and make sure it maps to local info
    for (Iterator iterator = av.getSchedule().iterator();
         iterator.hasNext();) {
      AssignedRelationshipElement verify = (AssignedRelationshipElement)iterator.next();
      String itemIDA = verify.getItemIDA();
      String itemIDB = verify.getItemIDB();
      
      if (!(itemIDA.equals(assetID) &&
            itemIDB.equals(assigneeID)) &&
          !(itemIDA.equals(assigneeID) &&
            itemIDB.equals(assetID))) {
        System.err.println("ReceiveAssetVerificationLP: schedule element - " 
                           + verify + 
                           " does not match asset - " +
                           av.getAsset() + " - and assignee - " + 
                           av.getAssignee()); 
        continue;
      }
      
      Iterator localIterator = localRelationships.iterator();
      
      boolean found = false;
      while (localIterator.hasNext()) {
        Relationship localRelationship = 
          (Relationship)localIterator.next();
        HasRelationships localA = localRelationship.getA();
        HasRelationships localB = localRelationship.getB();
        
        if ((localA instanceof Asset) &&
            (localB instanceof Asset)) {
          String localItemIDA = 
            ((Asset)localA).getItemIdentificationPG().getItemIdentification();
          String localItemIDB = 
            ((Asset)localB).getItemIdentificationPG().getItemIdentification();
          
          if (((verify.getRoleA().equals(localRelationship.getRoleA()) &&
                itemIDA.equals(localItemIDA) &&
                verify.getRoleB().equals(localRelationship.getRoleB()) &&
                itemIDB.equals(localItemIDB)) ||
               (verify.getRoleB().equals(localRelationship.getRoleA()) &&
                itemIDB.equals(localItemIDA) &&
                verify.getRoleA().equals(localRelationship.getRoleB()) &&
                itemIDA.equals(localItemIDB))) &&
              verify.getStartTime() == localRelationship.getStartTime() &&
              verify.getEndTime() == localRelationship.getEndTime()) {
            found = true;
            break;
          }
        }
      }
      
      if (!found) {
        if (rescindSchedule == null) {
          rescindSchedule = 
            ldmf.newAssignedRelationshipSchedule();
        }
        ((NewSchedule)rescindSchedule).add(verify);
      }
    }

    return rescindSchedule;
  }
    
}





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

package org.cougaar.core.domain;

import java.util.*;
import org.cougaar.core.domain.LDMServesPlugin;
import org.cougaar.core.blackboard.LogPlan;
import org.cougaar.core.blackboard.XPlanServesBlackboard;
import org.cougaar.core.blackboard.BlackboardServesLogicProvider;
import org.cougaar.core.blackboard.LogPlanServesLogicProvider;
import org.cougaar.core.agent.ClusterServesLogicProvider;

import org.cougaar.planning.ldm.lps.*;

/**
 * This is the "root" or infrastructure domain, defining the
 * objects and behavior shared by all COUGAAR-based systems.
 **/

public class RootDomain 
  implements Domain
{
  /**
   * construct an LDM factory to serve the specified LDM instance.
   **/
  public Factory getFactory(LDMServesPlugin ldm) {
    return new RootFactory(ldm, ldm.getClusterIdentifier());
  }

  /** initialize Domain. Called once on a new instance immediately
   * after creating the Domain instance via the zero-argument constructor.
   **/
  public void initialize() {
  }

  public XPlanServesBlackboard createXPlan(Collection existingXPlans) {
    for (Iterator plans = existingXPlans.iterator(); plans.hasNext(); ) {
      XPlanServesBlackboard xPlan = (XPlanServesBlackboard) plans.next();
      if (xPlan instanceof LogPlan) return xPlan;
    }
    return new LogPlan();
  }

  /**
   * Create new Domain-specific LogicProviders for loading into the LogPlan.
   * @return a Collection of the LogicProvider instances or null.
   **/
  public Collection createLogicProviders(BlackboardServesLogicProvider alpplan, 
                                         ClusterServesLogicProvider cluster) 
  {
    ArrayList l = new ArrayList(15); // don't let this be too small.
    LogPlanServesLogicProvider logplan = (LogPlanServesLogicProvider) alpplan;

    l.add(new org.cougaar.core.relay.RelayLP(logplan, cluster));

    // MessageLPs
    l.add(new ReceiveAssetLP(logplan, cluster));
    l.add(new ReceiveAssetVerificationLP(logplan, cluster));
    l.add(new ReceiveAssetRescindLP(logplan, cluster));
    l.add(new ReceiveNotificationLP(logplan, cluster));
    l.add(new ReceiveDeletionLP(logplan, cluster));
    l.add(new ReceiveRescindLP(logplan, cluster));
    l.add(new ReceiveTaskLP(logplan, cluster));
    //l.add(new AggregationPublishLP(logplan, cluster));

    // envelopeLPs
    l.add(new AssetTransferLP(logplan, cluster));    
    l.add(new NotificationLP(logplan, cluster));
    l.add(new DeletionLP(logplan, cluster));
    l.add(new RemoteClusterAllocationLP(logplan, cluster));
    l.add(new PreferenceChangeLP(logplan, cluster));
    l.add(new RescindLP(logplan, cluster));
    // error detection LP
    l.add(new ComplainingLP(logplan, cluster));

    // should be done by expander plugins now
    //l.add(new WorkflowAllocationLP(logplan, cluster));
    // turn off for now because it is a cpu hog (and isn't used). 
    //l.add(new RoleScheduleConflictLP(logplan, cluster));
    return l;
  }
}

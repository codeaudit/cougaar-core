/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm;

import java.util.*;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.core.cluster.LogPlan;
import org.cougaar.core.cluster.XPlanServesALPPlan;
import org.cougaar.core.cluster.ALPPlanServesLogicProvider;
import org.cougaar.core.cluster.LogPlanServesLogicProvider;
import org.cougaar.core.cluster.ClusterServesLogicProvider;

import org.cougaar.domain.planning.ldm.lps.*;

/**
 * This is the "root" or infrastructure domain, defining the
 * objects and behavior shared by all ALP-based systems.
 **/

public class RootDomain 
  implements Domain
{
  /**
   * construct an LDM factory to serve the specified LDM instance.
   **/
  public Factory getFactory(LDMServesPlugIn ldm) {
    return new RootFactory(ldm, ldm.getClusterIdentifier());
  }

  /** initialize Domain. Called once on a new instance immediately
   * after creating the Domain instance via the zero-argument constructor.
   **/
  public void initialize() {
  }

  public XPlanServesALPPlan createXPlan(Collection existingXPlans) {
    for (Iterator plans = existingXPlans.iterator(); plans.hasNext(); ) {
      XPlanServesALPPlan xPlan = (XPlanServesALPPlan) plans.next();
      if (xPlan instanceof LogPlan) return xPlan;
    }
    return new LogPlan();
  }

  /**
   * Create new Domain-specific LogicProviders for loading into the LogPlan.
   * @return a Collection of the LogicProvider instances or null.
   **/
  public Collection createLogicProviders(ALPPlanServesLogicProvider alpplan, 
                                         ClusterServesLogicProvider cluster) 
  {
    ArrayList l = new ArrayList(14); // don't let this be too small.
    LogPlanServesLogicProvider logplan = (LogPlanServesLogicProvider) alpplan;

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

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

import org.cougaar.core.agent.ClusterServesLogicProvider;

import org.cougaar.core.blackboard.LogPlan;
import org.cougaar.core.blackboard.XPlanServesBlackboard;

import org.cougaar.core.component.BindingSite;

import org.cougaar.core.relay.RelayLP;

import org.cougaar.planning.ldm.lps.*;

/**
 * This is the "root" or infrastructure domain, defining the
 * objects and behavior shared by all COUGAAR-based systems.
 **/

public class RootDomain extends DomainAdapter {
  public static final String ROOT_NAME = "root";

  public String getDomainName() {
    return ROOT_NAME;
  }

  public void load() {
    super.load();
  }

  protected void loadFactory() {
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException("Binding site for the domain has not be set.\n" +
                             "Unable to initialize domain Factory without a binding site.");
    } 

    setFactory(new RootFactory(bindingSite.getClusterServesLogicProvider().getLDM()));
  }

  protected void loadXPlan() {
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException("Binding site for the domain has not be set.\n" +
                             "Unable to initialize domain XPlan without a binding site.");
    } 

    Collection xPlans = bindingSite.getXPlans();
    LogPlan logPlan = null;
    
    for (Iterator iterator = xPlans.iterator(); iterator.hasNext();) {
      XPlanServesBlackboard  xPlan = (XPlanServesBlackboard) iterator.next();
      if (xPlan instanceof LogPlan) {
        // Note that this means there are 2 paths to the plan.
        // Is this okay?
        logPlan = (LogPlan) logPlan;
        break;
      }
    }
    
    if (logPlan == null) {
      logPlan = new LogPlan();
    }
    
    setXPlan(logPlan);
  }

  protected void loadLPs() {
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException("Binding site for the domain has not be set.\n" +
                             "Unable to initialize domain LPs without a binding site.");
    } 

    ClusterServesLogicProvider cluster = bindingSite.getClusterServesLogicProvider();
    LogPlan logPlan = (LogPlan) getXPlan();
    
    addLogicProvider(new ReceiveAssetLP(logPlan, cluster));
    addLogicProvider(new ReceiveAssetVerificationLP(logPlan, cluster));
    addLogicProvider(new ReceiveAssetRescindLP(logPlan, cluster));
    addLogicProvider(new ReceiveNotificationLP(logPlan, cluster));
    addLogicProvider(new ReceiveDeletionLP(logPlan, cluster));
    addLogicProvider(new ReceiveRescindLP(logPlan, cluster));
    addLogicProvider(new ReceiveTaskLP(logPlan, cluster));
    
    // envelopeLPs
    addLogicProvider(new AssetTransferLP(logPlan, cluster));    
    addLogicProvider(new NotificationLP(logPlan, cluster));
    addLogicProvider(new DeletionLP(logPlan, cluster));
    addLogicProvider(new RemoteClusterAllocationLP(logPlan, cluster));
    addLogicProvider(new PreferenceChangeLP(logPlan, cluster));
    addLogicProvider(new RescindLP(logPlan, cluster));
    
    // error detection LP
    addLogicProvider(new ComplainingLP(logPlan, cluster));
    
    addLogicProvider(new RelayLP(logPlan, cluster));
  }
}









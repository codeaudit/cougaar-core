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

package org.cougaar.planning.ldm;

import java.util.*;
import java.util.Iterator;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.agent.ClusterServesLogicProvider;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.domain.*;
import org.cougaar.core.relay.RelayLP;
import org.cougaar.planning.ldm.lps.AssetTransferLP;
import org.cougaar.planning.ldm.lps.ComplainingLP;
import org.cougaar.planning.ldm.lps.DeletionLP;
import org.cougaar.planning.ldm.lps.NotificationLP;
import org.cougaar.planning.ldm.lps.PreferenceChangeLP;
import org.cougaar.planning.ldm.lps.ReceiveAssetLP;
import org.cougaar.planning.ldm.lps.ReceiveAssetRescindLP;
import org.cougaar.planning.ldm.lps.ReceiveAssetVerificationLP;
import org.cougaar.planning.ldm.lps.ReceiveDeletionLP;
import org.cougaar.planning.ldm.lps.ReceiveNotificationLP;
import org.cougaar.planning.ldm.lps.ReceiveRescindLP;
import org.cougaar.planning.ldm.lps.ReceiveTaskLP;
import org.cougaar.planning.ldm.lps.RemoteClusterAllocationLP;
import org.cougaar.planning.ldm.lps.RescindLP;

/**
 * This is the "planning" domain, which defines planning
 * data types (Task, PlanElement, etc) and related LPs.
 */
public class PlanningDomain extends DomainAdapter {
  public static final String PLANNING_NAME = "planning";

  private RootPlan rootplan;

  public String getDomainName() {
    return PLANNING_NAME;
  }

  public void load() {
    super.load();
  }

  public Collection getAliases() {
    ArrayList l = new ArrayList(2);
    l.add("planning");
    l.add("log");
    return l;
  }

  protected void loadFactory() {
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException("Binding site for the domain has not be set.\n" +
                             "Unable to initialize domain Factory without a binding site.");
    } 

    LDMServesPlugin ldm = bindingSite.getClusterServesLogicProvider().getLDM();
    Factory f = new PlanningFactoryImpl(ldm);
    setFactory(f);
  }

  protected void loadXPlan() {
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException(
          "Binding site for the domain has not be set.\n" +
          "Unable to initialize domain XPlan without a binding site.");
    } 

    Collection xPlans = bindingSite.getXPlans();
    LogPlan logplan = null;

    Iterator iterator = xPlans.iterator();
    while (true) {
      if (!iterator.hasNext()) {
        if (rootplan == null) {
          throw new RuntimeException(
              "\""+getDomainName()+
              "\" unable to find required RootPlan!");
        }
        if (logplan == null) {
          // typical case:
          logplan = new LogPlanImpl();
        }
        break;
      }
      XPlan xPlan = (XPlan) iterator.next();
      if (xPlan instanceof RootPlan) {
        rootplan = (RootPlan) xPlan;
      }
      if (xPlan instanceof LogPlan) {
        // already loaded?
        logplan = (LogPlan) logplan;
        break;
      }
    }
    
    setXPlan(logplan);
  }

  protected void loadLPs() {
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException("Binding site for the domain has not be set.\n" +
                             "Unable to initialize domain LPs without a binding site.");
    } 

    ClusterServesLogicProvider cluster = bindingSite.getClusterServesLogicProvider();
    // already holding onto rootplan
    LogPlan logplan = (LogPlan) getXPlan();
    PlanningFactory ldmf = (PlanningFactory) getFactory();
    MessageAddress self = cluster.getMessageAddress();

    // input LPs
    addLogicProvider(new ReceiveAssetLP(rootplan, logplan, ldmf, self));
    addLogicProvider(new ReceiveAssetVerificationLP(rootplan, logplan, ldmf));
    addLogicProvider(new ReceiveAssetRescindLP(rootplan, logplan, ldmf));
    addLogicProvider(new ReceiveNotificationLP(rootplan, logplan, ldmf));
    addLogicProvider(new ReceiveDeletionLP(logplan));
    addLogicProvider(new ReceiveRescindLP(rootplan, logplan));
    addLogicProvider(new ReceiveTaskLP(rootplan, logplan));
    
    // output LPs (+ some input)
    addLogicProvider(new AssetTransferLP(rootplan, logplan, ldmf, self));    
    addLogicProvider(new NotificationLP(rootplan, logplan, ldmf, self));
    addLogicProvider(new DeletionLP(rootplan, ldmf, self));
    addLogicProvider(new RemoteClusterAllocationLP(rootplan, ldmf, self));
    addLogicProvider(new PreferenceChangeLP(rootplan));
    addLogicProvider(new RescindLP(rootplan, logplan, ldmf));
    
    // error detection LP
    addLogicProvider(new ComplainingLP(rootplan, self));
  }
}

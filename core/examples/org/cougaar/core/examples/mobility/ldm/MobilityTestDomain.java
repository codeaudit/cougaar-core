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

package org.cougaar.core.examples.mobility.ldm;

import java.util.*;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.agent.ClusterServesLogicProvider;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.LogPlan;
import org.cougaar.core.blackboard.XPlanServesBlackboard;
import org.cougaar.core.domain.Domain;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.domain.LDMServesPlugin;

import org.cougaar.core.domain.DomainAdapter;
import org.cougaar.core.domain.DomainBindingSite;

/**
 * The mobility test domain ("mobilityTest") just has a 
 * factory, with no LPs.
 */
public class MobilityTestDomain extends DomainAdapter {

  private static final String MOBILTY_TEST_NAME = "mobilityTest";

  public String getDomainName() {
    return MOBILTY_TEST_NAME;
  }

  public Collection getAliases() {
    return Collections.singleton(getDomainName());
  }

  protected void loadFactory() {
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException(
          "Binding site for the domain has not be set.\n" +
          "Unable to initialize domain Factory without a binding site.");
    } 

    LDMServesPlugin ldm = bindingSite.getClusterServesLogicProvider().getLDM();
    Factory f = new MobilityTestFactoryImpl(ldm);
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

  // zero LPs
  protected void loadLPs() {
  }
  public void invokeMessageLogicProviders(DirectiveMessage message) {
    return;
  }
  public void invokeEnvelopeLogicProviders(
      EnvelopeTuple tuple, boolean isPersistenceEnvelope) {
    return;
  }
  public void invokeRestartLogicProviders(MessageAddress cid) {
    return;
  }

}

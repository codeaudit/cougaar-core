/*
 * <copyright>
 *  Copyright 2001-2002 BBNT Solutions, LLC
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
package org.cougaar.multicast;

import org.cougaar.core.agent.ClusterServesLogicProvider;
import org.cougaar.core.blackboard.LogPlan;
import org.cougaar.core.blackboard.BlackboardServesLogicProvider;
import org.cougaar.core.blackboard.LogPlanServesLogicProvider;
import org.cougaar.core.blackboard.XPlanServesBlackboard;
import org.cougaar.core.domain.Domain;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.domain.LDMServesPlugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;


/**
 * Domain created and used in the <code>ABM</code>messgaging framework. 
 * Allows access to the logplan and creates the one Logic Provider the ABM API 
 * uses.<br> 
 * 
 * Load this domain with <code>-Dorg.cougaar.domain.abm=org.cougaar.multicast.ABMDomain</code>
 *
 * @see ABMFactory
 **/
public class ABMDomain implements Domain {
  public ABMDomain() { }
  
  /**
   * Create the ABMFactory for creating ABMs and things
   * @return the ABMFactory instance
   **/
  public Factory getFactory(LDMServesPlugin ldm) {
    return new ABMFactory(ldm);
  }
  
  /**
   * ABM has no Domain initialization as of yet
   **/
  public void initialize() {
  }

  /**
   * Return the basic LogPlan
   * @return the <code>LogPlan</code>
   **/
  public XPlanServesBlackboard createXPlan(Collection existingXPlans) {

    for (Iterator plans = existingXPlans.iterator(); plans.hasNext(); ) {
      XPlanServesBlackboard xPlan = (XPlanServesBlackboard) plans.next();
      if (xPlan != null) return xPlan;
    }
    
    return new LogPlan();
  }  

  /**
   * ABM has one LogicProvider - <code>ABMTransportLP</code>.
   * @return a Collection of the ABM LogicProviders or null
   * @see org.cougaar.core.domain.Domain
   **/
  public Collection createLogicProviders(BlackboardServesLogicProvider logplan,
                                         ClusterServesLogicProvider cluster) {
      ArrayList l = new ArrayList(1);
      l.add(new ABMTransportLP((LogPlanServesLogicProvider)logplan, cluster));
      return l;
  }

} // end of ABMDomain.java



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

import java.util.Collection;

import org.cougaar.core.domain.LDMServesPlugIn;
import org.cougaar.core.blackboard.BlackboardServesLogicProvider;
import org.cougaar.core.blackboard.XPlanServesBlackboard;
import org.cougaar.core.agent.ClusterServesLogicProvider;

/**
 * Describe an COUGAAR "Pluggable Domain Package" which consists of
 * a set of domain-specific LDM objects as represented by a 
 * Factory class, and a set of LogicProviders.
 *
 * Domain classes must also implement a static
 * create() method so that they can be constructed
 * by the infrastructure.
 *
 * Domains may optionally implement <pre>Collection<String> getAliases()</pre>
 * to present alias names to the domain manager.  This feature is likely
 * to be removed in short order, as it is only to allow backward-compatability
 * when domain names change.  Use of domain aliases may result in warnings.
 **/

public interface Domain 
{
  /**
   * construct an LDM factory to serve the specified LDM instance.
   **/
  Factory getFactory(LDMServesPlugIn ldm);

  /** initialize Domain. Called once on a new instance immediately
   * after creating the Domain instance via the zero-argument constructor,
   * but before the DomainManager adds the Domain to the domain list.
   **/
  void initialize();

  /**
   * Create new Domain-specific LogicProviders for loading into the LogPlan.
   * @return a Collection of the LogicProvider instances or null.
   **/
  Collection createLogicProviders(BlackboardServesLogicProvider logplan, 
                                  ClusterServesLogicProvider cluster);

  /**
   * Allow the domain to specify an XPlan for use by its LogicProviders.
   * This allows the domain's LPs to have custom collections/subscriptions
   * for efficiency rather than having to do slow searches every time.
   **/
  XPlanServesBlackboard createXPlan(java.util.Collection existingXPlans);

}

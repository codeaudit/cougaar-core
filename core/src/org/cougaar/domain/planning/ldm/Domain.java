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

import java.util.Collection;

import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.core.cluster.WhiteboardServesLogicProvider;
import org.cougaar.core.cluster.XPlanServesWhiteboard;
import org.cougaar.core.cluster.ClusterServesLogicProvider;

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
  Collection createLogicProviders(WhiteboardServesLogicProvider logplan, 
                                  ClusterServesLogicProvider cluster);

  /**
   * Allow the domain to specify an XPlan for use by its LogicProviders.
   * This allows the domain's LPs to have custom collections/subscriptions
   * for efficiency rather than having to do slow searches every time.
   **/
  XPlanServesWhiteboard createXPlan(java.util.Collection existingXPlans);

}

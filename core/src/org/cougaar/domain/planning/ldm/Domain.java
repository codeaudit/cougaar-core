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
 **/

public interface Domain 
{
  /**
   * construct an LDM factory to serve the specified LDM instance.
   **/
  Factory getFactory(LDMServesPlugIn ldm);

  /** initialize Domain. Called once on a new instance immediately
   * after creating the Domain instance via the zero-argument constructor.
   **/
  void initialize();

  /**
   * Create new Domain-specific LogicProviders for loading into the LogPlan.
   * @return a Collection of the LogicProvider instances or null.
   **/
  Collection createLogicProviders(WhiteboardServesLogicProvider logplan, 
                                  ClusterServesLogicProvider cluster);

  XPlanServesWhiteboard createXPlan(java.util.Collection existingXPlans);
}

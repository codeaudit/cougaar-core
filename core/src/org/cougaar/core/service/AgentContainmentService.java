/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.service;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.Service;

/** 
 * Agent's "containment service" that allows a child component
 * of the agent to interact with the agent's container (add, 
 * remove, etc).
 * <p>
 * For example, a plugin within an agent can add another 
 * brand-new plugin to its agent by using this service.
 * <p>
 * Each agent has its own containment service that only
 * applies to that agent's contents -- this service can not
 * be used to add a component to a different agent.
 */
public interface AgentContainmentService extends Service {

  boolean add(ComponentDescription desc);

  boolean remove(ComponentDescription desc);

  boolean contains(ComponentDescription desc);

  // add "list" enhancements here -- see bug 1113

}

/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.agent;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mts.MessageAddress;

/**
 * 
 */
public interface AgentContainer {

  /**
   * Add a new agent to the local node.
   *
   * @throws RuntimeException if the agent already exists, or 
   *    the agent can't be loaded.
   */
  void addAgent(MessageAddress agentId, StateTuple tuple);

  /**
   * Remove an agent that's on the local node.
   * <p>
   * Assumes that the agent has already been stopped 
   * and unloaded.
   * 
   * @throws RuntimeException if the agent is not on the
   *    local node, or it can't be removed.
   */
  void removeAgent(MessageAddress agentId);

  /**
   * Get the component description for an agent on the
   * local node.
   *
   * @return null if the agent is not on the local node,
   *    or the description is not known.
   */
  ComponentDescription getAgentDescription(
      MessageAddress agentId);

}

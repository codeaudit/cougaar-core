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

import java.util.Map;
import java.util.Set;
import java.util.List;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mts.MessageAddress;

/**
 * 
 */
public interface AgentContainer {

  /**
   * Equivalent to
   *   <code>(getAgentDescription(agentId) != null)</code>
   *
   * @return true if the agent is on the local node
   */
  boolean containsAgent(MessageAddress agentId);

  /**
   * Equivalent to
   *   <code>getLocalAgentDescriptions().keySet()</code>
   *
   * @return a Set of all local agent MessageAddresses
   */
  Set getAgentAddresses();

  /**
   * Equivalent to
   *   <code>getLocalAgentDescriptions().get(agentId)</code>
   *
   * @return null if the agent is not on the local node,
   *    or the description is not known.
   */
  ComponentDescription getAgentDescription(
      MessageAddress agentId);

  /**
   * Get an unmodifiable map of local agent MessageAddress to the 
   * ComponentDescriptions.
   *
   * @return a Map&lt;MessageAddress&gt;&lt;ComponentDescriptions&gt;
   *   for the local agents
   */
  Map getAgents();

  List getComponents();

  /**
   * Add a new agent to the local node.
   *
   * @throws RuntimeException if the agent already exists, or 
   *    the agent can't be loaded.
   */
  void addAgent(MessageAddress agentId, StateTuple tuple);

  /**
   * Add a component to this agent container. Only certain components
   * are allowed using this method. In particular, agents cannot be
   * added.
   **/
  boolean add(Object o);

  /**
   * Remove an agent that's on the local node.
   * 
   * @throws RuntimeException if the agent is not on the
   *    local node, or it can't be removed.
   */
  void removeAgent(MessageAddress agentId);

  /**
   * Remove component from this agent container. Only components added
   * with the add method can be removed this way. Agents cannot be
   * removed with this method.
   **/
  boolean remove(Object o);
}

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

package org.cougaar.core.agent;

import org.cougaar.core.mts.Message;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.node.NodeIdentifier;

import org.cougaar.core.blackboard.*;

import org.cougaar.core.agent.ClusterIdentifier;

/**
 * Tell a Node to move one of it's Agent to another Node.
 * <p>
 * The agent must reside on the <code>getTarget()</code> Node.
 */
public class MoveAgentMessage 
extends AgentManagementMessage 
{

  // agent to move
  private ClusterIdentifier agentID;

  // destination node
  private NodeIdentifier nodeID;

  public MoveAgentMessage(
      MessageAddress aSource, 
      MessageAddress aTarget,
      ClusterIdentifier agentID,
      NodeIdentifier nodeID) {
    super(aSource, aTarget);
    this.agentID = agentID;
    this.nodeID = nodeID;
  }

  /**
   * Get the <code>ClusterIdentifier</code> for the Agent to be moved.
   * <p>
   * The agent must reside on the <code>getTarget()</code> Node.
   */
  public ClusterIdentifier getAgentIdentifier() {
    return agentID;
  }

  /**
   * Get the <code>NodeIdentifier</code> of the destination Node for the
   * Agent.
   */
  public NodeIdentifier getNodeIdentifier() {
    return nodeID;
  }

  public String toString() {
    return 
      "Move Agent "+
      getAgentIdentifier()+
      " from Node "+
      getTarget()+
      " to Node "+
      getNodeIdentifier()+
      ", as ordered by "+
      getOriginator();
  }
}
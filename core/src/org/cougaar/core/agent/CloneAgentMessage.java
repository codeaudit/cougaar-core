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

import org.cougaar.core.blackboard.*;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentifier;

/**
 * Tell a Node to clone one of its Agent to another Node;
 * the new Agent name and whether or not to clone the
 * Blackboard is provided.
 * <p>
 * Typically the Blackboard is not cloned, since it may
 * contain data that's private or non-applicable for the 
 * clone.
 * <p>
 * The agent must reside on the <code>getTarget()</code> Node.
 */
public class CloneAgentMessage 
extends AgentManagementMessage 
{

  private ClusterIdentifier agentID;
  private NodeIdentifier nodeID;
  private final ClusterIdentifier cloneAgentID;
  private final boolean cloneBlackboard;

  public CloneAgentMessage(
      MessageAddress aSource,
      MessageAddress aTarget,
      ClusterIdentifier agentID,
      NodeIdentifier nodeID,
      ClusterIdentifier cloneAgentID,
      boolean cloneBlackboard) {
    super(aSource, aTarget);
    this.agentID = agentID;
    this.nodeID = nodeID;
    this.cloneAgentID = cloneAgentID;
    this.cloneBlackboard = cloneBlackboard;
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

  /**
   * Get the new <code>ClusterIdentifier</code> for the clone.
   */
  public ClusterIdentifier getCloneAgentIdentifier() { 
    return cloneAgentID; 
  }

  /**
   * Whether or not to clone the blackboard.
   */
  public boolean getCloneBlackboard() { 
    return cloneBlackboard; 
  }

  public String toString() {
    return
      "Clone Agent "+
      getAgentIdentifier()+
      " from Node "+
      getTarget()+
      " on Node "+
      getNodeIdentifier()+
      ", with the new name "+
      getCloneAgentIdentifier()+
      " and with"+
      (getCloneBlackboard() ? "" : "out")+
      " the blackboard"+
      ", as ordered by "+
      getOriginator();
  }
}

/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import org.cougaar.core.cluster.ClusterIdentifier;

/**
 * Tell a Node to move one of it's Agent to another Node.
 * <p>
 * The agent must reside on the <code>getTarget()</code> Node.
 */
public class MoveAgentMessage 
extends Message {

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

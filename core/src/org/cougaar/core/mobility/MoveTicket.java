/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.mobility;

import org.cougaar.core.mts.MessageAddress;

/**
 * A MoveTicket specifies the destination node and other parameters
 * of agent movement.
 * <p>
 * A ticket is immutable.
 * @see Ticket
 */
public class MoveTicket extends AbstractTicket implements java.io.Serializable {

  private final Object id;
  private final MessageAddress mobileAgent;
  private final MessageAddress origNode;
  private final MessageAddress destNode;
  private final boolean forceRestart;
  
  // FIXME maybe add "timeout" here
  // FIXME maybe add "clone" here + clone name
  // FIXME maybe add security tags here
  
  public MoveTicket(
      Object id,
      MessageAddress mobileAgent,
      MessageAddress origNode,
      MessageAddress destNode,
      boolean forceRestart) {
    this.id = id;
    this.mobileAgent = mobileAgent;
    this.origNode = origNode;
    this.destNode = destNode;
    this.forceRestart = forceRestart;
  }

  /**
   * An optional identifier for this ticket instance.
   * <p>
   * The identifier <u>must</u> be serializable and should be
   * immutable.   A UID is a good identifier.
   */
  public Object getIdentifier() {
    return id;
  }

  /**
   * The agent to be moved.
   * <p>
   * An agent can only pass a Ticket to "dispatch(..)" if
   * the agent <i>is</i> the one moving.  Aside from this
   * sanity check, tagging the ticket with the agent address
   * aids debugging.
   * <p>
   * If the agent is null then the caller of the 
   * MobilityService is assumed.
   */
  public MessageAddress getMobileAgent() {
    return mobileAgent;
  }

  /**
   * Optional assertion on the current node that the mobile agent 
   * is running on.
   * <p>
   * If the origin node is non-null then the ticket will only be 
   * accepted if the agent is on that node.  Of course, a ticket 
   * can only be presented to the MobilityService <i>by</i> 
   * the local agent, but this is a sanity check that the agent
   * wasn't moved or restarted after the ticket was created.
   */
  public MessageAddress getOriginNode() {
    return origNode;
  }

  /**
   * Destination node for the mobile agent.
   * <p>
   * If the destination node is null then the agent should 
   * remain at its current node.  This is is useful for
   * a "force-restart".
   */
  public MessageAddress getDestinationNode() {
    return destNode;
  }
  
  /**
   * If true, force the agent to be restarted even if the agent 
   * is already on the destination node.
   * <p>
   * If false then a move of an agent already at the destination 
   * will reply with a trivial success.  In practice one would 
   * tyically set this to <b>false</b> for performance reasons.
   * <p>
   * The "force reload" capability is primarily designed to test
   * the agent-side mobility requirements without actually 
   * relocating the agent.
   * <p>
   * If the agent and its components (plugins, services, etc) 
   * correctly implement the suspend and persistence APIs then 
   * the restart of an agent on the same node should have 
   * <i>no</i> permanent side effects.  A hundred restarts 
   * should have no side effect other than a temporary 
   * performance penalty.
   * <p>
   * On the other hand, a failure to support a restart might 
   * result in an agent:<ul>
   *  <li>memory leak (garbage-collection)</li>
   *  <li>thread leak (didn't stop/pool all theads)</li>
   *  <li>serialization/deserialization error</li>
   *  <li>state loss (some state not captured)</li>
   *  <li>internal deadlock (synchronization bug)</li>
   *  <li>persistence error</li>
   *  <li>naming-service mess</li>
   *  <li>crypto-key loss (unable to re-obtain identity)</li>
   *  <li>conflict with other services (health-check, etc)</li>
   * </ul>
   * or some other error that (ideally) should be easier to 
   * debug than the full relocation of the agent on another 
   * node.
   */
  public boolean isForceRestart() {
    return forceRestart;
  }

  public int hashCode() {
    return 
      (((id != null) ? id.hashCode() : 17) ^
       ((mobileAgent != null) ? mobileAgent.hashCode() : 53));
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof MoveTicket)) {
      return false;
    } else {
      MoveTicket t = (MoveTicket) o;
      return
        ((forceRestart == t.forceRestart) &&
         ((id == null) ? 
          (t.id == null) :
          (id.equals(t.id))) &&
         ((mobileAgent == null) ? 
          (t.mobileAgent == null) :
          (mobileAgent.equals(t.mobileAgent))) &&
         ((origNode == null) ? 
          (t.origNode == null) :
          (origNode.equals(t.origNode))) &&
         ((destNode == null) ? 
          (t.destNode == null) :
          (destNode.equals(t.destNode))));
    }
  }

  public String toString() {
    // cache?
    return 
      "Move "+
      ((id != null) ? 
       (id) :
       (" unidentified "))+
      " of "+
      ((mobileAgent != null) ? 
       "agent \""+mobileAgent+"\"" :
       "this agent")+
      " from "+
      ((origNode != null) ? 
       "node \""+origNode+"\"" :
       "this node")+
      " to "+
      ((destNode != null) ? 
       "node \""+destNode+"\"" :
       "this node")+
      (forceRestart ? " with forced restart" : "");
  }

  private static final long serialVersionUID = 3892837467898101093L;

  
}

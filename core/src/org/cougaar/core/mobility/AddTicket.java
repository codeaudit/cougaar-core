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
 * A ticket to add an agent.
 */
public final class AddTicket extends AbstractTicket implements java.io.Serializable {

  private final Object id;
  private final MessageAddress mobileAgent;
  private final MessageAddress destNode;

  // FIXME maybe add "timeout" here
  // FIXME maybe add "clone" here + clone name
  // FIXME maybe add security tags here

  public AddTicket(
      Object id,
      MessageAddress mobileAgent,
      MessageAddress destNode) {
    this.id = id;
    this.mobileAgent = mobileAgent;
    this.destNode = destNode;
  }

  /**
   * An optional identifier for this addticket instance.
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
   * An agent can only pass a AddTicket to "dispatch(..)" if
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
   * Destination node for the mobile agent.
   * <p>
   * If the destination node is null then the AgentControl status  
   * should be changed to FAILURE.
   */
  public MessageAddress getDestinationNode() {
    return destNode;
  }

  public int hashCode() {
    return 
      (((id != null) ? id.hashCode() : 17) ^
       ((mobileAgent != null) ? mobileAgent.hashCode() : 53));
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof AddTicket)) {
      return false;
    } else {
      AddTicket t = (AddTicket) o;
      return
	((id == null) ? 
	 (t.id == null) :
	 (id.equals(t.id))) &&
	((mobileAgent == null) ? 
	 (t.mobileAgent == null) :
	 (mobileAgent.equals(t.mobileAgent))) &&
	((destNode == null) ? 
	 (t.destNode == null) :
	 (destNode.equals(t.destNode)));
    }
  }
  
  public String toString() {
    // cache?
    return 
      "Add "+
      ((id != null) ? 
       (id) :
       (" unidentified "))+
      " of "+
      ((mobileAgent != null) ? 
       "agent \""+mobileAgent+"\"" :
       "this agent")+
      " to "+
      ((destNode != null) ? 
       "node \""+destNode+"\"" :
       "this node");
  }
}

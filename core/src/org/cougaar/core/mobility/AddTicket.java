/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

import java.io.Serializable;
import java.util.List;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.StateTuple;

/**
 * A ticket to add an agent.
 */
public final class AddTicket extends AbstractTicket implements java.io.Serializable {

  private static final String DEFAULT_AGENT_CLASSNAME =
    "org.cougaar.core.agent.SimpleAgent";

  private final Object id;
  private final MessageAddress mobileAgent;
  private final StateTuple state;
  private final MessageAddress destNode;

  // FIXME maybe add "timeout" here
  // FIXME maybe add "clone" here + clone name
  // FIXME maybe add security tags here

  /**
   * Add an agent with address <code>mobileAgent</code> and optional
   * state <code>state</code> to node <code>destNode</code>.
   * <p>
   * If the state is null, then no state and class "SimpleAgent"
   * is assumed.
   * <p>
   * The <code>id</code> should be obtained from the MobilityFactory
   * "createTicketIdentifier()".
   */
  public AddTicket(
      Object id,
      MessageAddress mobileAgent,
      StateTuple state,
      MessageAddress destNode) {
    this.id = id;
    this.mobileAgent = mobileAgent;
    this.destNode = destNode;
    if (mobileAgent == null) {
      throw new IllegalArgumentException(
          "Must specify an agent to add");
    }
    if (state == null) {
      ComponentDescription desc =
        new ComponentDescription(
            DEFAULT_AGENT_CLASSNAME,
            Agent.INSERTION_POINT,
            DEFAULT_AGENT_CLASSNAME,
            null,  // codebase
            mobileAgent,
            null,  // certificate
            null,  // lease
            null,  // policy
            ComponentDescription.PRIORITY_COMPONENT);
      state = new StateTuple(desc, null);
    } else {
      // validate the component description
      assertIsValid(
          mobileAgent,
          state.getComponentDescription());
    }
    this.state = state;
  }

  /** Equivalent to "AddTicket(id, mobileAgent, null, destNode)" */
  public AddTicket(
      Object id,
      MessageAddress mobileAgent,
      MessageAddress destNode) {
    this(id, mobileAgent, null, destNode);
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
   * Get the new agent's ComponentDescription and initial state.
   */
  public StateTuple getStateTuple() {
    return state;
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

  private static void assertIsValid(
      MessageAddress mobileAgent,
      ComponentDescription desc) {
    // check the insertion point
    String ip = desc.getInsertionPoint();
    if (!ip.equals(Agent.INSERTION_POINT)) {
      throw new IllegalArgumentException(
          "Insertion point must be \""+
          Agent.INSERTION_POINT+"\", not "+ip);
    }
    // check the agent id:
    Object o = desc.getParameter();
    MessageAddress cid = null;
    if (o instanceof MessageAddress) {
      cid = (MessageAddress) o;
    } else if (o instanceof String) {
      cid = MessageAddress.getMessageAddress((String) o);
    } else if (o instanceof List) {
      List parameters = (List) o;
      if (parameters.size() > 0) {
        Object o1 = parameters.get(0);
        if (o1 instanceof MessageAddress) {
          cid = (MessageAddress) o1;
        } else if (o1 instanceof String) {
          cid = MessageAddress.getMessageAddress((String) o1);
        }
      }
    }
    if (cid == null) {
      throw new IllegalArgumentException(
          "The ComponentDescription must specify the agent"+
          " name as the first parameter (see SimpleAgent)");
    }
    if (!mobileAgent.equals(cid)) {
      throw new IllegalArgumentException(
          "New agent address "+mobileAgent+
          " must match the ComponentDescription's address "+cid);
    }
    // let the remote node check the class type, since it
    // may be a class that we don't have in our local jars.
  }

  public int hashCode() {
    return 
      (((id == null) ? 17 : id.hashCode()) ^
       mobileAgent.hashCode());
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
	((state == null) ? 
	 (t.state == null) :
	 (state.equals(t.state))) &&
	((destNode == null) ? 
	 (t.destNode == null) :
	 (destNode.equals(t.destNode)));
    }
  }
  
  public String toString() {
    // cache?
    return 
      "Add "+
      (id == null ? 
       " unidentified ":
       id)+
      " of "+
      (mobileAgent == null ? 
       "this agent":
       "agent \""+mobileAgent+"\"")+
      " with "+state+" to "+
      (destNode == null ? 
       "this node":
       "node \""+destNode+"\"");
  }
}

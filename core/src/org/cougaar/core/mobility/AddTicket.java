/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.mobility;

import java.io.Serializable;
import java.net.URL;
import java.util.List;
import org.cougaar.core.agent.Agent; // inlined
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.mts.MessageAddress;

/**
 * A ticket requesting agent creation.
 */
public final class AddTicket extends AbstractTicket implements java.io.Serializable {

  private static final String DEFAULT_AGENT_CLASSNAME =
    "org.cougaar.core.agent.SimpleAgent";
  private static final String AGENT_INSERTION_POINT =
    Agent.INSERTION_POINT;

  private final Object id;
  private final MessageAddress mobileAgent;
  private final ComponentDescription desc;
  private final MessageAddress destNode;
  private Object state;

  // FIXME maybe add "timeout" here
  // FIXME maybe add "clone" here + clone name
  // FIXME maybe add security tags here

  /**
   * Add an agent with address <code>mobileAgent</code> to node
   * <code>destNode</code>.
   * <p>
   * If the desc is null then "SimpleAgent" is assumed.
   * <p>
   * The <code>id</code> should be obtained from the MobilityFactory
   * "createTicketIdentifier()".
   */
  public AddTicket(
      Object id,
      MessageAddress mobileAgent,
      MessageAddress destNode,
      ComponentDescription desc,
      Object state) {
    this.id = id;
    this.mobileAgent = mobileAgent;
    this.destNode = destNode;
    if (mobileAgent == null) {
      throw new IllegalArgumentException(
          "Must specify an agent to add");
    }
    if (desc == null) {
      desc =
        new ComponentDescription(
            DEFAULT_AGENT_CLASSNAME,
            AGENT_INSERTION_POINT,
            DEFAULT_AGENT_CLASSNAME,
            null,  // codebase
            mobileAgent,
            null,  // certificate
            null,  // lease
            null,  // policy
            ComponentDescription.PRIORITY_COMPONENT);
    } else {
      // validate the component description
      assertIsValid(
          mobileAgent,
          desc);
    }
    this.desc = desc;
    this.state = state;
  }

  /** Equivalent to "AddTicket(id, mobileAgent, destNode, null, null)" */
  public AddTicket(
      Object id,
      MessageAddress mobileAgent,
      MessageAddress destNode) {
    this(id, mobileAgent, destNode, null, null);
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

  /**
   * Get the new agent's ComponentDescription.
   */
  public ComponentDescription getComponentDescription() {
    return desc;
  }

  /**
   * Get the new agent's optional mobile state.
   */
  public Object getState() {
    return state;
  }

  private static void assertIsValid(
      MessageAddress mobileAgent,
      ComponentDescription desc) {
    // check the insertion point
    String ip = desc.getInsertionPoint();
    if (!ip.equals(AGENT_INSERTION_POINT)) {
      throw new IllegalArgumentException(
          "Insertion point must be \""+
          AGENT_INSERTION_POINT+"\", not "+ip);
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
	((destNode == null) ? 
	 (t.destNode == null) :
	 (destNode.equals(t.destNode))) &&
	((desc == null) ? 
	 (t.desc == null) :
	 (desc.equals(t.desc))) &&
	((state == null) ? 
	 (t.state == null) :
	 (state.equals(t.state)));
    }
  }
  
  public String toString() {
    // cache?
    return 
      "Add "+
      (id == null ? " unidentified ": id)+
      " of "+
      (mobileAgent == null ? 
       "this agent":
       "agent \""+mobileAgent+"\"")+
      " to "+
      (destNode == null ? 
       "this node":
       "node \""+destNode+"\"") +
      " with "+desc+
      (state == null ? " and state" : "");
  }
}

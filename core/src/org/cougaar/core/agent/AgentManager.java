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
package org.cougaar.core.agent;

import org.cougaar.core.mts.Message;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.node.NodeIdentifier;

import org.cougaar.core.blackboard.*;

import java.io.InputStream;
import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.node.*;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.MessageTransportService;

import java.beans.*;
import java.lang.reflect.*;


/** A container for Agents.
 * Although the AgentManager can hold Components other than Agents, the 
 * default BinderFactory will only actually accept Agents and other Binders.
 * If you want to load other sorts of components into AgentManager, you'll
 * need to supply a Binder which knows how to bind your Component class.
 **/
public class AgentManager 
  extends ContainerSupport
  implements ContainerAPI, AgentContainer
{
  public AgentManager() {
    if (!attachBinderFactory(new AgentBinderFactory())) {
      throw new RuntimeException("Failed to load the AgentBinderFactory");
    }
  }

  /** this constructor used for backwards compatability mode.  Goes away
   * when we are a contained by a Node component
   **/
  public AgentManager(ComponentDescription comdesc) {
    if (!attachBinderFactory(new AgentBinderFactory())) {
      throw new RuntimeException("Failed to load the AgentBinderFactory");
    }
  }

  private AgentManagerBindingSite bindingSite = null;

  public final void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    if (bs instanceof AgentManagerBindingSite) {
      bindingSite = (AgentManagerBindingSite) bs;
      setChildServiceBroker(new AgentManagerServiceBroker(bindingSite));
    } else {
      throw new RuntimeException("Tried to laod "+this+"into "+bs);
    }

    // We cannot start adding services until after the serviceBroker has been created.
    // add some services for the agents (clusters).
    // maybe this can be hooked in from Node soon.
    //childContext.addService(MetricsService.class, new MetricsServiceProvider(agent));
    //childContext.addService(MessageTransportService.class, new MessageTransportServiceProvider(agent));

  }

  protected final AgentManagerBindingSite getBindingSite() {
    return bindingSite;
  }


  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return "Node.AgentManager";
  }

  protected ContainerAPI getContainerProxy() {
    return new AgentManagerProxy();
  }

  public void requestStop() { }

  public String getName() {
    return getBindingSite().getName();
  }

  private void registerAgent(Agent agent) {
    if (agent instanceof ClusterServesClusterManagement) {
      getBindingSite().registerCluster((ClusterServesClusterManagement) agent);
    } else {
      System.err.println("Warning: attempted to registerAgent of non-cluster.");
    }
  }

  //
  // support classes
  //

  private static class AgentManagerServiceBroker 
    extends PropagatingServiceBroker
    {
      public AgentManagerServiceBroker(BindingSite bs) {
        super(bs);
      }
    }

  private class AgentManagerProxy 
    implements AgentManagerForBinder, 
  ClusterManagementServesCluster, 
  BindingSite {

    public String getName() {return AgentManager.this.getName(); }
    public void registerAgent(Agent agent) { 
      AgentManager.this.registerAgent(agent); 
    }

    // BindingSite
    public ServiceBroker getServiceBroker() {
      return AgentManager.this.getServiceBroker();
    }
    public void requestStop() {}
    public boolean remove(Object o) {return true; }
  }

  public boolean add(Object o) {
    try {
      return super.add(o);
    } catch (RuntimeException re) {
      System.err.println("Failed to add "+o+" to "+this+":");
      re.printStackTrace();
      return false;
    }
  }
  //
  // Implement the "AgentContainer" API, primarily to support
  //   agent mobility
  //

  public void addAgent(MessageAddress agentId, StateTuple tuple) {
    // FIXME cleanup this code
    //
    // first check that the agent isn't already loaded
    Iterator iter = super.boundComponents.iterator();
    while (iter.hasNext()) {
      Object oi = iter.next();
      if (!(oi instanceof BoundComponent)) {
        continue;
      }
      BoundComponent bc = (BoundComponent)oi;
      Binder b = bc.getBinder();
      if (!(b instanceof AgentBinder)) {
        continue;
      }
      Agent a = ((AgentBinder)b).getAgent();
      if ((a != null) &&
          (agentId.equals(a.getAgentIdentifier()))) {
        // agent already exists
        throw new RuntimeException(
            "Agent "+agentId+" already exists");
      }
    }
    
    // add the agent
    if (!(super.add(tuple))) {
      throw new RuntimeException(
          "Agent "+agentId+" returned \"false\"");
    }
  }

  // NOTE: assumes that "agent.unload()" has been called.
  public void removeAgent(MessageAddress agentId) {
    // FIXME cleanup this code
    //
    // find the agent in the set of children
    Iterator iter = super.boundComponents.iterator();
    while (iter.hasNext()) {
      Object oi = iter.next();
      if (!(oi instanceof BoundComponent)) {
        continue;
      }
      BoundComponent bc = (BoundComponent)oi;
      Binder b = bc.getBinder();
      if (!(b instanceof AgentBinder)) {
        continue;
      }
      Agent a = ((AgentBinder)b).getAgent();
      if ((a != null) &&
          (agentId.equals(a.getAgentIdentifier()))) {
        // remove the agent
        iter.remove();
        return;
      }
    }
    // no such agent
    throw new RuntimeException(
        "Agent "+agentId+" is not loaded");
  }

  public ComponentDescription getAgentDescription(MessageAddress agentId) {
    // FIXME cleanup this code
    Iterator iter = super.boundComponents.iterator();
    while (iter.hasNext()) {
      Object oi = iter.next();
      if (!(oi instanceof BoundComponent)) {
        continue;
      }
      BoundComponent bc = (BoundComponent)oi;
      Binder b = bc.getBinder();
      if (!(b instanceof AgentBinder)) {
        continue;
      }
      Agent a = ((AgentBinder)b).getAgent();
      if ((a != null) &&
          (agentId.equals(a.getAgentIdentifier()))) {
        Object cmp = bc.getComponent();
        if (cmp instanceof ComponentDescription) {
          // found the description
          return (ComponentDescription) cmp;
        } else {
          // description not known
          return null;
        }
      }
    }
    // no such agent
    return null;
  }



  //
  //
  // All the code below will soon (9.3) be replaced by the
  // new agent-mobility implementation, which now uses
  // node-level and agent-level components.
  //
  //



  /**
   * Support Node-issued agent mobility requests.
   * <p>
   * @param agentID agent to move
   * @param nodeID destination node address
   */
  public void moveAgent(
      ClusterIdentifier agentID,
      NodeIdentifier nodeID) {
    // wouldn't usually create a new AgentManagement per request...
    AgentManagement stubAM = new AgentManagement();
    stubAM.moveAgent(agentID, nodeID);
  }

  /**
   * Support Node-issued agent mobility requests.
   * <p>
   * @param agentID agent to move
   * @param nodeID destination node address
   */
  public void cloneAgent(
      ClusterIdentifier agentID,
      NodeIdentifier nodeID,
      ClusterIdentifier cloneAgentID,
      boolean cloneBlackboard) {
    AgentManagement stubAM = new AgentManagement();
    stubAM.cloneAgent(agentID, nodeID, cloneAgentID, cloneBlackboard);
  }

  /**
   * For use by <tt>findAgent(..)</tt>.
   */
  private static class AgentEntry {
    private final ComponentDescription desc;
    private final Agent agent;
    public AgentEntry(
        ComponentDescription desc,
        Agent agent) {
      this.desc = desc;
      this.agent = agent;
      if ((desc == null) || (agent == null)) {
        throw new NullPointerException();
      }
    }
    public ComponentDescription getComponentDescription() {
      return desc;
    }
    public Agent getAgent() {
      return agent;
    }
  }

  private AgentEntry findAgent(ClusterIdentifier agentID) {
    // lookup the agent on this node
    ComponentDescription desc = null;
    Agent agent = null;
    for (Iterator iter = super.boundComponents.iterator(); ;) {
      if (!(iter.hasNext())) {
        // no such agent?
        return null;
      }
      Object oi = iter.next();
      if (!(oi instanceof BoundComponent)) {
        continue;
      }
      BoundComponent bc = (BoundComponent)oi;
      Binder b = bc.getBinder();
      if (!(b instanceof AgentBinder)) {
        continue;
      }
      Agent a = ((AgentBinder)b).getAgent();
      if ((a != null) &&
          (agentID.equals(a.getAgentIdentifier()))) {
        // found our agent
        agent = a;
        Object cmp = bc.getComponent();
        if (cmp instanceof ComponentDescription) {
          desc = (ComponentDescription)cmp;
        }
        break;
      }
    }
    return new AgentEntry(desc, agent);
  }

  private boolean removeAgent(ClusterIdentifier agentID) {
    // unhand the original agent, let GC reclaim it
    //
    // NOTE: assumes that "agent.unload()" has been called.
    //
    // ContainerSupport should be modified to clean this up...
    for (Iterator iter = super.boundComponents.iterator();
        iter.hasNext();
        ) {
      Object oi = iter.next();
      if (!(oi instanceof BoundComponent)) {
        continue;
      }
      BoundComponent bc = (BoundComponent)oi;
      Binder b = bc.getBinder();
      if (!(b instanceof AgentBinder)) {
        continue;
      }
      Agent a = ((AgentBinder)b).getAgent();
      if ((a != null) &&
          (agentID.equals(a.getAgentIdentifier()))) {
        // remove the agent
        iter.remove();
        return true;
      }
    }
    return false;
  }

  /**
   * Agent management services for "moveAgent(..)" and "cloneAgent(..)".
   * <p>
   * This is an inner class just to make the code tidy.  This
   * should be made a Service and moved out of AgentManager.
   */
  private class AgentManagement {

    private static final boolean VERBOSE = true;

    public void moveAgent(
        ClusterIdentifier agentID,
        NodeIdentifier nodeID) {
      // check parameters, security, etc
      if ((agentID == null) ||
          (nodeID == null)) {
        throw new IllegalArgumentException(
            "Must specify an agentID ("+
            agentID+") and nodeID ("+nodeID+")");
      }

      // get this node's id
      NodeIdentifier thisNodeID = getNodeIdentifier();
      if (thisNodeID.equals(nodeID)) {
        throw new RuntimeException(
            "Agent "+agentID+" already on Node "+nodeID);
      }

      // lookup the agent on this node
      AgentEntry agentEntry = 
        AgentManager.this.findAgent(agentID);
      if (agentEntry == null) {
        throw new RuntimeException(
            "Agent "+agentID+" is not on Node "+thisNodeID);
      }
      ComponentDescription origDesc = 
        agentEntry.getComponentDescription();
      Agent agent = agentEntry.getAgent();

      if (VERBOSE) {
        System.out.println("Move Agent - Suspend Agent "+agentID);
      }

      // suspend the agent's activity, prepare for state capture
      agent.suspend();

      if (VERBOSE) {
        System.out.println("Move Agent - Get the Agent state");
      }

      // recursively gather the agent state
      Object state = 
        ((agent instanceof StateObject) ?
         ((StateObject)agent).getState() :
         null);

      if (VERBOSE) {
        System.out.println("Move Agent - The state is: "+state);
      }

      // create an ADD ComponentMessage
      ComponentMessage addMsg =
        new ComponentMessage(
            new NodeIdentifier(bindingSite.getIdentifier()),
            nodeID,
            ComponentMessage.ADD,
            origDesc,
            state);

      // send the message to destination node
      sendMessage(addMsg, thisNodeID);

      if (VERBOSE) {
        System.out.println("Move Agent - Sent Message: "+addMsg);
      }

      // wait for an add acknowledgement -- postponed to 8.6+

      // stop and unload the original agent
      agent.stop();
      agent.unload();

      // disable the agent's ServiceBroker, cancel all services requested 
      //   by the agent, and set all pointers leaving the agent to null.
      //
      // this could be done by a ServiceFilter using an
      //   java.lang.reflect.InvocationHandler to proxy all
      //   actual Services.
      //
      // postponed to 8.6+ -- we'll assume the Agent is well-behaved

      // unhand the original agent, let GC reclaim it
      AgentManager.this.removeAgent(agentID);

      // the agent is isolated and will be GC'ed
      //
      // even if the agent spawned Threads it should be unable
      //   to interact with the Node

      if (VERBOSE) {
        System.out.println(
            "Moved Agent "+agentID+" to Node "+nodeID);
      }
    }

    public void cloneAgent(
        ClusterIdentifier agentID,
        NodeIdentifier nodeID,
        ClusterIdentifier cloneAgentID,
        boolean cloneBlackboard) {
      // check parameters, security, etc
      if ((agentID == null) ||
          (nodeID == null) ||
          (cloneAgentID == null)) {
        throw new IllegalArgumentException(
            "CloneAgent - Must specify an agentID ("+
            agentID+") and nodeID ("+nodeID+") and cloneID ("+
            cloneAgentID+")");
      } else if (agentID.equals(cloneAgentID)) {
        throw new IllegalArgumentException(
            "Clone name must be different than original name ("+
            agentID+")");
      }

      // get this node's id
      final NodeIdentifier thisNodeID = getNodeIdentifier();
      if (thisNodeID == null) {
        throw new RuntimeException(
            "Clone Agent - Unable to get this Node's Identification");
      }

      // lookup the agent on this node
      AgentEntry agentEntry = 
        AgentManager.this.findAgent(agentID);
      if (agentEntry == null) {
        throw new RuntimeException(
            "Agent "+agentID+" is not on Node "+thisNodeID);
      }
      ComponentDescription origDesc = 
        agentEntry.getComponentDescription();
      Agent agent = agentEntry.getAgent();

      // the description of the agent on the other side will be 
      // exactly the same except that it will have a new name
      ComponentDescription cloneDesc = 
        new ComponentDescription (
            origDesc.getName(),
            origDesc.getInsertionPoint(),
            origDesc.getClassname(),
            origDesc.getCodebase(),
            cloneAgentID,
            origDesc.getCertificate(),
            origDesc.getLeaseRequested(),
            origDesc.getPolicy());

      if (VERBOSE) {
        System.out.println("Clone Agent - Suspend Agent "+agentID);
      }

      // suspend the agent's activity, prepare for state capture
      agent.suspend();

      if (VERBOSE) {
        System.out.println("Clone Agent - Get the Agent state");
      }

      // recursively gather the agent state
      Object state;
      if (agent instanceof StateObject) {
        if (cloneBlackboard) {
          state = ((StateObject) agent).getState();
        } else {
          // FIXME kluge:
          if (!(agent instanceof ClusterImpl)) {
            throw new RuntimeException(
                "Unable to get Agent state without Blackboard; "+
                "Agent class is: "+agent.getClass().toString());
          }
          state = ((ClusterImpl) agent).getStateExcludingBlackboard();
        }
      } else {
        // error?
        state = null;
      }

      // create an ADD ComponentMessage
      ComponentMessage addMsg =
        new ComponentMessage(
            new NodeIdentifier(bindingSite.getIdentifier()),
            nodeID,
            ComponentMessage.ADD,
            cloneDesc,
            state);

      // send the message to destination node
      sendMessage(addMsg, thisNodeID);

      if (VERBOSE) {
        System.out.println("Sent Message: "+addMsg);
      }

      // wait for an add acknowledgement -- postponed to 8.6+

      // resume the original agent
      if (VERBOSE) {
        System.out.println("Clone Agent - Resume Agent "+agentID);
      }

      agent.resume();

      if (VERBOSE) {
        System.out.println(
            "Cloned Agent "+agentID+" to Node "+nodeID);
      }
    }

    //
    // utility methods
    //
    // should save these services and release!  turn this
    // class into a Component and add load/unload methods.
    //

    private NodeIdentifier getNodeIdentifier() {
      // get this node's id
      NodeIdentificationService nis = (NodeIdentificationService)
        AgentManager.this.getServiceBroker().getService(
            this,
            NodeIdentificationService.class,
            null);
      if (nis == null) {
        System.err.println("Unable to get this Node's Identification");
        return null;
      }
      return nis.getNodeIdentifier();
    }

    private boolean sendMessage(
        Message m,
        final NodeIdentifier thisNodeID) {
      // create a dummy message transport client
      MessageTransportClient mtc = 
        new MessageTransportClient() {
          public void receiveMessage(Message message) {
            // never
          }
          public MessageAddress getMessageAddress() {
            return thisNodeID;
          }
        };

      // get the message transport
      MessageTransportService mts = (MessageTransportService)
        AgentManager.this.getServiceBroker().getService(
            mtc,
            MessageTransportService.class,
            null);
      if (mts == null) {
        System.err.println(
            "Unable to get MessageTransport for mobility message");
        return false;
      }

      // send the message to destination node
      mts.sendMessage(m);

      return true;
    }
  }
}

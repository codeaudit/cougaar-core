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

import java.io.InputStream;
import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.cluster.*;
import org.cougaar.core.society.*;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.MessageTransportService;

import java.beans.*;
import java.lang.reflect.*;


/** A container for Agent Components.
 **/
public class AgentManager 
  extends ContainerSupport
  implements ContainerAPI
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

  /**
   * Recursively print the result of "agent.getState()".
   * <p>
   * This is expected to break once Components create
   * customized state holders!
   */
  private static void debugState(Object state, String path) {
    if (state instanceof StateTuple[]) {
      StateTuple[] tuples = (StateTuple[])state;
      for (int i = 0; i < tuples.length; i++) {
        String prefix = path+"["+i+" / "+tuples.length+"]";
        StateTuple sti = tuples[i];
        if (sti == null) {
          System.out.println(
              prefix+": null");
          continue;
        }
        ComponentDescription cdi = sti.getComponentDescription();
        if (cdi == null) {
          System.out.println(
            prefix+": {null, ..}");
          continue;
        }
        System.out.println(
            prefix+": "+
            cdi.getInsertionPoint()+" = "+
            cdi.getClassname()+" "+
            cdi.getParameter());
        Object si = sti.getState();
        if (si != null) {
          debugState(si, prefix);
        }
      }
    } else {
      System.out.println(path+" non-StateTuple[] "+state);
    }
  }

  /**
   * Support Node-issued agent mobility requests.
   * <p>
   * @param agentID agent to move
   * @param nodeID destination node address
   */
  public void moveAgent(
      ClusterIdentifier agentID,
      NodeIdentifier nodeID) {

    // check parameters, security, etc
    if ((agentID == null) ||
        (nodeID == null)) {
      // error
      System.err.println(
          "Must specify an agentID ("+
          agentID+") and nodeID ("+nodeID+")");
      return;
    }

    // get this node's id
    NodeIdentificationService nis = (NodeIdentificationService)
      getServiceBroker().getService(
          this,
          NodeIdentificationService.class,
          null);
    if (nis == null) {
      System.err.println("Unable to get this Node's Identification");
      return;
    }
    final NodeIdentifier thisNodeID = nis.getNodeIdentifier();
    if (thisNodeID.equals(nodeID)) {
      System.err.println("Agent "+agentID+" already on Node "+nodeID);
      return;
    }

    // lookup the agent on this node
    ComponentDescription origDesc = null;
    Agent agent = null;
    for (Iterator iter = super.boundComponents.iterator(); ;) {
      if (!(iter.hasNext())) {
        // no such agent?
        System.err.println(
            "Agent "+agentID+" is not on Node "+thisNodeID);
        return;
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
          origDesc = (ComponentDescription)cmp;
        }
        break;
      }
    }

    System.out.println("Suspend Agent "+agentID);

    // suspend the agent's activity, prepare for state capture
    agent.suspend();

    System.out.println("Get the Agent state");

    // recursively gather the agent state
    Object state = 
      ((agent instanceof StateObject) ?
       ((StateObject)agent).getState() :
       null);

    System.out.println("The state is: "+state);
    debugState(state, "");

    // create an ADD ComponentMessage
    ComponentMessage addMsg =
      new ComponentMessage(
          new NodeIdentifier(bindingSite.getIdentifier()),
          nodeID,
          ComponentMessage.ADD,
          origDesc,
          state);

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
      getServiceBroker().getService(
          mtc,
          MessageTransportService.class,
          null);
    if (mts == null) {
      // error!  we should have requested this earlier...
      System.err.println(
          "Unable to get MessageTransport for mobility message");
      return;
    }

    // send the message to destination node
    mts.sendMessage(addMsg);
    System.out.println("Sent Message: "+addMsg);

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
        // remove our agent
        iter.remove();
        break;
      }
    }

    // the agent is isolated and will be GC'ed
    //
    // even if the agent spawned Threads it should be unable
    //   to interact with the Node

    System.out.println(
        "Moved Agent "+agentID+" to Node "+nodeID);
  }

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
 

  private class AgentManagerProxy implements AgentManagerForBinder, 
                                             ClusterManagementServesCluster, 
                                             BindingSite {

    public String getName() {return AgentManager.this.getName(); }
    public void registerAgent(Agent agent) { AgentManager.this.registerAgent(agent); }

    // BindingSite
    public ServiceBroker getServiceBroker() {
      return AgentManager.this.getServiceBroker();
    }
    public void requestStop() {}
    public boolean remove(Object o) {return true; }
  }

}


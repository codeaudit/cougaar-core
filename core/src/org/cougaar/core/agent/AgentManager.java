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

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.blackboard.*;

import java.io.InputStream;
import java.util.*;
import org.cougaar.util.*;
import org.cougaar.util.log.*;
import org.cougaar.core.component.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.node.*;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.MessageAddress;
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
  /** The Insertion point for the AgentManager, defined relative to that of Node. **/
  public static final String INSERTION_POINT = Node.INSERTION_POINT + ".AgentManager";
  public AgentManager() {
    BinderFactory bf = new DefaultAgentBinderFactory();
    if (!attachBinderFactory(bf)) {
      throw new RuntimeException("Failed to load the "+bf);
    }
  }

  private AgentManagerBindingSite bindingSite = null;

  public final void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    if (bs instanceof AgentManagerBindingSite) {
      bindingSite = (AgentManagerBindingSite) bs;
      setChildServiceBroker(new AgentManagerServiceBroker(bindingSite));
    } else {
      throw new RuntimeException("Tried to load "+this+"into "+bs);
    }

    // We cannot start adding services until after the serviceBroker has been created.
    // add some services for the agents (clusters).
    // maybe this can be hooked in from Node soon.
    //childContext.addService(MetricsService.class, new MetricsServiceProvider(agent));
    //childContext.addService(MessageTransportService.class, new MessageTransportServiceProvider(agent));

  }

  /** Load up any externally-specified AgentBinders **/
  public void load() {
    super.load();

    ComponentDescriptions cds;
    
    ServiceBroker sb = getServiceBroker();
    
    String nodeName;
    try {
      NodeIdentificationService nis = (NodeIdentificationService) 
        sb.getService(this,NodeIdentificationService.class,null);
      if (nis != null) {
        nodeName = nis.getMessageAddress().toString();
      } else {
        throw new RuntimeException("No node name specified");
      }
      sb.releaseService(this, NodeIdentificationService.class, nis);
    } catch (RuntimeException e) {
      throw new Error("Couldn't figure out Node name ", e);
    }

    try {
      InitializerService is = (InitializerService) 
        sb.getService(this, InitializerService.class, null);
      Logging.getLogger(AgentManager.class).info(nodeName + " AgentManager.load about to look for CompDesc's of Agent Binders.");
      // Get all items _below_ given insertion point.
      // To get just binders, must use extract method later....
      cds = new ComponentDescriptions(is.getComponentDescriptions(nodeName, INSERTION_POINT));
      sb.releaseService(this, InitializerService.class, is);
    } catch (Exception e) {
      throw new Error("Couldn't initialize AgentManager Binders with InitializerService ", e);
    }

    addAll(ComponentDescriptions.sort(cds.extractInsertionPointComponent(INSERTION_POINT + ".Binder")));
  }


  protected final AgentManagerBindingSite getBindingSite() {
    return bindingSite;
  }


  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
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
      Logging.getLogger(this.getClass()).error("Failed to add "+o+" to "+this, re);
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
    for (Iterator iter = binderIterator(); iter.hasNext(); ) {
      Object oi = iter.next();
      if (!(oi instanceof AgentBinder)) {
        continue;
      }
      MessageAddress id = ((AgentBinder) oi).getAgentIdentifier();
      if (agentId.equals(id)) {
        // agent already exists
        throw new RuntimeException(
            "Agent "+agentId+" already exists");
      }
    }
    
    // add the agent
    if (! add(tuple)) {
      throw new RuntimeException(
          "Agent "+agentId+" returned \"false\"");
    }

    // the agent has started and is now ACTIVE
  }

  public void removeAgent(MessageAddress agentId) {
    // find the agent's component description
    ComponentDescription desc = getAgentDescription(agentId);
    if (desc == null) {
      // no such agent, or not loaded with a desc
      throw new RuntimeException(
          "Agent "+agentId+" is not loaded");
    }

    if (! remove(desc)) {
      throw new RuntimeException(
          "Unable to remove agent "+agentId+
          ", \"remove()\" returned false");
    }

    // the agent has been UNLOADED and removed
  }

  public ComponentDescription getAgentDescription(MessageAddress agentId) {
    // FIXME cleanup this code
    synchronized (boundComponents) {
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
        MessageAddress id = ((AgentBinder) b).getAgentIdentifier();
        if (agentId.equals(id)) {
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
    }

    // no such agent
    return null;
  }
}

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

package org.cougaar.core.agent;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.component.Binder;
import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BinderFactorySupport;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BinderSupport;
import org.cougaar.core.component.BoundComponent;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ComponentFactory;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.PropagatingServiceBroker;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.Node;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

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

  public final void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    setChildServiceBroker(new AgentManagerServiceBroker(bs));
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
      ComponentInitializerService cis = (ComponentInitializerService) 
        sb.getService(this, ComponentInitializerService.class, null);
      Logging.getLogger(AgentManager.class).info(
          nodeName + " AgentManager.load about to look for CompDesc's of Agent Binders.");
      // Get all items _below_ given insertion point.
      // To get just binders, must use extract method later....
      cds = new ComponentDescriptions(cis.getComponentDescriptions(nodeName, INSERTION_POINT));
      sb.releaseService(this, ComponentInitializerService.class, cis);
    } catch (Exception e) {
      throw new Error("Couldn't initialize AgentManager Binders with ComponentInitializerService ", e);
    }

    addAll(ComponentDescriptions.sort(cds.extractInsertionPointComponent(INSERTION_POINT + ".Binder")));
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
    implements BindingSite, ContainerAPI {

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

  public boolean containsAgent(MessageAddress agentId) {
    return (getAgentDescription(agentId) != null);
  }

  public Set getAgentAddresses() {
    Map m = getAgents();
    return Collections.unmodifiableSet(m.keySet());
  }

  public ComponentDescription getAgentDescription(MessageAddress agentId) {
    Map m = getAgents();
    return (ComponentDescription) m.get(agentId);
  }

  public List getComponents() {
    // get the child components
    synchronized (boundComponents) {
      int n = boundComponents.size();
      List result = new ArrayList(n);
      for (int i = 0; i < n; i++) {
        BoundComponent bc = (BoundComponent) boundComponents.get(i);
        Object comp = bc.getComponent();
        if (comp instanceof ComponentDescription) {
          ComponentDescription cd = (ComponentDescription) comp;
          result.add(cd);
        }
      }
      return result;
    }
  }

  public Map getAgents() {
    // FIXME cleanup this code
    //
    // assume that all agents are loaded with a ComponentDescription,
    // where the desc's parameter is (<agentId> [, ...])
    synchronized (boundComponents) {
      Map ret = new HashMap(boundComponents.size());
      for (Iterator iter = boundComponents.iterator(); iter.hasNext(); ) {
        Object obc = iter.next();
        if (!(obc instanceof BoundComponent)) {
          continue;
        }
        BoundComponent bc = (BoundComponent) obc;
        Object cmp = bc.getComponent();
        if (!(cmp instanceof ComponentDescription)) {
          continue;
        }
        ComponentDescription desc = (ComponentDescription) cmp;
        Object o = desc.getParameter();
        MessageAddress cid = null;
        if (o instanceof MessageAddress) {
          cid = (MessageAddress) o;
        } else if (o instanceof String) {
          cid = MessageAddress.getMessageAddress((String) o);
        } else if (o instanceof List) {
          List l = (List)o;
          if (l.size() > 0) {
            Object o1 = l.get(0);
            if (o1 instanceof MessageAddress) {
              cid = (MessageAddress) o1;
            } else if (o1 instanceof String) {
              cid = MessageAddress.getMessageAddress((String) o1);
            }
          }
        }
        if (cid != null) {
          ret.put(cid, desc);
        }
      }
      return ret;
    }
  }

  public void addAgent(MessageAddress agentId, StateTuple tuple) {
    if (containsAgent(agentId)) {
      // agent already exists
      throw new RuntimeException(
          "Agent "+agentId+" already exists");
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

  /**
   * The default factory for binding Agents to the AgentManager.
   **/
  private static class DefaultAgentBinderFactory extends BinderFactorySupport {

    public Binder getBinder(Object child) {
      if (child instanceof ComponentDescription) {
        ComponentDescription cd = (ComponentDescription) child;
        if (Agent.INSERTION_POINT.equals(cd.getInsertionPoint())) {
          //Might want to differentiate between Agent and specializations of
          //agents such as Clusters at some point.  But for now...
          return new DefaultAgentBinder(this, child);
        }
      } else if (child instanceof Agent) {
        return new DefaultAgentBinder(this, child);
      }
      return null;
    }

    /**
     * The default Binder for Agents.
     */
    private static class DefaultAgentBinder
      extends BinderSupport
      implements BindingSite {
        public DefaultAgentBinder(BinderFactory bf, Object child) {
          super(bf, child);
        }
        protected final BindingSite getBinderProxy() {
          // horribly unsecure!
          return this;
        }
      }
  }

}

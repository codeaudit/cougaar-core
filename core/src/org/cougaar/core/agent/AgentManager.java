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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.Node;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/** A container for Agents.
 * Although the AgentManager can hold Components other than Agents, the 
 * default BinderFactory will only actually accept Agents and other Binders.
 * If you want to load other sorts of components into AgentManager, you'll
 * need to supply a Binder which knows how to bind your Component class.
 **/
public class AgentManager 
extends ContainerSupport
implements AgentContainer
{
  /** The Insertion point for the AgentManager, defined relative to that of Node. **/
  public static final String INSERTION_POINT = Node.INSERTION_POINT + ".AgentManager";

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
      Logger logger = Logging.getLogger(AgentManager.class);
      if (logger.isInfoEnabled()) {
        logger.info(nodeName + " AgentManager.load about to look for CompDesc's of Agent Binders.");
      }
      // Get all items _below_ given insertion point.
      // To get just binders, must use extract method later....
      cds = new ComponentDescriptions(cis.getComponentDescriptions(nodeName, INSERTION_POINT));
      sb.releaseService(this, ComponentInitializerService.class, cis);
    } catch (Exception e) {
      throw new Error("Couldn't initialize AgentManager Binders with ComponentInitializerService ", e);
    }

    addAll(ComponentDescriptions.sort(cds.extractInsertionPointComponent(INSERTION_POINT + ".Binder")));
  }

  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    return null;
  }

  public boolean add(Object o) {
    try {
      return super.add(o);
    } catch (RuntimeException re) {
      Logging.getLogger(this.getClass()).error(
          "Failed to add "+o+" to "+this, re);
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
    return super.listComponents();
  }

  public Map getAgents() {
    // FIXME cleanup this code
    //
    // assume that all agents are loaded with a ComponentDescription,
    // where the desc's parameter is (<agentId> [, ...])
    List descs = listComponents();
    Map ret = new HashMap(descs.size());
    for (Iterator iter = descs.iterator(); iter.hasNext();) {
      ComponentDescription desc = (ComponentDescription) iter.next();
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
}

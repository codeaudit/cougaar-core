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

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.NodeControlService;
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
implements AgentContainer
{
  public static final String INSERTION_POINT = "Node.AgentManager";

  private static final String FILENAME_PROP = "org.cougaar.filename";
  private static final String EXPTID_PROP = "org.cougaar.experiment.id";
  private static final String INITIALIZER_PROP = 
    "org.cougaar.core.node.InitializationComponent";
  private static final String NODE_AGENT_CLASSNAME_PROPERTY =
    "org.cougaar.core.node.classname";

  public void load() {
    super.load();

    String nodeName = getNodeName();

    add_node_identification_service(nodeName);

    add_node_control_service();

    add(getInitializerDescription());

    addAll(getAgentBinderDescriptions(nodeName));

    add(getNodeAgentDescription(nodeName));
  }

  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    return null;
  }

  //
  // override "add(o)" to log exceptions:
  //

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
  // methods used by "load()":
  //

  private String getNodeName() {
    String nodeName = System.getProperty("org.cougaar.node.name");
    if (nodeName == null) {
      try {
        nodeName = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException uhe) {
      }
      if (nodeName == null) {
        throw new IllegalArgumentException("Node name not specified");
      }
    }
    return nodeName;
  }

  private void add_node_identification_service(String nodeName) {
    final MessageAddress localNode = 
      MessageAddress.getMessageAddress(nodeName);
    Class clazz = NodeIdentificationService.class;
    Service service =
      new NodeIdentificationService() {
        public MessageAddress getMessageAddress() {
          return localNode;
        }
      };
    add_service(clazz, service);
  }

  private void add_node_control_service() {
    // instead of using the node's or our service broker as the
    // "rootsb", we use our child service broker.  All components
    // and agents are loaded as our children, so this is should
    // be fine.
    final ServiceBroker rootsb = getChildServiceBroker();
    // create a proxy for our agent manager, to prevent casting
    final AgentContainer rootac = 
      new AgentContainer() {
        public boolean containsAgent(MessageAddress agentId) {
          return AgentManager.this.containsAgent(agentId);
        }
        public Set getAgentAddresses() {
          return AgentManager.this.getAgentAddresses();
        }
        public ComponentDescription getAgentDescription(
            MessageAddress agentId) {
          return AgentManager.this.getAgentDescription(agentId);
        }
        public Map getAgents() {
          return AgentManager.this.getAgents();
        }
        public List getComponents() {
          return AgentManager.this.getComponents();
        }
        public void addAgent(
            MessageAddress agentId, StateTuple tuple) {
          AgentManager.this.addAgent(agentId, tuple);
        }
        public boolean add(Object o) {
          return AgentManager.this.add(o);
        }
        public void removeAgent(MessageAddress agentId) {
          AgentManager.this.removeAgent(agentId);
        }
        public boolean remove(Object o) {
          return AgentManager.this.remove(o);
        }
      };
    Class clazz = NodeControlService.class;
    Service service =
      new NodeControlService() {
        public ServiceBroker getRootServiceBroker() {
          return rootsb;
        }
        public AgentContainer getRootContainer() {
          return rootac;
        }
      };
    add_service(clazz, service);
  }

  private ComponentDescription getInitializerDescription() {
    // we need the ComponentInitializerService for to read the
    // agent binder descriptions in "getAgentBinderDescriptions",
    // so we must load it here.
    //
    // Ideally we'd do this within each agent, but that's not
    // possible if we must support agent binders.
    String classname = getComponentInitializerClass();
    return
      new ComponentDescription(
          classname,
          AgentManager.INSERTION_POINT+".Component",
          classname,
          null,  //codebase
          null,  //params
          null,  //certificate
          null,  //lease
          null,  //policy
          ComponentDescription.PRIORITY_COMPONENT);
  }

  private List getAgentBinderDescriptions(String nodeName) {
    ServiceBroker csb = getChildServiceBroker();
    ComponentDescriptions cds;
    try {
      ComponentInitializerService cis = (ComponentInitializerService) 
        csb.getService(this, ComponentInitializerService.class, null);
      Logger logger = Logging.getLogger(AgentManager.class);
      if (logger.isInfoEnabled()) {
        logger.info(
          nodeName + 
          " AgentManager.load about to look for CompDesc's"+
          " of Agent Binders.");
      }
      // Get all items _below_ given insertion point.
      // To get just binders, must use extract method later....
      cds = new ComponentDescriptions(
          cis.getComponentDescriptions(nodeName, INSERTION_POINT));
      csb.releaseService(this, ComponentInitializerService.class, cis);
    } catch (Exception e) {
      throw new Error(
          "Couldn't initialize AgentManager Binders"+
          "  with ComponentInitializerService ", e);
    }

    return 
      ComponentDescriptions.sort(
          cds.extractInsertionPointComponent(
            INSERTION_POINT + ".Binder"));
  }

  private ComponentDescription getNodeAgentDescription(
      String nodeName) {
    String classname = System.getProperty(
        NODE_AGENT_CLASSNAME_PROPERTY,
        "org.cougaar.core.agent.AgentImpl");
    List params = new ArrayList(1);
    params.add(nodeName);
    ComponentDescription desc = 
      new ComponentDescription(
          classname,
          Agent.INSERTION_POINT,
          classname,
          null,  //codebase
          params,
          null,  //certificate
          null,  //lease
          null,  //policy
          ComponentDescription.PRIORITY_COMPONENT);
    return desc;
  }

  private String getComponentInitializerClass() {
    String component = System.getProperty(INITIALIZER_PROP);
    if (component == null) {
      component = getDefaultComponentInitializerClass();
      System.setProperty(INITIALIZER_PROP, component);
    }
    // if full class name not specified, intuit it
    if (component.indexOf(".") < 0) {
      // build up the name, full name was not specified.
      component = 
        "org.cougaar.core.node." +
        component +
        "ComponentInitializerServiceComponent";
    }
    Logger logger = Logging.getLogger(AgentManager.class);
    if (logger.isInfoEnabled()) {
      logger.info("Will intialize components from " + component);
    }
    return component;
  }

  private String getDefaultComponentInitializerClass() {
    // figure out whether to use Files or CSMART DB for component
    // initalization (since it's not explicitly specified by
    // a -D argument)
    //
    // This will use the CSMART DB (advertising the DBInitializerService)
    // if the experiment_id system property was set, and some other
    // initializer was not selected.  Otherwise, INI files are used
    // (and no DBInitializerService is provided) however, users may
    // specify, for example, an XML initializer
    String filename = System.getProperty(FILENAME_PROP);
    String expt = System.getProperty(EXPTID_PROP);
    Logger logger = Logging.getLogger(AgentManager.class);
    if ((filename == null) && (expt == null)) {
      // use the default "name.ini"
      if (logger.isWarnEnabled()) {
        logger.warn(
            "Got no filename or experimentId! Using default File");
      }
      return "File";
    }
    if (filename == null) {
      // use the experiment ID to read from the DB
      if (logger.isWarnEnabled()) {
        logger.warn("Got no filename, using exptID " + expt);
      }
      return "DB";
    }
    if (expt == null) {
      // use the filename provided
      if (logger.isWarnEnabled()) {
        logger.warn("Got no exptID, using given filename " + filename);
      }
    }
    return "File";
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

  private ServiceProvider add_service(
      Class clazz, Service service) {
    // we must use our service broker, otherwise our child components
    // will not be able to block our services
    ServiceBroker sb = getServiceBroker();
    ServiceProvider sp = new SimpleServiceProvider(clazz, service);
    if (!sb.addService(clazz, sp)) {
      throw new RuntimeException("Unable to add service "+clazz);
    }
    return sp;
  }
  private void revoke_service(
      Class clazz, ServiceProvider sp) {
    if (sp != null) {
      ServiceBroker sb = getServiceBroker();
      sb.revokeService(clazz, sp);
    }
  }
  private static final class SimpleServiceProvider
    implements ServiceProvider {
      private final Class clazz;
      private final Service service;
      public SimpleServiceProvider(
          Class clazz, Service service) {
        this.clazz = clazz;
        this.service = service;
      }
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (clazz.isAssignableFrom(serviceClass)) {
          return service;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }
}

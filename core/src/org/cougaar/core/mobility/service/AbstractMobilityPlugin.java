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
package org.cougaar.core.mobility.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mobility.MobileAgentService;
import org.cougaar.core.mobility.MobilityException;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.arch.*;
//import org.cougaar.core.mobility.ldm.AgentMove;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.AgentTransfer;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.GenericStateModel;
import org.cougaar.util.UnaryPredicate;

/**
 * Base class for root (node-level) agent mobility plugin.
 * <p>
 * This plugin is loaded into all agents, both for
 * node-agents and regular leaf-agents.
 * <p>
 * In leaf-agents this plugin simply proxies AgentMove 
 * requests to the agent's parent node.  Most of the code
 * is turned off.
 * <p>
 * In node-agents this plugin coordinates agent 
 * mobility.
 */
public abstract class AbstractMobilityPlugin 
  extends ComponentPlugin 
{

  private static final UnaryPredicate CONTROL_PRED =
    new UnaryPredicate() {
	public boolean execute(Object o) {
	  return (o instanceof AgentControl);
	}
      };
  
  private static final UnaryPredicate TRANSFER_PRED =
    new UnaryPredicate() {
	public boolean execute(Object o) {
	  return (o instanceof AgentTransfer);
	}
      };
  

  protected MessageAddress agentId;
  protected MessageAddress nodeId;
  protected boolean isNode;

  //private IncrementalSubscription moveSub;
  private IncrementalSubscription controlSub;
  
  protected LoggingService log;

  private MobilityFactory mobilityFactory;


  // the rest is only used if (isNode == true):


  private IncrementalSubscription transferSub;

  protected NodeControlService nodeControlService;
  protected TopologyReaderService topologyReaderService;

  private ServiceBroker nodeSB;
  protected AgentContainer agentContainer;

  private MobileAgentServiceProviderImpl mobileAgentSP;

  private final List todo = new ArrayList(5);

  // a map from agent MessageAddress to an AgentEntry
  private final Map entries = new HashMap(13);


  public void load() {
    super.load();

    // get the logger
    log = (LoggingService) 
      getServiceBroker().getService(
          this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    // get the agentId
    AgentIdentificationService agentIdService = 
      (AgentIdentificationService)
      getServiceBroker().getService(
          this,
          AgentIdentificationService.class,
          null);
    if (agentIdService == null) {
      throw new RuntimeException(
          "Unable to obtain agent-id service");
    }
    this.agentId = agentIdService.getMessageAddress();
    getServiceBroker().releaseService(
        this, AgentIdentificationService.class, agentIdService);
    if (agentId == null) {
      throw new RuntimeException(
          "Unable to obtain agent id");
    }

    // get the nodeId
    NodeIdentificationService nodeIdService = 
      (NodeIdentificationService)
      getServiceBroker().getService(
          this,
          NodeIdentificationService.class,
          null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to obtain node-id service");
    }
    this.nodeId = nodeIdService.getNodeIdentifier();
    getServiceBroker().releaseService(
        this, NodeIdentificationService.class, nodeIdService);
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node id");
    }

    // either running within a node-agent or leaf-agent
    isNode = (agentId.equals(nodeId));

    // get the mobility factory
    DomainService domain = (DomainService)
      getServiceBroker().getService(
          this,
          DomainService.class,
          null);
    if (domain == null) {
      throw new RuntimeException(
          "Unable to obtain the domain service");
    }
    mobilityFactory = (MobilityFactory) 
      domain.getFactory("mobility");
    if (mobilityFactory == null) {
      if (log.isWarnEnabled()) {
        log.warn(
          "Unable to obtain the agent mobility domain"+
          " (\"mobility\"), please check your "+
          "LDMDomains.ini configuration.");
      }
      // okay, will fail most mobility requests
    }
    getServiceBroker().releaseService(
        this, DomainService.class, domain);

    if (isNode) {
      // get control of the node
      nodeControlService = (NodeControlService)
        getServiceBroker().getService(
            this,
            NodeControlService.class,
            null);
      if (nodeControlService == null) {
        throw new RuntimeException(
            "Unable to obtain node-control service");
      }
      this.nodeSB = nodeControlService.getRootServiceBroker();
      this.agentContainer = (AgentContainer) 
        nodeControlService.getRootContainer();

      // get the topology service
      topologyReaderService = (TopologyReaderService)
        getServiceBroker().getService(
            this,
            TopologyReaderService.class,
            null);
      if (topologyReaderService == null) {
        throw new RuntimeException(
            "Unable to obtain the topology-reader service");
      }
    }

    if (log.isDebugEnabled()) {
      log.debug("Loaded");
    }
  }

  public void start() {
    super.start();

    if (isNode) {
      if (log.isDebugEnabled()) {
        log.debug("Starting agent-mobility support for "+nodeId);
      }

      if (mobileAgentSP == null) {
        this.mobileAgentSP = new MobileAgentServiceProviderImpl();
        nodeSB.addService(MobileAgentService.class, mobileAgentSP);

        if (log.isDebugEnabled()) {
          log.debug("Added mobile agent registry service for "+nodeId);
        }
      } else {
        if (log.isErrorEnabled()) {
          log.error(
              "Mobile Agent registry service already created? "+
              mobileAgentSP);
        }
      }
    }
  }

  public void unload() {
    if (isNode) {
      if (mobileAgentSP != null) {
        nodeSB.revokeService(MobileAgentService.class, mobileAgentSP);
        mobileAgentSP = null;
      }
      if (topologyReaderService != null) {
        getServiceBroker().releaseService(
            this, TopologyReaderService.class, topologyReaderService);
        topologyReaderService = null;
      }
      if (nodeControlService != null) {
        getServiceBroker().releaseService(
            this, NodeControlService.class, nodeControlService);
        nodeControlService = null;
      }
    }
    if ((log != null) &&
        (log != LoggingService.NULL)) {
      getServiceBroker().releaseService(
          this, LoggingService.class, log);
      log = LoggingService.NULL;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
    // subscribe to control requests that we'll execute
     controlSub = (IncrementalSubscription)
       blackboard.subscribe(CONTROL_PRED);
    
    if (isNode) {
      // subscribe to transfers that we'll generate
      transferSub = (IncrementalSubscription)
        blackboard.subscribe(TRANSFER_PRED);

      if (blackboard.didRehydrate()) {
        if (log.isWarnEnabled()) {
          log.warn(
              "Node rehydration for agent mobility is not supported");
        }
      }
    }
  }

  protected void execute() {
    if (log.isDebugEnabled()) {
      log.debug("Execute");
    }

    if (isNode) {
      // fire pending blackboard changes
      fireAll();

      // watch transfer objects
      if (transferSub.hasChanged()) {
        // adds
        Enumeration en = transferSub.getAddedList();
        while (en.hasMoreElements()) {
          AgentTransfer transfer = (AgentTransfer) en.nextElement();
          addedAgentTransfer(transfer);
        }
        // changes
        en = transferSub.getChangedList();
        while (en.hasMoreElements()) {
          AgentTransfer transfer = (AgentTransfer) en.nextElement();
          changedAgentTransfer(transfer);
        }
        // removes
        en = transferSub.getRemovedList();
        while (en.hasMoreElements()) {
          AgentTransfer transfer = (AgentTransfer) en.nextElement();
          removedAgentTransfer(transfer);
        }
      }
    }

    // watch control requests
    if(controlSub.hasChanged()) {
      // adds
      Enumeration en = controlSub.getAddedList();
      while (en.hasMoreElements()) {
        AgentControl control = (AgentControl) en.nextElement();
        addedAgentControl(control);
      }
      // changes
      en = controlSub.getChangedList();
      while (en.hasMoreElements()) {
        AgentControl control = (AgentControl) en.nextElement();
        changedAgentControl(control);
      }
      // removes
      en = controlSub.getRemovedList();
      while (en.hasMoreElements()) {
        AgentControl control = (AgentControl) en.nextElement();
        removedAgentControl(control);
      }
    }
  }

  protected AgentControl findAgentControl(UID controlUID) {
    return (AgentControl) query(controlSub, controlUID);
  }


  /** control request for a local agent. */
  protected abstract void addedAgentControl(AgentControl control);

  /** a control was changed. */
  protected abstract void changedAgentControl(AgentControl control);

  /** a control was removed. */
  protected abstract void removedAgentControl(AgentControl control);

  /** arrival of a controled agent. */
  protected abstract void addedAgentTransfer(AgentTransfer transfer);

  /** response to a control of a local agent. */
  protected abstract void changedAgentTransfer(AgentTransfer transfer);

  /** removal of either the source-side or target-side transfer */
  protected abstract void removedAgentTransfer(AgentTransfer transfer);

  /** an agent registers as a mobile agent in the local node. */
  protected abstract void registerAgent(
      MessageAddress id, 
      ComponentDescription desc, 
      Agent agent);

  /** an agent unregisters itself from the local mobility registry. */
  protected abstract void unregisterAgent(
      MessageAddress id);


  // more node-only code:

  protected void queue(Runnable r) {
    // run in a separate thread
    //
    // FIXME use thread service!
    Thread t = new Thread(r, r.toString());
    t.start();
  }

  protected AgentTransfer createAgentTransfer(
      UID controlUID,
      AbstractTicket ticket,
      StateTuple state) {
    if (mobilityFactory == null) {
      throw new RuntimeException(
          "The agent mobility domain is not available on node "+
          nodeId);
    }
    return
      mobilityFactory.createAgentTransfer(
          controlUID,
          ticket,
          state);
  }

  protected void fireLater(Runnable r) {
    synchronized (todo) {
      todo.add(r);
    }
    blackboard.signalClientActivity();
  }


  private static UniqueObject query(
      CollectionSubscription sub, UID uid) {
    Collection real = sub.getCollection();
    int n = real.size();
    if (n > 0) {
      for (Iterator iter = real.iterator(); iter.hasNext(); ) {
        Object o = iter.next();
        if (o instanceof UniqueObject) {
          UniqueObject uo = (UniqueObject) o;
          UID x = uo.getUID();
          if (uid.equals(x)) {
            return uo;
          }
        }
      }
    }
    return null;
  }

  private void fireAll() {
    int n;
    List l;
    synchronized (todo) {
      n = todo.size();
      if (n <= 0) {
        return;
      }
      l = new ArrayList(todo);
      todo.clear();
    }
    for (int i = 0; i < n; i++) {
      Runnable r = (Runnable) l.get(i);
      r.run();
    }
  }
  
  private class MobileAgentServiceProviderImpl
  implements ServiceProvider {

    // single dummy service instance
    private final MobileAgentService SINGLE_SERVICE_INSTANCE =
      new MobileAgentService() {
      };

    public Object getService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Request for mobile agent registry service by "+
            requestor);
      }
      // check service class
      if (serviceClass != MobileAgentService.class) {
        throw new IllegalArgumentException(
            "Unsupported service "+serviceClass);
      }
      // assert that the requestor is an agent
      if (!(requestor instanceof Agent)) {
        throw new RuntimeException(
            "Expecting an Agent requestor, not "+requestor);
      }
      Agent agent = (Agent) requestor;
      MessageAddress id = agent.getAgentIdentifier();

      // get the agent's description from its container
      ComponentDescription desc = 
        agentContainer.getAgentDescription(id);
      if (desc == null) {
        throw new RuntimeException(
            "Unable to get agent \""+id+"\"'s ComponentDescription"+
            " from the agent container ("+agentContainer+")");
      }

      registerAgent(id, desc, agent);

      // create a dummy service instance
      return SINGLE_SERVICE_INSTANCE;
    }

    public void releaseService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass, 
        Object service)  {
      // check service instance
      if (service != SINGLE_SERVICE_INSTANCE) {
        throw new IllegalArgumentException(
            "Wrong service instance "+service);
      }
      Agent agent = (Agent) requestor;
      MessageAddress id = agent.getAgentIdentifier();
      unregisterAgent(id);
    }
  }
}

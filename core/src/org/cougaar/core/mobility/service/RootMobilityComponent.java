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

import java.util.*;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.*;
import org.cougaar.core.mobility.*;
import org.cougaar.core.mobility.arch.*;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.util.GenericStateModel;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * Node-level implementation of the mobility service.
 *
 * Requestor must be an Agent.
 */
public class RootMobilityComponent 
extends Agent // extends GenericStateModelAdapter    // BIG HACK!!!
implements Component 
{

  // HACK!  AgentManager currently forces all children to 
  //   be agents (w/ AgentBinder)!
  public MessageAddress getAgentIdentifier() {
    return null;
  }
  public org.cougaar.util.ConfigFinder getConfigFinder() {
    return null;
  }

  // HACK! FIXME!  send/recv messages via the node's MTS!
  //
  // see bug 1146
  private MessageTransportService nodeMTS;

  // HACK! FIXME!  add/remove via node's container!
  //
  // should use ContainmentService, pending node-agent.
  private AgentContainer agentContainer;



  private ServiceBroker sb;

  private LoggingService log;

  // local node address
  private MessageAddress nodeId;

  // map of agents waiting for ACK/NACK transfer response
  private final Map rootPendingMap = new HashMap(5);

  // registry for intra-agent listeners
  private final MobilityListenerRegistry mlReg = 
    new MobilityListenerRegistry();

  private RootMobilityListenerServiceProviderImpl rootMLSP;
  private RootMobilityDispatchServiceProviderImpl rootMDSP;


  /***   BEGIN HACK   ***/

  public RootMobilityComponent(Object o) {

    // FIXME
    // node needs direct pointer to this component, for
    // message passing, so node directly constructs the 
    // component instead of using a component description

    setParameter(o);
  }

  public void setParameter(Object o) {

    //
    // FIXME expect a list of [MTS, Agent-manager]
    //

    if (o instanceof List) {
      List l = (List) o;
      Object o1 = l.get(0);
      Object o2 = l.get(1);
      if ((o1 == null) || (o2 == null)) {
        throw new RuntimeException("null list entry?");
      }
      this.nodeMTS = (MessageTransportService) o1;
      this.agentContainer = (AgentContainer) o2;
    }
  }

  public void provideServices(ServiceBroker theSB) {
    if (rootMLSP != null) {
      theSB.addService(MobilityListenerService.class, rootMLSP);
    }
    if (rootMDSP != null) {
      theSB.addService(MobilityDispatchService.class, rootMDSP);
    }
  }

  public void receiveMessage(Message m) {

    // this isn't so bad, assuming that the node is 
    //   hard-coded to call this method for every message 
    //   received

    if (!(m instanceof MobilityMessage)) {
      return;
    }
    MobilityMessage mm = (MobilityMessage) m;

    MobilitySupport support = 
      new MobilitySupportImpl(
          mm.getOriginator(),
          mm.getAgentIdentifier(),
          mm.getTicket());

    AbstractHandler h;
    if (m instanceof TransferMessage) {
      TransferMessage tm = (TransferMessage) m;
      h = 
        new ArrivalHandler(
            support,
            tm.getStateTuple());
    } else if (m instanceof AckMessage) {
      AckMessage am = (AckMessage) m;
      h = new AckHandler(support);
    } else if (m instanceof NackMessage) {
      NackMessage nm = (NackMessage) m;
      h =
        new NackHandler(
            support,
            nm.getThrowable());
    } else {
      return;
    }

    // add to work queue
    queue(h);
  }

  /***   END HACK   ***/



  public void setBindingSite(BindingSite bs) {
    // only care about the service broker
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    // get logger
    log = (LoggingService) 
      sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    if (log.isDebugEnabled()) {
      log.debug("Loading root mobility component");
    }

    // get this node's id
    NodeIdentificationService nis = (NodeIdentificationService)
      sb.getService(this, NodeIdentificationService.class, null);
    if (nis != null) {
      this.nodeId = nis.getNodeIdentifier();
      sb.releaseService(this, NodeIdentificationService.class, nis);
    }
    if (nodeId == null) {
      if (log.isErrorEnabled()) {
        log.error("Unable to get local Node's Id!");
      }
    }

    // get the node-level containment service
    // FIXME pending node-agent

    // get the node-level MTS service
    // FIXME see bug 1146
  }

  public void start() {
    super.start();

    if (log.isDebugEnabled()) {
      log.debug("Starting agent-mobility support for "+nodeId);
    }

    if (rootMDSP != null) {
      if (log.isErrorEnabled()) {
        log.error(
          "Root mobility service already created? "+rootMDSP);
      }
    }

    this.rootMLSP = new RootMobilityListenerServiceProviderImpl();
    sb.addService(MobilityListenerService.class, rootMLSP);

    if (log.isDebugEnabled()) {
      log.debug("Added mobility listener service for "+nodeId);
    }

    this.rootMDSP = new RootMobilityDispatchServiceProviderImpl();
    sb.addService(MobilityDispatchService.class, rootMDSP);

    if (log.isDebugEnabled()) {
      log.debug("Added mobility dispatch service for "+nodeId);
    }

  }

  public void unload() {
    if (rootMDSP != null) {
      sb.revokeService(MobilityDispatchService.class, rootMDSP);
      rootMDSP = null;
    }

    if (rootMLSP != null) {
      sb.revokeService(MobilityListenerService.class, rootMLSP);
      rootMLSP = null;
    }

    if ((log != null) &&
        (log != LoggingService.NULL)) {
      sb.releaseService(this, LoggingService.class, log);
      log = LoggingService.NULL;
    }
    super.unload();
  }

  private class RootMobilityListenerServiceProviderImpl
  implements ServiceProvider {

    public Object getService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Request for mobility listener service by "+
            requestor);
      }
      // check service class
      if (serviceClass != MobilityListenerService.class) {
        throw new IllegalArgumentException(
            this+" does not provide a service for: "+
            serviceClass);
      }
      if (!(requestor instanceof MobilityListener)) {
        throw new IllegalArgumentException(
            this+" only accepts requestors that implement "+
            "the MobilityListener API, not "+requestor);
      }
      MobilityListener listener = (MobilityListener) requestor;
      MessageAddress id = listener.getAddress();
      // register the listener
      Object key = mlReg.add(id, listener);
      // create a new service instance
      return new MobilityListenerServiceImpl(id, key, listener);
    }

    public void releaseService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass, 
        Object service)  {
      // check service instance
      if (!(service instanceof MobilityListenerServiceImpl)) {
        throw new IllegalArgumentException(
            this+" unable to release service: "+
            ((service != null) ? service.getClass().toString() : "null"));
      }
      MobilityListenerServiceImpl mlsi = (MobilityListenerServiceImpl) service;
      // check requestor
      if (requestor != mlsi.listener) {
        if (log.isErrorEnabled()) {
          log.error(
              this+" release of service by different requestor? "+
              requestor+" != "+mlsi.listener);
        }
      }
      // remove the listener
      mlReg.remove(mlsi.id, mlsi.key);
    }

    public String toString() {
      return "root "+nodeId+" mobility-listener-service provider";
    }

    private class MobilityListenerServiceImpl
    implements MobilityListenerService {
      private final MessageAddress id;
      private final Object key;
      private final MobilityListener listener;
      public MobilityListenerServiceImpl(
          MessageAddress id, Object key, MobilityListener listener) {
        this.id = id;
        this.key = key;
        this.listener = listener;
      }
      public String toString() {
        return "mobility listener service for "+id;
      }
    }
  }

  private class RootMobilityDispatchServiceProviderImpl
  implements ServiceProvider {

    public Object getService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Request for mobility dispatch service by "+
            requestor);
      }
      // check service class
      if (serviceClass != MobilityDispatchService.class) {
        throw new IllegalArgumentException(
            this+" does not provide a service for: "+
            serviceClass);
      }
      // assert that the requestor is an agent
      if (!(requestor instanceof Agent)) {
        throw new RuntimeException(
            this+" only allows Agent requestors, not: "+requestor);
      }
      Agent agent = (Agent) requestor;
      MessageAddress id = agent.getAgentIdentifier();
      // FIXME get the agent's description from its container
      ComponentDescription desc = 
        agentContainer.getAgentDescription(id);
      if (desc == null) {
        throw new RuntimeException(
            "Unable to get agent \""+id+"\"'s ComponentDescription"+
            " from the agent container ("+agentContainer+")");
      }
      // FIXME assume that the agent itself provides the state
      StateObject stateProvider =
        ((agent instanceof StateObject) ?
         ((StateObject) agent) :
         (null));
      // FIXME assume that the itself agent regulates the 
      //   state-model
      GenericStateModel model = agent;
      // create a new service instance
      return new MobilityDispatchServiceImpl(
          id, desc, stateProvider, model);
    }

    public void releaseService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass, 
        Object service)  {
      // check service instance
      if (!(service instanceof MobilityDispatchServiceImpl)) {
        throw new IllegalArgumentException(
            this+" unable to release service: "+
            ((service != null) ? service.getClass().toString() : "null"));
      }
      MobilityDispatchServiceImpl mdsi = (MobilityDispatchServiceImpl) service;
      // check requestor
      // check for pending moves
      // done
    }

    private class MobilityDispatchServiceImpl
    implements MobilityDispatchService {

      private final MessageAddress id;
      private final ComponentDescription desc;
      private final StateObject stateProvider;
      private final GenericStateModel model;

      public MobilityDispatchServiceImpl(
          MessageAddress id,
          ComponentDescription desc,
          StateObject stateProvider,
          GenericStateModel model) {
        this.id = id;
        this.desc = desc;
        this.stateProvider = stateProvider;
        this.model = model;
      }
      public void dispatch(Ticket ticket) {
        if (ticket == null) {
          throw new IllegalArgumentException(
              "Null mobility ticket!");
        }
        MobilitySupport support = 
          new MobilitySupportImpl(
              nodeId, id, ticket);
        MessageAddress destNode = ticket.getDestinationNode();
        AbstractHandler h;
        if ((destNode == null) ||
            (destNode.equals(nodeId))) {
          if (ticket.isForceRestart()) {
            h = new DispatchTestHandler(
                support, model, desc, stateProvider);
          } else {
            h = new DispatchNoopHandler(support);
          }
        } else {
          h = new DispatchRemoteHandler(
              support, model, desc, stateProvider);
        }
        queue(h);
      }
      public String toString() {
        return "mobility listener service for "+id;
      }
    }
  }

  private void queue(AbstractHandler h) {
    // run in a separate thread
    //
    // FIXME use thread service!
    Thread t = new Thread(h, h.toString());
    t.start();
  }


  private class MobilitySupportImpl 
    extends AbstractMobilitySupport {

      public MobilitySupportImpl(
          MessageAddress sender,
          MessageAddress id,
          Ticket ticket) {
        super(
            sender,
            id, 
            RootMobilityComponent.this.nodeId, 
            ticket, 
            RootMobilityComponent.this.log,
            RootMobilityComponent.this.mlReg);
      } 
      
      protected void sendMessage(Message message) {
        nodeMTS.sendMessage(message);
      }

      protected PendingEntry putPendingEntry(PendingEntry pe) {
        synchronized (rootPendingMap) {
          return (PendingEntry) rootPendingMap.put(id, pe);
        }
      }

      protected PendingEntry removePendingEntry() {
        synchronized (rootPendingMap) {
          return (PendingEntry) rootPendingMap.remove(id);
        }
      }

      public void addAgent(StateTuple tuple) {
        agentContainer.addAgent(id, tuple);
      }

      public void removeAgent() {
        agentContainer.removeAgent(id);
      }

    }

  public String toString() {
    return "mobility service provider for "+nodeId;
  }
}

/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.topology;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.TopologyEntry; // inlined
import org.cougaar.core.service.TopologyWriterService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component creates and maintains the node-level
 * TopologyWriterService.
 *
 * @see TopologyWriterService just for the agents to modify the 
 *     topology
 */
public final class TopologyWriterServiceComponent 
extends GenericStateModelAdapter
implements Component 
{

  private ServiceBroker sb;

  private String localenclave;
  private String localsite;
  private String localhost;
  private String localnode;
  private NamingService namingService;
  private NodeIdentificationService nodeIdService;
  private LoggingService log;

  private TopologyWriterServiceProviderImpl topologyWSP;

  public void setBindingSite(BindingSite bs) {
    // only care about the service broker
    //this.sb = bs.getServiceBroker();
  }

  public void setNodeControlService(NodeControlService ncs) {
    this.sb = ncs.getRootServiceBroker();
  }

  public void load() {
    super.load();

    // FIXME get these values from system properties?
    this.localenclave = "enclave";
    this.localsite = "site";

    try {
      InetAddress localAddr = InetAddress.getLocalHost();
      this.localhost =  localAddr.getHostName();
    } catch (java.net.UnknownHostException ex) {
      throw new RuntimeException(
          "Unable to lookup localhost", ex);
    }

    this.log = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    nodeIdService = (NodeIdentificationService)
      sb.getService(this, NodeIdentificationService.class, null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to obtain node-id service");
    }
    MessageAddress nodeAddr = nodeIdService.getMessageAddress();
    this.localnode = 
      ((nodeAddr != null) ? nodeAddr.getAddress() : null);
    if (localnode == null) {
      throw new RuntimeException(
          "Node name is null");
    }

    this.namingService = (NamingService)
      sb.getService(this, NamingService.class, null);
    if (namingService == null) {
      throw new RuntimeException(
          "Unable to obtain naming service");
    }

    // create and advertise our service
    this.topologyWSP = new TopologyWriterServiceProviderImpl();
    sb.addService(TopologyWriterService.class, topologyWSP);
  }

  public void unload() {
    // clean up ns?
    // revoke our service
    if (topologyWSP != null) {
      sb.revokeService(TopologyWriterService.class, topologyWSP);
      topologyWSP = null;
    }
    // release all services
    if (namingService != null) {
      sb.releaseService(this, NamingService.class, namingService);
      namingService = null;
    }
    if (nodeIdService != null) {
      sb.releaseService(this, NodeIdentificationService.class, nodeIdService);
      nodeIdService = null;
    }
    if ((log != null) && (log != LoggingService.NULL)) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
    super.unload();
  }

  private class TopologyWriterServiceProviderImpl
    implements ServiceProvider {

      private final TopologyWriterServiceImpl topologyWS;

      public TopologyWriterServiceProviderImpl() {
        // keep only one instance
        topologyWS = new TopologyWriterServiceImpl();
      }

      public Object getService(
          ServiceBroker sb, 
          Object requestor, 
          Class serviceClass) {
        if (serviceClass == TopologyWriterService.class) {
          return topologyWS;
        } else {
          return null;
        }
      }

      public void releaseService(
          ServiceBroker sb, 
          Object requestor, 
          Class serviceClass, 
          Object service) {
      }
    }

  private class TopologyWriterServiceImpl 
    implements TopologyWriterService {

      public TopologyWriterServiceImpl() {
        // cache "ensureTopologyContext()" ?
	  try {
	      ensureTopologyContext();
	  } catch (javax.naming.NamingException name_ex) {
	      throw new RuntimeException(name_ex);
	  }
      }

      public void createAgent(
          String agent,
          int type,
          long newIncarnation,
          long newMoveId,
          int newStatus) {
        // FIXME add verification
        if (agent == null) {
          throw new IllegalArgumentException(
              "Null agent name");
        }
        if ((localnode.equals(agent)) !=
            (type == TopologyEntry.NODE_AGENT_TYPE)) {
          throw new IllegalArgumentException(
              "Incorrect type ("+type+") for agent "+
              agent+" on node "+localnode);
        }
        Attributes ats = 
          createAttributes(
              agent,
              type,
              newIncarnation,
              newMoveId,
              newStatus);
        try {
          register(agent, ats);
        } catch (NamingException e) {
          throw new RuntimeException(
              "Unable to add agent \""+agent+"\"", e);
        }
      }

      public void updateAgent(
          String agent, 
          int assertType,
          long assertIncarnation, 
          long newMoveId,
          int newStatus,
          long assertMoveId) {
        // FIXME add verification
        createAgent(
            agent,
            assertType,
            assertIncarnation,
            newMoveId,
            newStatus);
      }


      public void removeAgent(String agent) {
        try {
          unregister(agent);
        } catch (NamingException e) {
          throw new RuntimeException(
              "Unable to add agent \""+agent+"\"", e);
        }
      }

      private Attributes createAttributes(
          String agent,
          int type,
          long incarnation,
          long moveId,
          int status) {

        Attributes ats = new BasicAttributes();
        ats.put(
            TopologyNamingConstants.AGENT_ATTR, 
            agent);
        ats.put(
            TopologyNamingConstants.NODE_ATTR, 
            localnode);
        ats.put(
            TopologyNamingConstants.HOST_ATTR, 
            localhost);
        ats.put(
            TopologyNamingConstants.SITE_ATTR, 
            localsite);
        ats.put(
            TopologyNamingConstants.ENCLAVE_ATTR,
            localenclave);
        ats.put(
            TopologyNamingConstants.INCARNATION_ATTR, 
            (new Long(incarnation)).toString());
        ats.put(
            TopologyNamingConstants.MOVE_ID_ATTR, 
            (new Long(moveId)).toString());
        ats.put(
            TopologyNamingConstants.TYPE_ATTR, 
            (new Integer(type)).toString());
        ats.put(
            TopologyNamingConstants.STATUS_ATTR, 
            (new Integer(status)).toString());

        return ats;
      }

      private void unregister(String agent) throws NamingException {
        DirContext ctx = ensureTopologyContext();
        ctx.unbind(agent);
      }

      private void register(
          String agent, Attributes ats) throws NamingException {
        DirContext ctx = ensureTopologyContext();
        // value is message-address for backwards compatibility
        MessageAddress value = MessageAddress.getMessageAddress(agent);
        ctx.rebind(agent, value, ats);
      }

      private DirContext ensureTopologyContext() throws NamingException {
        DirContext ctx = namingService.getRootContext();
        try {
          ctx = (DirContext) 
            ctx.lookup(
                TopologyNamingConstants.TOPOLOGY_DIR);
        } catch (NamingException ne) {
          ctx = (DirContext) 
            ctx.createSubcontext(
                TopologyNamingConstants.TOPOLOGY_DIR, 
                new BasicAttributes());
        } catch (Exception e) {
          NamingException x = 
            new NamingException(
                "Unable to access name-server");
          x.setRootCause(e);
          throw x;
        }
        return ctx;
      }
    }

}

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
package org.cougaar.core.node;

import java.io.Serializable;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.identity.AgentIdentityClient;
import org.cougaar.core.service.identity.AgentIdentityService;
import org.cougaar.core.service.identity.TransferableIdentity;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * Default implementation of the AgentIdentityService, which is
 * used if the node configuration doesn't include a high-priority
 * component which provides the AgentIdentityService.
 */
public class DefaultAgentIdentityComponent
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private LoggingService log;

  private MessageAddress thisNode;

  private ServiceProvider myAISP;

  public void setBindingSite(BindingSite bs) {
    // only care about the root-level service broker
  }

  public void setNodeControlService(NodeControlService ncs) {
    this.sb = ncs.getRootServiceBroker();
  }

  public void setParameter(Object o) {
    throw new UnsupportedOperationException(
        "Default agent-identity service provider"+
        " not expecting a parameter: "+
        ((o != null) ? o.getClass().getName() : "null"));
  }

  public void load() {
    super.load();

    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    if (sb.hasService(AgentIdentityService.class)) {
      // already have an agent-id service!
      //
      // leave the existing service in place
      if (log.isInfoEnabled()) {
        log.info(
            "Not loading the default agent-identity service");
      }
      if (log != LoggingService.NULL) {
        sb.releaseService(this, LoggingService.class, log);
        log = null;
      }
      return;
    }

    // get our node's address
    NodeIdentificationService nodeIdService = 
      (NodeIdentificationService)
      sb.getService(this, NodeIdentificationService.class, null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to get node-id service");
    }
    thisNode = nodeIdService.getMessageAddress();
    if (thisNode == null) {
      throw new RuntimeException(
          "Node address is null?");
    }
    sb.releaseService(
        this, NodeIdentificationService.class, nodeIdService);

    // advertise the default agent-id service
    myAISP = new DefaultAgentIdentityServiceProviderImpl();
    sb.addService(AgentIdentityService.class, myAISP);

    // maybe also advertise a control-service for testing
    // the identity-revoked handling
  }

  public void unload() {
    // revoke our service
    if (myAISP != null) {
      sb.revokeService(AgentIdentityService.class, myAISP);
      myAISP = null;
    }
    if ((log != null) && (log != LoggingService.NULL)) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
    super.unload();
  }

  private class DefaultAgentIdentityServiceProviderImpl
    implements ServiceProvider {

      public Object getService(
          ServiceBroker sb,
          Object requestor,
          Class serviceClass) {
        if (AgentIdentityService.class.isAssignableFrom(serviceClass)) {
          AgentIdentityClient client = (AgentIdentityClient) requestor;
          return new DefaultAgentIdentityServiceImpl(client);
        } else {
          return null;
        }
      }

      public void releaseService(
          ServiceBroker sb,
          Object requestor,
          Class serviceClass,
          Object service)  {
        if (AgentIdentityService.class.isAssignableFrom(serviceClass)) {
          AgentIdentityClient client = (AgentIdentityClient) requestor;
          DefaultAgentIdentityServiceImpl dais =
            (DefaultAgentIdentityServiceImpl) service;
          dais.onServiceRelease(client);
        }
      }

      private class DefaultAgentIdentityServiceImpl 
        implements AgentIdentityService {

          private final AgentIdentityClient client;
          private final String name;
          private boolean hasAcquired;

          public DefaultAgentIdentityServiceImpl(
              AgentIdentityClient client) {
            this.client = client;
            this.name = client.getName();
            if (name == null) {
              throw new IllegalArgumentException(
                  "Agent-id client has null name");
            }
          }

          public void acquire(TransferableIdentity id) {
            if (hasAcquired) {
              throw new IllegalStateException(
                  "Already acquired identity for "+name+
                  ", can't aquire an additional "+
                  ((id != null) ?
                   ("transfered "+id) :
                   ("new id")));
            }
            if (id != null) {
              if (!(id instanceof DefaultTransferableIdentity)) {
                throw new IllegalArgumentException(
                    "Default agent-identity service provider doesn't "+
                    "support transferable identities of type "+
                    id.getClass().getName());
              }
              DefaultTransferableIdentity dtid =
                (DefaultTransferableIdentity) id;
              if (!(name.equals(dtid.getName()))) {
                throw new IllegalArgumentException(
                    "Transfered identity name "+dtid.getName()+
                    " doesn't match client name "+name);
              }
              if (thisNode.equals(dtid.getTargetNode())) {
                // okay transfer
              } else if (thisNode.equals(dtid.getSourceNode())) {
                // okay failed transfer rollback
              } else {
                throw new IllegalArgumentException(
                    "Transfered identity for "+name+
                    " arrived at node ("+thisNode+
                    ") doesn't match expected source ("+
                    dtid.getSourceNode()+") or target ("+
                    dtid.getTargetNode()+") node");
              }
            }
            hasAcquired = true;
          }

          public void release() {
            if (!(hasAcquired)) {
              if (log.isErrorEnabled()) {
                log.error("Never acquired identity for "+name);
              }
            }
            hasAcquired = false;
          }

          public TransferableIdentity transferTo(
              MessageAddress targetNode) {
            if (!(hasAcquired)) {
              throw new IllegalStateException(
                  "Never acquired identity for "+name);
            }
            TransferableIdentity id =
              new DefaultTransferableIdentity(
                  name, thisNode, targetNode);
            hasAcquired = false;
            return id;
          }

          private void onServiceRelease(
              AgentIdentityClient releaseClient) {
            if (hasAcquired) {
              if (log.isErrorEnabled()) {
                log.error(
                    "Never released or transfered agent identity "+
                    " for "+name);
              }
            }
            if (releaseClient != client) {
              if (log.isErrorEnabled()) {
                log.error(
                    "Agent identity for "+name+
                    " released by "+releaseClient+
                    ", not the original requestor "+client);
              }
            }
          }

        }
    }

  private static final class DefaultTransferableIdentity
    implements TransferableIdentity, Serializable {

      private final String name;
      private final MessageAddress sourceNode;
      private final MessageAddress targetNode;

      public DefaultTransferableIdentity(
          String name,
          MessageAddress sourceNode,
          MessageAddress targetNode) {
        this.name = name;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        if ((name == null) ||
            (sourceNode == null) ||
            (targetNode == null)) {
          throw new IllegalArgumentException(
              "Null name/source/target");
        }
      }

      public String getName() { 
        return name;
      }
      public MessageAddress getSourceNode() { 
        return sourceNode;
      }
      public MessageAddress getTargetNode() { 
        return targetNode;
      }

      public String toString() {
        return 
          "Transferable identity for "+name+
          " from "+sourceNode+" to "+targetNode;
      }
    }
}

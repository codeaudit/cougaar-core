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

package org.cougaar.core.agent;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.NullService;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component blocks the NodeControlService if the agent's
 * address does not match the node's address, and is typically
 * one of the first component loaded in all agents.
 */
public final class NodeControlBlocker
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private ServiceProvider ncsp;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    MessageAddress localAgent = find_local_agent();
    MessageAddress localNode = find_local_node();
    boolean isNode = 
      (localAgent == null ||
       localAgent.equals(localNode));

    if (!isNode) {
      // block the NodeControlService!
      ncsp = new BlockSP();
      if (!sb.addService(NodeControlService.class, ncsp)) {
        throw new RuntimeException("Unable to block NodeControlService");
      }

      // verify
      NodeControlService ncs = (NodeControlService)
       sb.getService(this, NodeControlService.class, null); 
      if (ncs != null) {
        throw new RuntimeException("Could not block NodeControlService");
      }
    }
  }

  public void unload() {
    super.unload();

    if (ncsp != null) {
      sb.revokeService(NodeControlService.class, ncsp);
      ncsp = null;
    }
  }

  private MessageAddress find_local_agent() {
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (ais == null) {
      return null;
    }
    MessageAddress ret = ais.getMessageAddress();
    sb.releaseService(
        this, AgentIdentificationService.class, ais);
    return ret;
  }

  private MessageAddress find_local_node() {
    NodeIdentificationService nis = (NodeIdentificationService)
      sb.getService(this, NodeIdentificationService.class, null);
    if (nis == null) {
      return null;
    }
    MessageAddress ret = nis.getMessageAddress();
    sb.releaseService(
        this, NodeIdentificationService.class, nis);
    return ret;
  }

  private static final class BlockSP
    implements ServiceProvider {
      // we are not required to implement our service API if the
      // instance implements NullService
      private final Service NULL = new NullService() {};

      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (NodeControlService.class.isAssignableFrom(serviceClass)) {
          return NULL; // service blocker!
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

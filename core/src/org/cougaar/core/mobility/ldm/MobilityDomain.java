/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.core.mobility.ldm;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import org.cougaar.core.agent.ClusterServesLogicProvider;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.domain.DomainAdapter;
import org.cougaar.core.domain.DomainBindingSite;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.UIDService;

/**
 * The mobility domain just has a factory, with no LPs.
 */
public class MobilityDomain extends DomainAdapter {

  private static final String MOBILTY_NAME = "mobility";

  public String getDomainName() {
    return MOBILTY_NAME;
  }

  public Collection getAliases() {
    return Collections.singleton(getDomainName());
  }

  protected void loadFactory() {
    UIDService uidService = getUIDService();
    MessageAddress nodeId = getNodeId();
    MessageAddress agentId = getAgentId();
    MobilityFactory f = new MobilityFactoryImpl(uidService, nodeId, agentId);

    setFactory(f);
  }

  protected void loadXPlan() {
    // no xplan
  }

  private UIDService getUIDService() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    UIDService uidService = 
      (UIDService)
      sb.getService(
          this,
          UIDService.class,
          null);
    if (uidService == null) {
      throw new RuntimeException(
          "Unable to obtain uid service");
    }
    return uidService;
  }

  private MessageAddress getAgentId() {
    // get the agentId
    ServiceBroker sb = getBindingSite().getServiceBroker();
    AgentIdentificationService agentIdService = 
      (AgentIdentificationService)
      sb.getService(
          this,
          AgentIdentificationService.class,
          null);
    if (agentIdService == null) {
      throw new RuntimeException(
          "Unable to obtain node-id service");
    }
    MessageAddress agentId = agentIdService.getMessageAddress();
    sb.releaseService(
        this, AgentIdentificationService.class, agentIdService);
    if (agentId == null) {
      throw new RuntimeException(
          "Unable to obtain agent id");
    }
    return agentId;
  }

  private MessageAddress getNodeId() {
    // get the nodeId
    ServiceBroker sb = getBindingSite().getServiceBroker();
    NodeIdentificationService nodeIdService = 
      (NodeIdentificationService)
      sb.getService(
          this,
          NodeIdentificationService.class,
          null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to obtain node-id service");
    }
    MessageAddress nodeId = nodeIdService.getMessageAddress();
    sb.releaseService(
        this, NodeIdentificationService.class, nodeIdService);
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node id");
    }
    return nodeId;
  }

  // zero LPs
  protected void loadLPs() {
  }
  public void invokeMessageLogicProviders(DirectiveMessage message) {
    return;
  }
  public void invokeEnvelopeLogicProviders(
      EnvelopeTuple tuple, boolean isPersistenceEnvelope) {
    return;
  }
  public void invokeRestartLogicProviders(MessageAddress cid) {
    return;
  }

}

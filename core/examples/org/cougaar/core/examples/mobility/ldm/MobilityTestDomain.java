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

package org.cougaar.core.examples.mobility.ldm;

import java.util.*;

import org.cougaar.core.agent.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.domain.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.UIDService;

/**
 * The mobility test domain ("mobilityTest") just has a 
 * factory, with no LPs.
 */
public class MobilityTestDomain extends DomainAdapter {

  private static final String MOBILTY_TEST_NAME = "mobilityTest";

  private MessageAddress self;
  private AgentIdentificationService agentIdService;
  private UIDService uidService;

  public String getDomainName() {
    return MOBILTY_TEST_NAME;
  }

  public Collection getAliases() {
    return Collections.singleton(getDomainName());
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    this.self = ais.getMessageAddress();
  }

  public void setUIDService(UIDService uidService) {
    this.uidService = uidService;
  }

  public void unload() {
    super.unload();
    ServiceBroker sb = getBindingSite().getServiceBroker();
    if (uidService != null) {
      sb.releaseService(this, UIDService.class, uidService);
      uidService = null;
    }
    if (agentIdService != null) {
      sb.releaseService(this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
  }

  protected void loadFactory() {
    Factory f = new MobilityTestFactoryImpl(self, uidService);
    setFactory(f);
  }

  protected void loadXPlan() {
    // none
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

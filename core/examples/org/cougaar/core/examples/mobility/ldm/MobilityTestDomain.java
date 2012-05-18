/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.examples.mobility.ldm;

import java.util.Collection;
import java.util.Collections;

import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.domain.DomainAdapter;
import org.cougaar.core.domain.Factory;
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

  @Override
public String getDomainName() {
    return MOBILTY_TEST_NAME;
  }

  public Collection getAliases() {
    return Collections.singleton(getDomainName());
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais != null) {
      this.self = ais.getMessageAddress();
    }
  }

  public void setUIDService(UIDService uidService) {
    this.uidService = uidService;
  }

  @Override
public void unload() {
    super.unload();
    ServiceBroker sb = getServiceBroker();
    if (uidService != null) {
      sb.releaseService(this, UIDService.class, uidService);
      uidService = null;
    }
    if (agentIdService != null) {
      sb.releaseService(this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
  }

  @Override
protected void loadFactory() {
    Factory f = new MobilityTestFactoryImpl(self, uidService);
    setFactory(f);
  }

  @Override
protected void loadXPlan() {
    // none
  }

  // zero LPs
  @Override
protected void loadLPs() {
  }
  @Override
public void invokeMessageLogicProviders(DirectiveMessage message) {
    return;
  }
  @Override
public void invokeEnvelopeLogicProviders(
      EnvelopeTuple tuple, boolean isPersistenceEnvelope) {
    return;
  }
  @Override
public void invokeRestartLogicProviders(MessageAddress cid) {
    return;
  }

}

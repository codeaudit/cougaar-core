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

package org.cougaar.core.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.cougaar.core.blackboard.ChangeEnvelopeTuple;
import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.RelayLP;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This is the "root" or infrastructure domain, defining the
 * objects and behavior shared by all COUGAAR-based systems.
 * <p>
 * The root domain only loads the "RelayLP".  User-defined
 * domains can load other logic providers and XPlans.
 * <p>
 * This implementation could use the DomainAdapter base class.
 * In this case we directly implement the Domain API for
 * efficiency and illustative purposes.
 */
public final class RootDomain
extends GenericStateModelAdapter
implements Component, Domain 
{
  private ServiceBroker serviceBroker;
  private DomainRegistryService domainRegistryService;
  private MessageAddress self;
  private RootPlan rootplan;
  private RelayLP relayLP;

  public void setParameter(Object o) {
    String domainName = (String) (((List) o).get(0));
    if (!getDomainName().equals(domainName)) {
      throw new IllegalArgumentException(
          "Invalid Domain name parameter - " +
          " specified " + domainName + 
          " should be " + getDomainName());
    }
  }

  public void setBindingSite(BindingSite bs) {
    this.serviceBroker = bs.getServiceBroker();
  }

  public void setAgentIdentificationService(
      AgentIdentificationService ais) {
    if (ais != null) {
      this.self = ais.getMessageAddress();
    }
  }

  public void load() {
    super.load();
    rootplan = new RootPlanImpl();
    relayLP = new RelayLP(rootplan, self);
    domainRegistryService = (DomainRegistryService)
      serviceBroker.getService(
          this, DomainRegistryService.class, null);
    if (domainRegistryService != null) {
      domainRegistryService.registerDomain(this);
    }
  }

  public void unload() {
    super.unload();
    if (domainRegistryService != null) {
      domainRegistryService.unregisterDomain(this);
      serviceBroker.releaseService(
          this, DomainRegistryService.class, domainRegistryService);
      domainRegistryService = null;
    }
  }

  public String getDomainName() {
    return "root";
  }

  public XPlan getXPlan() {
    return rootplan;
  }

  public Factory getFactory() {
    return null;
  }

  public void invokeMessageLogicProviders(DirectiveMessage message) {
    Directive[] directives = message.getDirectives();
    for (int index = 0; index < directives.length; index++) {
      Directive directive = directives[index];
      invokeMessageLogicProviders(directive);
    }
  }

  private void invokeMessageLogicProviders(Directive directive) {
    Collection changeReports = null;
    if (directive instanceof DirectiveMessage.DirectiveWithChangeReports) {
      DirectiveMessage.DirectiveWithChangeReports dd = 
        (DirectiveMessage.DirectiveWithChangeReports) directive;
      changeReports = dd.getChangeReports();
      directive = dd.getDirective();
    }
    relayLP.execute(directive, changeReports);
  }

  public void invokeEnvelopeLogicProviders(
      EnvelopeTuple tuple, boolean isPersistenceEnvelope) {
    if (isPersistenceEnvelope) {
      return;
    }
    Collection changeReports = null;
    if (tuple instanceof ChangeEnvelopeTuple) {
      changeReports = ((ChangeEnvelopeTuple) tuple).getChangeReports();
    }
    relayLP.execute(tuple, changeReports);
  }

  public void invokeRestartLogicProviders(MessageAddress cid) {
    relayLP.restart(cid);
  }

  public void invokeABAChangeLogicProviders(Set communities) {
    relayLP.abaChange(communities);
  }
}

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
import java.util.Iterator;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.relay.RelayLP;
import org.cougaar.core.service.AgentIdentificationService;

/**
 * This is the "root" or infrastructure domain, defining the
 * objects and behavior shared by all COUGAAR-based systems.
 * <p>
 * The root domain only loads the "RelayLP".  User-defined
 * domains can load other logic providers and XPlans.
 */
public class RootDomain extends DomainAdapter {

  public static final String ROOT_NAME = "root";

  private MessageAddress self;
  private AgentIdentificationService agentIdService;

  public String getDomainName() {
    return ROOT_NAME;
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    this.self = ais.getMessageAddress();
  }

  public void load() {
    super.load();
  }

  protected void loadFactory() {
    // no factory
  }

  protected void loadXPlan() {
    RootPlan rootPlan = new RootPlanImpl();
    setXPlan(rootPlan);
  }

  protected void loadLPs() {
    RootPlan rootplan = (RootPlan) getXPlan();
    addLogicProvider(new RelayLP(rootplan, self));
    // maybe include "ComplainingLP" as well...
  }
}

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

package org.cougaar.core.examples.mobility.ldm;

import java.util.*;

import org.cougaar.core.agent.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.domain.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.UIDServer;
import org.cougaar.planning.ldm.LDMServesPlugin;

/**
 * The mobility test domain ("mobilityTest") just has a 
 * factory, with no LPs.
 */
public class MobilityTestDomain extends DomainAdapter {

  private static final String MOBILTY_TEST_NAME = "mobilityTest";

  public String getDomainName() {
    return MOBILTY_TEST_NAME;
  }

  public Collection getAliases() {
    return Collections.singleton(getDomainName());
  }

  protected void loadFactory() {
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException(
          "Binding site for the domain has not be set.\n" +
          "Unable to initialize domain Factory without a binding site.");
    } 

    LDMServesPlugin ldm = bindingSite.getClusterServesLogicProvider().getLDM();
    MessageAddress self = ldm.getMessageAddress();
    UIDServer uidServer = ((ClusterContext) ldm).getUIDServer();

    Factory f = new MobilityTestFactoryImpl(self, uidServer);
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

/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.cougaar.core.agent.Agent; // inlined
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.wp.resolver.Resolver; // inlined

/**
 * This container holds the client-side WP resolver and
 * server WP authorities.
 * <p>
 * The job of this component is simply to decide which WP
 * components should be loaded and hold onto them.
 * <p>
 * This component should be inserted into the NodeAgent
 * before the MTS.
 */
public class WhitePages
extends ContainerSupport
{
  public static final String INSERTION_POINT = 
    Agent.INSERTION_POINT + ".WP";

  private LoggingService logger;
  private MessageAddress agentId;
  private AgentIdentificationService agentIdService;

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setAgentIdentificationService(
      AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais != null) {
      this.agentId = ais.getMessageAddress();
    }
  }

  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    List l = new ArrayList();

    // add defaults
    l.add(new ComponentDescription(
            "Resolver",
            Resolver.INSERTION_POINT,
            "org.cougaar.core.wp.resolver.Resolver",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));

    // read config
    ServiceBroker sb = getServiceBroker();
    ComponentInitializerService cis = (ComponentInitializerService)
      sb.getService(this, ComponentInitializerService.class, null);
    try {
      ComponentDescription[] descs =
        cis.getComponentDescriptions(
            agentId.toString(),
            specifyContainmentPoint());
      int n = (descs == null ? 0 : descs.length);
      for (int i = 0; i < n; i++) {
        l.add(descs[i]);
      }
    } catch (ComponentInitializerService.InitializerException cise) {
      if (logger.isInfoEnabled()) {
        logger.info("\nUnable to add "+agentId+"'s components", cise);
      }
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    }

    return new ComponentDescriptions(l);
  }

  public void unload() {
    super.unload();

    // release services
    ServiceBroker sb = getServiceBroker();
    if (agentIdService != null) {
      sb.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }
  }
}

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

package org.cougaar.core.wp.server;

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
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This is the server-side white pages server, which includes
 * subcomponents to:<ul>
 *   <li>act as a wp zone authority</li>
 *   <li>manage bind leases</li>
 *   <li>redirect requests to other zones</li>
 * </ul>
 * <p>
 * The subcomponents are pluggable to simply the configuration
 * and allow future enhancements.
 */
public class Server
extends ContainerSupport
{
  public static final String INSERTION_POINT = 
    Agent.INSERTION_POINT + ".WPServer";

  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    List l = new ArrayList();

    // add defaults
    l.add(new ComponentDescription(
            "ServerTransport",
            INSERTION_POINT+".ServerTransport",
            "org.cougaar.core.wp.server.ServerTransport",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));
    l.add(new ComponentDescription(
            "RootAuth",
            INSERTION_POINT+".RootAuth",
            "org.cougaar.core.wp.server.RootAuthority",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));

    ServiceBroker sb = getServiceBroker();

    // find our local agent
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    MessageAddress localAgent = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);

    // read config
    ComponentInitializerService cis = (ComponentInitializerService)
      sb.getService(this, ComponentInitializerService.class, null);
    try {
      ComponentDescription[] descs =
        cis.getComponentDescriptions(
            localAgent.toString(),
            specifyContainmentPoint());
      int n = (descs == null ? 0 : descs.length);
      for (int i = 0; i < n; i++) {
        l.add(descs[i]);
      }
    } catch (ComponentInitializerService.InitializerException cise) {
      Logger logger = Logging.getLogger(Server.class);
      if (logger.isInfoEnabled()) {
        logger.info("\nUnable to add "+localAgent+"'s components", cise);
      }
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    }

    return new ComponentDescriptions(l);
  }
}

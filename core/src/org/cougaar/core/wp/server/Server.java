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

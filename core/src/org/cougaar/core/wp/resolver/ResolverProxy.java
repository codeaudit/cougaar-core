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

package org.cougaar.core.wp.resolver;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This is a proxy to the WhitePagesService resolver that obtains
 * the local agent's name and tags all requests with that name.
 * <p>
 * This should be loaded into all agents.  All WhitePagesService
 * requests from within the agent will passed through a proxy
 * service.  From the node's point of view there will only be one
 * client per agent (ie. this proxy).
 * <p>
 * The proxy is disabled if this component is loaded into the
 * node agent. 
 */
public class ResolverProxy
extends GenericStateModelAdapter
implements Component
{
  private ServiceBroker sb;

  private LoggingService logger;
  private MessageAddress agentId;
  private MessageAddress nodeId;
  private WhitePagesService proxyWP;

  private WhitePagesSP proxySP;

  private final ResolverClient myClient =
    new ResolverClient() {
      public String getAgent() {
        return (agentId == null ? null : agentId.getAddress());
      }
    };

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void load() {
    super.load();

    if (logger.isDebugEnabled()) {
      logger.debug("Loading resolver proxy");
    }

    // which agent are we in?
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);

    // which node are we in?
    NodeIdentificationService nis = (NodeIdentificationService)
      sb.getService(this, NodeIdentificationService.class, null);
    nodeId = nis.getMessageAddress();
    sb.releaseService(this, NodeIdentificationService.class, nis);

    if (agentId == null ||
        agentId.equals(nodeId)) {
      // we're in the node agent
      return;
    }

    // get the node's wp service
    proxyWP = (WhitePagesService)
      sb.getService(myClient, WhitePagesService.class, null);
    if (proxyWP == null) {
      throw new RuntimeException(
          "Unable to obtain the WhitePagesService,"+
          " proxy failed for agent "+agentId+" on node "+nodeId);
    }

    // advertize our service proxy
    proxySP = new WhitePagesSP();
    sb.addService(WhitePagesService.class, proxySP);

    if (logger.isInfoEnabled()) {
      logger.info(
          "Loaded white pages resolver proxy for agent "+
          agentId+" on node "+nodeId);
    }
  }

  public void unload() {
    super.unload();

    // revoke white pages service
    if (proxySP != null) {
      sb.revokeService(WhitePagesService.class, proxySP);
      proxySP = null;
    }

    if (proxyWP != null) {
      sb.releaseService(myClient, WhitePagesService.class, proxyWP);
      proxyWP = null;
    }

    if (logger != null) {
      sb.releaseService(this, LoggingService.class, logger);
      logger = null;
    }
  }

  private class WhitePagesSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (WhitePagesService.class.isAssignableFrom(serviceClass)) {
          // return our proxyWP, where myClient identifies our agent
          if (logger.isDetailEnabled()) {
            logger.detail(
                "giving "+agentId+" proxy WP to "+requestor);
          }
          return proxyWP;
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

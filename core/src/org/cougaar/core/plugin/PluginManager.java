/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

package org.cougaar.core.plugin;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ComponentRuntimeException;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.util.ConfigFinder;

/**
 * A container for Plugin Components.
 */
public class PluginManager 
extends ContainerSupport
{
  /** The insertion point for a PluginManager, defined relative to its parent, Agent. **/
  public static final String INSERTION_POINT = Agent.INSERTION_POINT + ".PluginManager";

  private LoggingService logger;
  private MessageAddress agentId;
  private AgentIdentificationService agentIdService;

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais == null) {
      // Revocation
    } else {
      this.agentId = ais.getMessageAddress();
    }
  }

  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    String cname = agentId.toString();
    ServiceBroker sb = getServiceBroker();
    ComponentInitializerService cis = (ComponentInitializerService)
      sb.getService(this, ComponentInitializerService.class, null);
    try {
      return new ComponentDescriptions(
          cis.getComponentDescriptions(cname, specifyContainmentPoint()));
    } catch (ComponentInitializerService.InitializerException cise) {
      if (logger.isInfoEnabled()) {
        logger.info("\nUnable to add "+cname+"'s plugins ",cise);
      }
      return null;
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    }
  }

  public boolean add(Object o) {
    try {
      if (logger.isInfoEnabled()) {
        logger.info("Agent "+agentId+" adding plugin "+o);
      }
      boolean result = super.add(o);
      if (logger.isInfoEnabled()) {
        logger.info("Agent "+agentId+" added plugin "+o);
      }
      return result;
    } catch (ComponentRuntimeException cre) {
      Throwable cause = cre.getCause();
      if (cause == null) cause = cre;
      logger.error("Failed to add "+o+" to "+this, cause);
      throw cre;
    } catch (RuntimeException re) {
      //logger.error("Failed to add "+o+" to "+this, re);
      throw re;
    }
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

  // 
  // other services
  //
  
  public MessageAddress getMessageAddress() {
    return agentId;
  }
  public MessageAddress getAgentIdentifier() {
    return agentId;
  }
  public ConfigFinder getConfigFinder() {
    return ConfigFinder.getInstance(); // FIXME replace with service
  }
  public String toString() {
    return agentId+"/PluginManager";
  }

}

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

package org.cougaar.core.agent;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is the first component added to an agent, and
 * is used to bootstrap the agent with the minimal initial
 * components.
 * <p>
 * The last component bootstrapped in is {@link FindComponentsEarly}. 
 *
 * @see FindComponentsEarly 
 */
public final class Bootstrap
extends GenericStateModelAdapter
implements Component
{

  private static final int HIGH      = ComponentDescription.PRIORITY_HIGH;

  private ServiceBroker sb;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    MessageAddress localAgent = find_local_agent();
    MessageAddress localNode = find_local_node();

    boolean isNode = 
      (localAgent == null ||
       localAgent.equals(localNode));

    List l = getInitialComponents(isNode);

    overrideComponentList(l);
  }

  private static final ComponentDescription NODE_CONTROL_BLOCKER =
    new ComponentDescription(
        "org.cougaar.core.agent.NodeControlBlocker",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.agent.NodeControlBlocker",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription LOGGING_SERVICE =
    new ComponentDescription(
        "org.cougaar.core.node.LoggingServiceComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.LoggingServiceComponent",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription QUIESCENCE_REPORT =
    new ComponentDescription(
        "org.cougaar.core.node.QuiescenceReportComponent",
        "Node.AgentManager.Agent.Component",
        "org.cougaar.core.node.QuiescenceReportComponent",
        null, null, null, null, null,
        HIGH);

  private static final ComponentDescription BEGIN_LOGGER =
    new ComponentDescription(
          "org.cougaar.core.agent.BeginLogger",
          "Node.AgentManager.Agent.Component",
          "org.cougaar.core.agent.BeginLogger",
          null, null, null, null, null,
          HIGH);

  private static final ComponentDescription PERSISTENCE =
    new ComponentDescription(
          "org.cougaar.core.persist.PersistenceServiceComponent",
          "Node.AgentManager.Agent.Component",
          "org.cougaar.core.persist.PersistenceServiceComponent",
          null, null, null, null, null,
          HIGH);

  private static final ComponentDescription REGISTER_CONTEXT =
    new ComponentDescription(
          "org.cougaar.core.agent.RegisterContext",
          "Node.AgentManager.Agent.Component",
          "org.cougaar.core.agent.RegisterContext",
          null, null, null, null, null,
          HIGH);

  private static final ComponentDescription REHYDRATE_EARLY =
    new ComponentDescription(
          "org.cougaar.core.agent.RehydrateEarly",
          "Node.AgentManager.Agent.Component",
          "org.cougaar.core.agent.RehydrateEarly",
          null, null, null, null, null,
          HIGH);

  private static final ComponentDescription FIND_COMPONENTS_EARLY =
    new ComponentDescription(
          "org.cougaar.core.agent.FindComponentsEarly",
          "Node.AgentManager.Agent.Component",
          "org.cougaar.core.agent.FindComponentsEarly",
          null, null, null, null, null,
          HIGH);

  private static List getInitialComponents(boolean isNode) {
    List l = new ArrayList();

    l.add(NODE_CONTROL_BLOCKER);

    if (isNode) {
      l.add(LOGGING_SERVICE);
      l.add(QUIESCENCE_REPORT);
    }

    l.add(BEGIN_LOGGER);
    l.add(PERSISTENCE);
    l.add(REGISTER_CONTEXT);
    l.add(REHYDRATE_EARLY);
    l.add(FIND_COMPONENTS_EARLY);

    return l;
  }

  private void overrideComponentList(List l) {
    AgentBootstrapService abs = (AgentBootstrapService)
      sb.getService(this, AgentBootstrapService.class, null);
    if (abs == null) {
      throw new RuntimeException(
          "Unable to obtain AgentBootstrapService"+
          ", can not override the agent's component list");
    }
    abs.overrideComponentList(l);
    sb.releaseService(this, AgentBootstrapService.class, abs);
    abs = null;
  }

  private MessageAddress find_local_agent() {
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (ais == null) {
      return null;
    }
    MessageAddress ret = ais.getMessageAddress();
    sb.releaseService(
        this, AgentIdentificationService.class, ais);
    return ret;
  }

  private MessageAddress find_local_node() {
    NodeIdentificationService nis = (NodeIdentificationService)
      sb.getService(this, NodeIdentificationService.class, null);
    if (nis == null) {
      return null;
    }
    MessageAddress ret = nis.getMessageAddress();
    sb.releaseService(
        this, NodeIdentificationService.class, nis);
    return ret;
  }
}

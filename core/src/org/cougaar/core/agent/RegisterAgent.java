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

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.RegisterAgentService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component registers the agent with the node's
 * {@link RegisterAgentService}, which is used to track dynamically
 * added and removed agents.
 *
 * @see RegisterAgentService 
 */
public final class RegisterAgent
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private RegisterAgentService ras;

  private MessageAddress localAgent;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    ras = (RegisterAgentService)
      sb.getService(this, RegisterAgentService.class, null);
    if (ras != null) {
      ras.addAgent(localAgent);
    }
  }

  public void unload() {
    super.unload();

    if (ras != null) {
      ras.removeAgent(localAgent);
      sb.releaseService(this, RegisterAgentService.class, ras);
      ras = null;
    }
  }
}

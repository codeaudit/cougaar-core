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
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component registers the agent name in the
 * ClusterContextTable, which is required for serialization
 * and deserialization of persistence snapshots and messages.
 */
public final class RegisterContext
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private LoggingService log;
  private MessageAddress localAgent;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);

    // get our local agent's address
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    // add our address to our VM's cluster table
    if (log.isDebugEnabled()) {
      log.debug("Adding to the cluster context table");
    }
    ClusterContextTable.addContext(localAgent);
  }

  public void unload() {
    super.unload();
    // remove ourselves from the VM-local context
    if (log.isDebugEnabled()) {
      log.debug("Removing from the cluster context table");
    }
    ClusterContextTable.removeContext(localAgent);
  }
}

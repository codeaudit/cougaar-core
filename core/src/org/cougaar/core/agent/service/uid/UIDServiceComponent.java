/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

package org.cougaar.core.agent.service.uid;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceState;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The UIDServiceComponent is a provider class for the UID 
 * service within an agent.
 */
public final class UIDServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private UIDService uidS;
  private UIDServiceProvider uidSP;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    // get the local agent address
    MessageAddress agentAddr = null;
    AgentIdentificationService agentIdS = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (agentIdS != null) {
      agentAddr = agentIdS.getMessageAddress();
      sb.releaseService(this, AgentIdentificationService.class, agentIdS);
    }

    // create a single per-agent uid service instance
    this.uidS = new UIDServiceImpl(agentAddr);

    // create and advertise our service
    this.uidSP = new UIDServiceProvider();
    sb.addService(UIDService.class, uidSP);
  }

  public void unload() {
    // revoke our service
    if (uidSP != null) {
      sb.revokeService(UIDService.class, uidSP);
      uidSP = null;
    }
    super.unload();
  }

  private class UIDServiceProvider implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (UIDService.class.isAssignableFrom(serviceClass)) {
        return uidS;
      } else {
        return null;
      }
    }

    public void releaseService(
        ServiceBroker sb, Object requestor, 
        Class serviceClass, Object service)  {
    }
  }

}

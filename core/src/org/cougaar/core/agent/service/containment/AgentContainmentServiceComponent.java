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

package org.cougaar.core.agent.service.containment;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.AgentContainmentService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The AgentContainmentServiceComponent is a provider class for the
 * AgentContainment service within an agent.
 * <p>
 * This is essentially a back-pointer to the agent's Container API.
 */
public final class AgentContainmentServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private AgentContainmentService acS;
  private AgentContainmentServiceProvider acSP;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setParameter(Object o) {
    acS = (AgentContainmentService) o;
  }

  public void load() {
    super.load();

    if (acS == null) {
      throw new RuntimeException(
          "Missing AgentContainmentService parameter");
    }

    // create and advertise our service
    this.acSP = new AgentContainmentServiceProvider();
    sb.addService(AgentContainmentService.class, acSP);
  }

  public void unload() {
    // revoke our service
    if (acSP != null) {
      sb.revokeService(AgentContainmentService.class, acSP);
      acSP = null;
    }
    super.unload();
  }

  private class AgentContainmentServiceProvider implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (AgentContainmentService.class.isAssignableFrom(serviceClass)) {
        return acS;
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

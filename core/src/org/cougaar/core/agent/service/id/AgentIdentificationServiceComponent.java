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

package org.cougaar.core.agent.service.id;

import java.util.List;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The AgentIdentificationServiceComponent is a provider class for the 
 * AgentIdentification service within an agent.
 */
public final class AgentIdentificationServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private MessageAddress addr;
  private AgentIdentificationService aiS;
  private AgentIdentificationServiceProvider aiSP;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setParameter(Object o) {
    if (o instanceof MessageAddress) {
      addr = (MessageAddress) o;
    } else if (o instanceof String) {
      addr = MessageAddress.getMessageAddress((String) o);
    } else if (o instanceof List) {
      List l = (List)o;
      if (l.size() > 0) {
        setParameter(l.get(0));
      }
    }
  }

  public void load() {
    super.load();

    // create a single per-agent ai service instance
    this.aiS = new AgentIdentificationServiceImpl(addr);

    // create and advertise our service
    this.aiSP = new AgentIdentificationServiceProvider();
    sb.addService(AgentIdentificationService.class, aiSP);
  }

  public void unload() {
    // revoke our service
    if (aiSP != null) {
      sb.revokeService(AgentIdentificationService.class, aiSP);
      aiSP = null;
    }
    super.unload();
  }

  private class AgentIdentificationServiceProvider implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (AgentIdentificationService.class.isAssignableFrom(serviceClass)) {
        return aiS;
      } else {
        return null;
      }
    }

    public void releaseService(
        ServiceBroker sb, Object requestor, 
        Class serviceClass, Object service)  {
    }
  }

  private static class AgentIdentificationServiceImpl
    implements AgentIdentificationService {
      private final MessageAddress addr;
      public AgentIdentificationServiceImpl(MessageAddress addr) {
        this.addr = addr;
        if (addr == null) {
          throw new IllegalArgumentException(
              "Agent address is null");
        }
      }
      public MessageAddress getMessageAddress() { 
        return addr;
      }
      public String getName() {
        return addr.getAddress();
      }
      public String toString() {
        return getName();
      }
    }
}

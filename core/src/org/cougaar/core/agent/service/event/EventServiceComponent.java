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

package org.cougaar.core.agent.service.event;

import java.util.List;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.EventService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;
import org.cougaar.util.log.Logging;

/**
 * The EventServiceComponent is a provider class for the 
 * Event service within an agent.
 */
public final class EventServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  private static final String EVENT_PREFIX = "EVENT.";

  private ServiceBroker sb;
  private String prefix = "";
  private EventServiceProvider sp;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    if (ais == null) {
      // Revocation
    } else {
      prefix = ais.getMessageAddress()+": ";
    }
  }

  public void load() {
    super.load();

    // if we're in the node-agent then advertise at the root
    // level, to allow use by all node-level components
    //
    // note that the other agengs will override this node-level
    // service to make the prefix match their agent's id, as
    // opposed to the node's id.
    NodeControlService nodeControlService = (NodeControlService)
      sb.getService(
          this, NodeControlService.class, null);
    if (nodeControlService != null) {
      ServiceBroker rootsb =
        nodeControlService.getRootServiceBroker();
      sb.releaseService(
          this, NodeControlService.class, nodeControlService);
      sb = rootsb;
    }

    // create and advertise our service
    if (sp == null) {
      sp = new EventServiceProvider();
      sb.addService(EventService.class, sp);
    }
  }

  public void unload() {
    // revoke our service
    if (sp != null) {
      sb.revokeService(EventService.class, sp);
      sp = null;
    }
    super.unload();
  }

  private class EventServiceProvider implements ServiceProvider {

    private final LoggerFactory lf = LoggerFactory.getInstance();

    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (EventService.class.isAssignableFrom(serviceClass)) {
        String name = EVENT_PREFIX + Logging.getKey(requestor);
        Logger l = lf.createLogger(name);
        return new EventServiceImpl(prefix, l);
      } else {
        return null;
      }
    }

    public void releaseService(
        ServiceBroker sb, Object requestor, 
        Class serviceClass, Object service)  {
    }
  }

  private static class EventServiceImpl
    implements EventService {
    private final String prefix;
    private final Logger l;
    public EventServiceImpl(String prefix, Logger l) {
      this.prefix = prefix;
      this.l = l;
    }
    public boolean isEventEnabled() {
      return l.isEnabledFor(Logger.INFO); 
    }
    public void event(String s) {
      event(s, null);
    }
    public void event(String s, Throwable t) {
      l.log(Logger.INFO, prefix+s, t);
    }
  }
}

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

package org.cougaar.core.node;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The NodeBusyComponent adds the {@link NodeBusyService}
 * to the root-level service broker.
 */
public final class NodeBusyComponent
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;
  private ServiceBroker rootsb;

  private LoggingService log;

  private ServiceProvider nbsp;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);

    NodeControlService ncs = (NodeControlService)
      sb.getService(this, NodeControlService.class, null);
    if (ncs == null) {
      throw new RuntimeException("Unable to obtain NodeControlService");
    }
    rootsb = ncs.getRootServiceBroker();
    sb.releaseService(this, NodeControlService.class, ncs);

    nbsp = new NodeBusyServiceProvider(log);
    rootsb.addService(NodeBusyService.class, nbsp);
  }

  public void unload() {
    super.unload();

    rootsb.revokeService(NodeBusyService.class, nbsp);
    nbsp = null;
  }

  private static class NodeBusyServiceProvider
    implements ServiceProvider {
      private final LoggingService log;
      private final Set busyAgents = new HashSet();

      public NodeBusyServiceProvider(LoggingService log) {
        this.log = log;
      }

      public Object getService(
          ServiceBroker xsb, Object requestor, Class serviceClass) {
        if (serviceClass != NodeBusyService.class) {
          throw new IllegalArgumentException(
              "Can only provide NodeBusyService!");
        }
        return new NodeBusyService() {
          MessageAddress me = null;
          public void setAgentIdentificationService(
              AgentIdentificationService ais) {
            me = ais.getMessageAddress();
          }
          public void setAgentBusy(boolean busy) {
            if (me == null) {
              throw new RuntimeException(
                  "AgentIdentificationService has not been set");
            }
            if (log.isDebugEnabled()) {
              log.debug("setAgentBusy(" + me + ", " + busy + ")");
            }
            if (busy) {
              busyAgents.add(me);
            } else {
              busyAgents.remove(me);
            }
          }
          public boolean isAgentBusy(MessageAddress agent) {
            return busyAgents.contains(agent);
          }
          public Set getBusyAgents() {
            return Collections.unmodifiableSet(busyAgents);
          }
        };
      }

      public void releaseService(
          ServiceBroker xsb, Object requestor,
          Class serviceClass, Object service) {
      }
    }
}

/*
 * <copyright>
 *  Copyright 2000-2001 BBNT Solutions, LLC
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
package org.cougaar.core.examples;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.component.*;
import org.cougaar.core.domain.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.node.*;
import org.cougaar.core.mts.MessageAddress;

/**
 * A filter for agent-to-node services.
 * <p>
 * See core examples "PluginServiceFilter" for further details.
 * <p>
 * Add a line like the following to a cluster.ini file: <pre>
 *   Node.AgentManager.Binder = org.cougaar.core.agent.AgentServiceFilter
 * </pre>
 **/
public class AgentServiceFilter 
extends ServiceFilter 
{

  //  This method specifies the Binder to use (defined later)
  protected Class getBinderClass(Object child) {
    return AgentServiceFilterBinder.class;
  }
  
  // This is a "Wrapper" binder which installs a service filter for plugins
  public static class AgentServiceFilterBinder
    extends ServiceFilterBinder
    implements AgentBinder
  {
    public AgentServiceFilterBinder(BinderFactory bf, Object child) {
      super(bf,child);
    }

    public MessageAddress getAgentIdentifier() {
      AgentBinder ab = (AgentBinder) getChildBinder();
      MessageAddress ret = ab.getAgentIdentifier();
      System.out.println("Agent "+ret+" wrapper: get agent-id from binder "+ab);
      return ret;
    }

    public Agent getAgent() {
      AgentBinder ab = (AgentBinder) getChildBinder();
      MessageAddress addr = ab.getAgentIdentifier();
      Agent ret = ab.getAgent();
      System.out.println("Agent "+addr+" wrapper: get agent from binder "+ab);
      return ret;
    }

    protected final AgentManagerForBinder getAgentManager() { 
      return (AgentManagerForBinder) getContainer(); 
    }

    // this method specifies a binder proxy to use, so as to avoid exposing the binder
    // itself to the lower level objects.
    protected ContainerAPI createContainerProxy() { 
      return new AgentFilteringBinderProxy(); 
    }

    // this method installs the "filtering" service broker
    protected ServiceBroker createFilteringServiceBroker(ServiceBroker sb) {
      return new AgentFilteringServiceBroker(sb); 
    }

    // this class implements a simple proxy for a plugin wrapper binder
    protected class AgentFilteringBinderProxy
      extends ServiceFilterContainerProxy
      implements AgentManagerForBinder
    {
      public String getName() {
        return getAgentManager().getName();
      }
      public void registerAgent(Agent agent) {
        getAgentManager().registerAgent(agent);
      }
    }


    // this class catches requests for blackboard services, and 
    // installs its own service proxy.
    protected class AgentFilteringServiceBroker 
      extends FilteringServiceBroker
    {
      public AgentFilteringServiceBroker(ServiceBroker sb) {
        super(sb);
      }
      protected Object getServiceProxy(Object service, Class serviceClass, Object client) {
        if (service instanceof NodeIdentificationService) {
          return new NodeIdentificationServiceProxy(
              (NodeIdentificationService) service, client);
        } 
        return null;
      }
    }
  }

  public static class NodeIdentificationServiceProxy implements NodeIdentificationService {
    private NodeIdentificationService nis;
    private Object client;
    public NodeIdentificationServiceProxy(NodeIdentificationService nis, Object client) {
      this.nis = nis;
      this.client=client;
    }
    public NodeIdentifier getNodeIdentifier() {
      NodeIdentifier ret = nis.getNodeIdentifier();
      System.out.println("Agent wrapper: get node-id "+ret+" for client "+client);
      return ret;
    }
  }
}

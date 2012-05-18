/*
 * <copyright>
 *  
 *  Copyright 2000-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
package org.cougaar.core.examples;

import java.net.InetAddress;

import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceFilter;
import org.cougaar.core.component.ServiceFilterBinder;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;

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
  @Override
protected Class getBinderClass(Object child) {
    return AgentServiceFilterBinder.class;
  }
  
  // This is a "Wrapper" binder which installs a service filter for plugins
  public static class AgentServiceFilterBinder
    extends ServiceFilterBinder
  {
    public AgentServiceFilterBinder(BinderFactory bf, Object child) {
      super(bf,child);
    }

    // this method specifies a binder proxy to use, so as to avoid exposing the binder
    // itself to the lower level objects.
    @Override
   protected ContainerAPI createContainerProxy() { 
      return new AgentFilteringBinderProxy(); 
    }

    // this method installs the "filtering" service broker
    @Override
   protected ServiceBroker createFilteringServiceBroker(ServiceBroker sb) {
      return new AgentFilteringServiceBroker(sb); 
    }

    // this class implements a simple proxy for a plugin wrapper binder
    protected class AgentFilteringBinderProxy
      extends ServiceFilterContainerProxy
    {
    }


    // this class catches requests for blackboard services, and 
    // installs its own service proxy.
    protected class AgentFilteringServiceBroker 
      extends FilteringServiceBroker
    {
      public AgentFilteringServiceBroker(ServiceBroker sb) {
        super(sb);
      }
      @Override
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
    public MessageAddress getMessageAddress() {
      MessageAddress ret = nis.getMessageAddress();
      System.out.println("Agent wrapper: get node-id "+ret+" for client "+client);
      return ret;
    }
    
    public InetAddress getInetAddress() {
      return nis.getInetAddress();
    }
  }
}

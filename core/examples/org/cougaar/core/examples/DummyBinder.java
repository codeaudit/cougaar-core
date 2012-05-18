/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceFilter;
import org.cougaar.core.component.ServiceFilterBinder;

/** A plugin's view of its parent component (Container).
 * Print a message when instantiated, and then report whenever someone 
 * requests a service through its filter.
 * <p>
 * Add a line like the following to a cluster.ini file:
 * <pre>
 * Node.AgentManager.Agent.PluginManager.Binder = org.cougaar.core.examples.DummyBinder
 * </pre>

 **/
public class DummyBinder                 // PluginServiceFilter
  extends ServiceFilter
{
  public DummyBinder() {
    System.err.println("DummyBinder instantiated");
  }
  public void setParameter(Object o) {}
  //  This method specifies the Binder to use (defined later)
  protected Class getBinderClass(Object child) {
    return PluginServiceFilterBinder.class;
  }

  // This is a "Wrapper" binder which installs a service filter for plugins
  public static class PluginServiceFilterBinder
    extends ServiceFilterBinder
  {
    public PluginServiceFilterBinder(BinderFactory bf, Object child) {
      super(bf,child);
    }

    // this method specifies a binder proxy to use, so as to avoid exposing the binder
    // itself to the lower level objects.
    protected ContainerAPI createContainerProxy() {
      return new ServiceFilterContainerProxy();
    }

    // this method installs the "filtering" service broker
    protected ServiceBroker createFilteringServiceBroker(ServiceBroker sb) {
      return new PluginFilteringServiceBroker(sb); 
    }

    // this class catches requests for blackboard services, and 
    // installs its own service proxy.
    protected class PluginFilteringServiceBroker 
      extends FilteringServiceBroker
    {
      public PluginFilteringServiceBroker(ServiceBroker sb) {
        super(sb);
      }

      protected Object getServiceProxy(Object service, Class serviceClass, Object client) {
        System.err.println("DummyBinder: "+serviceClass+" requested by "+client);
        return null;
      }
    }
  }
}

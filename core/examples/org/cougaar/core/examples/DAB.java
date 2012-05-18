/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/** DAB is a Dummy Agent Binder, an example wrapping binder for
 * agents to be used as a trivial example to code writers.
 * A node.ini recipe can load this as a wrapping binder as:
 * <pre>
 * Node.AgentManager.Binder = org.cougaar.core.examples.DAB
 * </pre>
 * Then when run, you'll see one message indicating that the DAB
 * has been instantiated, and n+1 messages indicating that a DABber
 * has, where n = number of agents (the extra one is the NodeAgent).
 **/

public class DAB 
  extends ServiceFilter
{
  public int getPriority() { return MAX_PRIORITY; }
  Logger logger;

  public DAB() {
    logger = Logging.getLogger(DAB.class);
    logger.shout("DAB instantiated", new Throwable());
  }
  public void setParameter(Object o) {}

  protected Class getBinderClass(Object child) {
    return DABber.class;
  }

  // This is a "Wrapper" binder which installs a service filter for plugins
  public static class DABber
    extends ServiceFilterBinder
  {
    Logger logger;

    public DABber(BinderFactory bf, Object child) {
      super(bf,child);
      logger = Logging.getLogger(DAB.class);
      logger.shout("DABber instantiated for", new Throwable());
    }

    // this method specifies a binder proxy to use, so as to avoid exposing the binder
    // itself to the lower level objects.
    protected ContainerAPI createContainerProxy() { return this; }

    // this method installs the "filtering" service broker
    protected ServiceBroker createFilteringServiceBroker(ServiceBroker sb) {
      return sb;
    }
  }
}

/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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

import org.cougaar.core.blackboard.*;
import java.util.*;
import java.lang.reflect.*;
import org.cougaar.core.component.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.util.log.*;

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

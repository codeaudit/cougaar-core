/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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

package org.cougaar.core.node.service.jvmdump;

import org.cougaar.core.component.*;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.JvmStackDumpService;

/**
 * Component that provides the JvmStackDumpService.
 * <p>
 * This is simply a node-level wrapper for our package-private
 * JNI implementation (JniStackDump).
 * <p>
 * This can be loaded in a node by adding this line to a
 * node's ".ini" or CSMART configuration:
 * <pre>
 *   Node.AgentManager.Agent.JvmDump = org.cougaar.core.node.service.jvmdump.JvmStackDumpServiceComponent
 * </pre>
 */
public class JvmStackDumpServiceComponent 
extends org.cougaar.util.GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private ServiceProvider mySP;

  public void setBindingSite(BindingSite bs) {
    // ignore; we only care about the node-level service broker
  }

  public void setNodeControlService(NodeControlService ncs) {
    this.sb = ncs.getRootServiceBroker();
  }

  public void start() {
    super.start();
    // create and advertise our service
    this.mySP = new JvmStackDumpServiceProviderImpl();
    sb.addService(JvmStackDumpService.class, mySP);
  }

  public void stop() {
    // revoke our service
    if (mySP != null) {
      sb.revokeService(JvmStackDumpService.class, mySP);
      mySP = null;
    }
    super.stop();
  }


  /**
   * Service provider for our <code>JvmStackDumpService</code>.
   */
  private class JvmStackDumpServiceProviderImpl
  implements ServiceProvider {

    // single service instance, since it's just a wrapper
    private final JvmStackDumpServiceImpl SINGLETON =
      new JvmStackDumpServiceImpl();

    public Object getService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass) {
      if (serviceClass != JvmStackDumpService.class) {
        throw new IllegalArgumentException(
            "JvmStackDumpService does not provide a service for: "+
            serviceClass);
      }
      return SINGLETON;
    }

    public void releaseService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass, 
        Object service)  {
      if (!(service instanceof JvmStackDumpServiceImpl)) {
        throw new IllegalArgumentException(
            "JvmStackDumpService unable to release service: "+
            ((service != null) ? service.getClass().toString() : "null"));
      }
      // ignore our singleton
    }

    private class JvmStackDumpServiceImpl
    implements JvmStackDumpService {
      public boolean dumpStack() {
        // call our package-private implementation!
        return JniStackDump.dumpStack();
      }
    }
  }
}


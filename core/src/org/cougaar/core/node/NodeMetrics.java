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

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.service.NodeMetricsService;
import org.cougaar.util.GenericStateModelAdapter;

public final class NodeMetrics
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;
  private ServiceBroker rootsb;

  private ServiceProvider nmsp;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    NodeControlService ncs = (NodeControlService)
      sb.getService(this, NodeControlService.class, null);
    if (ncs == null) {
      throw new RuntimeException("Unable to obtain NodeControlService");
    }
    rootsb = ncs.getRootServiceBroker();
    sb.releaseService(this, NodeControlService.class, ncs);

    nmsp = new NodeMetricsServiceProvider(new NodeMetricsProxy());
    rootsb.addService(NodeMetricsService.class, nmsp);
  }

  public void unload() {
    super.unload();

    rootsb.revokeService(NodeMetricsService.class, nmsp);
    nmsp = null;
  }

  private static class NodeMetricsProxy 
    implements NodeMetricsService {

      /** Free Memory snapshot from the Java VM   **/
      public long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
      }
      /** Total memory snaphsot from the Java VM    */
      public long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
      }
      /** The number of active Threads in the main COUGAAR threadgroup **/
      public int getActiveThreadCount() {
        return Thread.currentThread().getThreadGroup().activeCount();
      }
    }
}

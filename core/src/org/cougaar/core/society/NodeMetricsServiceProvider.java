/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.society;

import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;

/** A NodeMetricsServiceProvider is a provider class for the NodeMetricsService
 * which provides Node/VM metrics.
 **/
public class NodeMetricsServiceProvider implements ServiceProvider {
  
  private NodeMetricsService theproxy;
  
  public NodeMetricsServiceProvider(NodeMetricsService theproxy) {
    super();
    this.theproxy = theproxy;
  }
  
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == NodeMetricsService.class) {
      return theproxy;
    } else {
      throw new IllegalArgumentException("NodeMetricsServiceProvider does not provide a service for: "+
                                         serviceClass);
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
  }

}

/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.blackboard;

import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.cluster.Subscriber;
import org.cougaar.core.cluster.Distributor;

import java.util.*;

/** A BlackboardServiceProvider is a provider class that PluginManager calls
 * when a client requests a BlackboardService.
 **/
public class BlackboardServiceProvider implements ServiceProvider {
  
  private Distributor distributor;
  
  public BlackboardServiceProvider(Distributor distributor) {
    super();
    this.distributor = distributor;
  }
  
  public Object getService(BlackboardClient bbclient) {
    return new Subscriber(bbclient, distributor);
  }
  
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == BlackboardService.class) {
      return new Subscriber( (BlackboardClient)requestor, distributor );
    } else if (serviceClass == BlackboardMetricsService.class) {
      return getBlackboardMetricsService();
    } else {
      throw new IllegalArgumentException("BlackboardServiceProvider does not provide a service for: "+
                                         serviceClass);
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
    // ?? each client will get its own subscriber - how can we clean them up??
  }

  // blackboard metrics
  private BlackboardMetricsService bbmetrics = null;

  // only need one instance of this service.
  private BlackboardMetricsService getBlackboardMetricsService() {
    if (bbmetrics != null) {
      return bbmetrics;
    } else {
      //create one
      bbmetrics = new BlackboardMetricsServiceImpl(distributor);
      return bbmetrics;
    }
  }
  
}


  
  

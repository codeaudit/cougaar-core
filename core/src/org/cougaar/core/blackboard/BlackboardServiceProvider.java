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
import org.cougaar.core.component.Services;
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
  
  public Object getService(Services services, Object requestor, Class serviceClass) {
   return new Subscriber( (BlackboardClient)requestor, distributor );
  }

  public void releaseService(Services services, Object requestor, Class serviceClass, Object service)  {
    // ?? each client will get its own subscriber - how can we clean them up??
  }
  
}


  
  
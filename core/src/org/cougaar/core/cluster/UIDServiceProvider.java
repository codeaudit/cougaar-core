/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster;

import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;

/** A UIDServiceProvider is a provider class for the UID services. **/
public class UIDServiceProvider implements ServiceProvider {
  private UIDServer _uidService = null;
  private ClusterContext ccontext = null;
  
  public UIDServiceProvider(ClusterContext context) {
    super();
    ccontext = context;
  }
  
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    return getUIDService();
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
    // more??
    _uidService = null;
  }

  //only want one per cluster  
  private UIDServer getUIDService() {
    synchronized (this) {
      if (_uidService == null) {
        _uidService = new UIDServer(ccontext);
      }
      return _uidService;
    }
  }

}


  
  

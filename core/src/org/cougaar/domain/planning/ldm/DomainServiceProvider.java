/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.domain.planning.ldm;

import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;

/** A DomainServiceProvider is a provider class for domain factory services. **/
public class DomainServiceProvider implements ServiceProvider {

  private DomainServiceImpl domService = null;
  
  public DomainServiceProvider() {
    super();
  }
  
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    return getDomainService();
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
    // more??
    domService = null;
  }

  //need one per cluster (really probably one per node/society????  
  private DomainServiceImpl getDomainService() {
    synchronized (this) {
      if (domService == null) {
        domService = new DomainServiceImpl();
      }
      return domService;
    }
  }

}


  
  

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
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;

/** A DomainServiceProvider is a provider class for domain factory services. **/
public class DomainServiceProvider implements ServiceProvider {
  private DomainServiceImpl theService;

  public DomainServiceProvider(DomainServiceImpl theDomainService) {
    this.theService = theDomainService;
  }
  
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (DomainService.class.isAssignableFrom(serviceClass)) {
      return new DomainServiceProxy();
    } else {
      return null;
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
  }

  private final class DomainServiceProxy implements DomainService {
    public ClusterObjectFactory getClusterObjectFactory() {
      return theService.getClusterObjectFactory();
    }
    public RootFactory getFactory() {
      return theService.getFactory();
    }
    public RootFactory getLdmFactory() {
      return theService.getLdmFactory();
    }
    public Factory getFactory(String domainname) {
      return theService.getFactory(domainname);
    }
  } // end of DomainServiceProxy

}


  
  

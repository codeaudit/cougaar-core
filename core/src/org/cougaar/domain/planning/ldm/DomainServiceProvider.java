/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
    /** @deprecated **/
    public RootFactory getLdmFactory() {
      return theService.getFactory();
    }
    public Factory getFactory(String domainname) {
      return theService.getFactory(domainname);
    }
  } // end of DomainServiceProxy

}


  
  

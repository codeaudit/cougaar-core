/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

package org.cougaar.core.domain;

import java.util.Collection;
import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BinderSupport;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ServiceBroker;

/** The standard Binder for Domains.
 **/
public class DefaultDomainBinder 
  extends BinderSupport 
  implements DomainBinder
{
  /** All subclasses must implement a matching constructor. **/
  public DefaultDomainBinder(BinderFactory bf, Object child) {
    super(bf, child);
  }

  protected final DomainBase getDomain() {
    return (DomainBase) getComponent();
  }
  protected final DomainManagerForBinder getDomainManager() {
    return (DomainManagerForBinder)getContainer();
  }

  protected BindingSite getBinderProxy() {
    return new DomainBindingSiteImpl();
  }

  /** Implement the binding site delegate **/
  protected class DomainBindingSiteImpl implements DomainBindingSite {
    public final ServiceBroker getServiceBroker() {
      return DefaultDomainBinder.this.getServiceBroker();
    } 

    public final void requestStop() {
      DefaultDomainBinder.this.requestStop();
    }

    public final Collection getXPlans() {
      return getDomainManager().getXPlans();
    }

    public final XPlan getXPlanForDomain(String domainName) {
      return getDomainManager().getXPlanForDomain(domainName);
    }

    public final XPlan getXPlanForDomain(Class domainClass) {
      return getDomainManager().getXPlanForDomain(domainClass);
    }

    public final Factory getFactoryForDomain(String domainName) {
      return getDomainManager().getFactoryForDomain(domainName);
    }

    public final Factory getFactoryForDomain(Class domainClass) {
      return getDomainManager().getFactoryForDomain(domainClass);
    }

    public String toString() {
      return "Proxy for "+(DefaultDomainBinder.this.toString());
    }
  }

  public String toString() {
    return (super.toString())+"/"+getDomain();
  }
}




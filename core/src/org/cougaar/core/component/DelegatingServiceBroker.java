/*
 * <copyright>
 * Copyright 2000-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.util.*;

/** A Simple ServiceBroker which just delegates all
 * queries to another, useful for making restricted extentions.
 * Note that it does a little bit of magic to hide the identity of the
 * other ServiceBroker from clients.
 **/

public class DelegatingServiceBroker
  implements ServiceBroker 
{
  public DelegatingServiceBroker(ServiceBroker delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("Delegate must be non-null");
    }

    this.delegate = delegate;
  }

  private ServiceBroker delegate;

  protected ServiceBroker getDelegate() {
    return delegate;
  }

  private HashMap listeners = new HashMap(11);

  /** add a ServiceListener to this ServiceBroker Context **/
  public void addServiceListener(final ServiceListener sl) {
    if (sl == null) 
      throw new IllegalArgumentException("Add of null ServiceListener");

    ServiceListener slp;
    if (sl instanceof ServiceAvailableListener) {
      slp = new ServiceAvailableListener() {
          public void serviceAvailable(ServiceAvailableEvent ae) {
            ((ServiceAvailableListener)sl).serviceAvailable(new ServiceAvailableEvent(DelegatingServiceBroker.this,
                                                                                      ae.getService()));
          }
        };
    } else if (sl instanceof ServiceRevokedListener) {
      slp = new ServiceRevokedListener() {
          public void serviceRevoked(ServiceRevokedEvent ae) {
            ((ServiceRevokedListener)sl).serviceRevoked(new ServiceRevokedEvent(DelegatingServiceBroker.this, 
                                                                                ae.getService()));
          }
        };
    } else {
      throw new IllegalArgumentException("DelegatingServiceBroker cannot delegate this listener: "+sl);
    }
    synchronized (listeners) {
      listeners.put(sl,slp);
    }
    delegate.addServiceListener(slp);
  }
      
  /** remove a services listener **/
  public void removeServiceListener(ServiceListener sl) {
    if (sl == null) 
      throw new IllegalArgumentException("Remove of null ServiceListener");

    ServiceListener slp;
    synchronized (listeners) {
      slp = (ServiceListener) listeners.get(sl);
      if (slp != null) {
        listeners.remove(sl);
      }
    }
    if (slp != null) {
      delegate.removeServiceListener(slp);
    }
  }
  
  /** add a Service to this ServiceBroker Context **/
  public boolean addService(Class serviceClass, ServiceProvider serviceProvider) {
    return delegate.addService(serviceClass, serviceProvider);
  }

  /** remoke or remove an existing service **/
  public void revokeService(Class serviceClass, ServiceProvider serviceProvider) {
    delegate.revokeService(serviceClass, serviceProvider);
  }

  /** is the service currently available? **/
  public boolean hasService(Class serviceClass) {
    return delegate.hasService(serviceClass);
  }

  /** gets the currently available services for this context.
   * This version copies the keyset to keep the iterator safe.
   **/
  public Iterator getCurrentServiceClasses() {
    return delegate.getCurrentServiceClasses();
  }

  /** get an instance of the requested service from a service provider associated
   * with this context.
   **/
  public Object getService(Object requestor, final Class serviceClass, final ServiceRevokedListener srl) {
    return delegate.getService(requestor, serviceClass, srl);
  }

  public void releaseService(Object requestor, Class serviceClass, Object service) {
    releaseService(requestor, serviceClass, service);
  }
}

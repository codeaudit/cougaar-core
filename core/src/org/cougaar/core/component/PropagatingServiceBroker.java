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

/** A service broker which implements not just a local SB, but also 
 * a pass-through to another (presumably higher-level) SB.
 **/

public class PropagatingServiceBroker
  extends ServiceBrokerSupport
{
  public PropagatingServiceBroker(BindingSite bs) {
    this(bs.getServiceBroker());
  }

  public PropagatingServiceBroker(ServiceBroker delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("Delegate must be non-null");
    }

    this.delegate = delegate;
    connectToDelegate(delegate);
  }

  private ServiceBroker delegate;

  protected ServiceBroker getDelegate() {
    return delegate;
  }

  protected void connectToDelegate(ServiceBroker d) {
    // listen to the delegating service and relay events to our clients.
    d.addServiceListener(new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent ae) {
          applyListeners(new ServiceAvailableEvent(PropagatingServiceBroker.this, ae.getService()));
        }
      });
    d.addServiceListener(new ServiceRevokedListener() {
        public void serviceRevoked(ServiceRevokedEvent ae) {
          applyListeners(new ServiceRevokedEvent(PropagatingServiceBroker.this, ae.getService())); 
        }
      });
  }

  /** is the service currently available in this broker or in the Delegate? **/
  public boolean hasService(Class serviceClass) {
    boolean localp = super.hasService(serviceClass);
    return localp || delegate.hasService(serviceClass);
  }

  /** gets the currently available services for this context.
   * This version copies the keyset to keep the iterator safe, so don't be doing this
   * too often.
   **/
  public Iterator getCurrentServiceClasses() {
    ArrayList l = new ArrayList(); // ugh!
    // get the local services
    {
      Iterator i = super.getCurrentServiceClasses();
      while (i.hasNext()) {
        l.add(i.next());
      }
    }
    // get the delegated services
    {
      Iterator i = delegate.getCurrentServiceClasses();
      while (i.hasNext()) {
        l.add(i.next());
      }
    }
    return l.iterator();
  }

  /** get an instance of the requested service from a service provider associated
   * with this context.
   **/
  public Object getService(Object requestor, final Class serviceClass, final ServiceRevokedListener srl) {
    Object service = super.getService(requestor, serviceClass, srl);
    if (service != null) {
      return service;
    } else {
      return delegate.getService(requestor, serviceClass, srl);
    }
  }
}

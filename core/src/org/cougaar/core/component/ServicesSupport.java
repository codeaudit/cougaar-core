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

/** Simple implementation of Cougaar component services layer.  
 * @see org.cougaar.core.component.Services
 **/

public abstract class ServicesSupport 
  implements Services 
{
  /** the current set of Listeners.  Elements are of type ServiceListener **/
  private ArrayList listeners = new ArrayList();

  /** add a ServiceListener to this Services Context **/
  public void addServiceListener(ServiceListener sl) {
    if (sl == null) 
      throw new IllegalArgumentException("Add of null ServiceListener");
    synchronized (listeners) {
      listeners.add(sl);
    }
  }
      
  /** remove a services listener **/
  public void removeServiceListener(ServiceListener sl) {
    if (sl == null) 
      throw new IllegalArgumentException("Remove of null ServiceListener");
    synchronized (listeners) {
      listeners.remove(sl);
    }
  }
  
  /** Apply each listener appropriately to the event **/
  protected void applyListeners(ServiceEvent se) {
    synchronized (listeners) {
      int n = listeners.size();
      for (int i = 0; i<n; i++) {
        applyListener((ServiceListener) listeners.get(i), se);
      }
    }
  }

  protected void applyListener(ServiceListener sl, ServiceEvent se) {
    if (sl instanceof ServiceAvailableListener) {
      if (se instanceof ServiceAvailableEvent) {
        ((ServiceAvailableListener)sl).serviceAvailable((ServiceAvailableEvent)se);
      }
    } else if (sl instanceof ServiceRevokedListener) {
      if (se instanceof ServiceRevokedEvent) {
        ((ServiceRevokedListener)sl).serviceRevoked((ServiceRevokedEvent)se);
      }
    } else {
      // what is this?
    }
  }

  /** the current set of services.  A map of Class serviceClass to ServiceProvider **/
  private HashMap services = new HashMap(89);

  /** add a Service to this Services Context **/
  public boolean addService(Class serviceClass, ServiceProvider serviceProvider) {
    if (serviceClass == null)
      throw new IllegalArgumentException("serviceClass null");
    if(serviceProvider == null)
      throw new IllegalArgumentException("serviceProvider null");
      
    synchronized (services) {
      Object old = services.get(serviceClass);
      if (old != null) {
        return false;
      } else {
        services.put(serviceClass, serviceProvider);
        // fall through
      }
    }

    // notify any listeners
    applyListeners(new ServiceAvailableEvent(this, serviceClass));

    return true;
  }

  /** remoke or remove an existing service **/
  public void revokeService(Class serviceClass, ServiceProvider serviceProvider) {
    if (serviceClass == null)
      throw new IllegalArgumentException("serviceClass null");
    if(serviceProvider == null)
      throw new IllegalArgumentException("serviceProvider null");
      
    synchronized (services) {
      Object old = services.remove(serviceClass); 
      if (old == null) {
        return;                 // bail out - already revoked
      } 
      // else fall through
    }

    // notify any listeners
    applyListeners(new ServiceRevokedEvent(this, serviceClass));
  }

  /** is the service currently available? **/
  public boolean hasService(Class serviceClass) {
    if (serviceClass == null)
      throw new IllegalArgumentException("serviceClass null");
    synchronized (services) {
      return (null != services.get(serviceClass));
    }
  }

  /** gets the currently available services for this context.
   * This version copies the keyset to keep the iterator safe.
   **/
  public Iterator getCurrentServiceClasses() {
    //We could cache the answer if this turns out to be a hot spot.
    synchronized (services) {
      ArrayList l = new ArrayList(services.keySet());
      return l.iterator();
    }
  }

  /** get an instance of the requested service from a service provider associated
   * with this context.
   **/
  public Object getService(Object requestor, final Class serviceClass, final ServiceRevokedListener srl) {
    if (requestor == null) throw new IllegalArgumentException("null requestor");
    if (serviceClass == null) throw new IllegalArgumentException("null serviceClass");

    Object service;
    synchronized (services) {
      ServiceProvider sp = (ServiceProvider) services.get(serviceClass);
      if (sp == null) return null; // bail

      service = sp.getService(this, requestor, serviceClass);

       // if we're going to succeed and they passed a revoked listener...
      if (service != null && srl != null) {
        addServiceListener(new ServiceRevokedListener() {
            public void serviceRevoked(ServiceRevokedEvent re) {
              if (serviceClass.equals(re.getRevokedService()))
                srl.serviceRevoked(re);
            }
          });
      }
      
      return service;
    }
  }

  public void releaseService(Object requestor, Class serviceClass, Object service) {
    if (requestor == null) throw new IllegalArgumentException("null requestor");
    if (serviceClass == null) throw new IllegalArgumentException("null serviceClass");

    synchronized (services) {
      ServiceProvider sp = (ServiceProvider) services.get(serviceClass);
      if (sp != null) {
        sp.releaseService(this, requestor, serviceClass, service);
      }
    }
  }
}

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
package org.cougaar.core.component;

import java.util.*;
import java.lang.reflect.*;

/** A Wrapper Binder contructed by ServiceFilter which 
 * watches service requests and has convenient overridable points
 * where extensions may monitor, edit, veto or wrap services
 * requested by the client.
 * <p>
 * Specific implementations still will need to implement getBinderProxy
 * so the right methods are presented
 * <p>
 * For an example of using this, see the class org.cougaar.core.examples.PluginServiceFilter
 * in core/examples (part of the core cougaar documentation/development package).
 **/
public abstract class ServiceFilterBinder 
  extends BinderWrapper         // only uses part of it...
{
  protected ServiceFilterBinder(BinderFactory bf, Object child) {
    super(bf, child);
    myContainerProxy = createContainerProxy();
  }

  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    myFilteringServiceBroker = createFilteringServiceBroker(getServiceBroker());
  }
  
  private ContainerAPI myContainerProxy = null;

  /** define to choose the class of the BinderProxy.  
   * Should usually be an extension of ServiceFilterBinderProxy.
   **/
  protected abstract ContainerAPI createContainerProxy();

  /** Defines a pass-through insulation layer to ensure that the plugin cannot 
   * downcast the BindingSite to the Binder and gain control via introspection
   * and/or knowledge of the Binder class.  This is neccessary when Binders do
   * not have private channels of communication to the Container.
   **/
  protected ContainerAPI getContainerProxy() { return myContainerProxy; }

  private ServiceBroker myFilteringServiceBroker = null;
  
  /** Define to choose the implementatino of the service broker filter, which
   * should be an extension of FilteringServiceBroker.  This method is called
   * when setBindingSite is invoked.
   **/
  protected abstract ServiceBroker createFilteringServiceBroker(ServiceBroker sb);
  
  protected ServiceBroker getFilteringServiceBroker() { return myFilteringServiceBroker; }

  /** Base class for implementing filtering BinderProxies.
   * Binders will need to extend this with any additional methods required by 
   * their BindingSite.
   **/
  public class ServiceFilterContainerProxy 
    implements ContainerAPI
  {
    public ServiceFilterContainerProxy() {}
    public ServiceBroker getServiceBroker() {
      return getFilteringServiceBroker();
    }

    public void requestStop() {} // ignore
    public boolean remove(Object childComponent) {
      return ServiceFilterBinder.this.remove(childComponent);
    }
  }

  /** Base class for filtering/auditing/etc services, extending 
   * DelegatingServiceBroker for security.
   *
   * This implementation acts as a passthrough for publish/subscribe 
   * mechanisms
   **/
  public class FilteringServiceBroker 
    extends DelegatingServiceBroker
  {
    public FilteringServiceBroker(ServiceBroker delegate) {
      super(delegate);
    }

    /** A map of wrapped Service to tuples of requested client and actual service
     * to support releaseService.
     **/
    private ArrayList serviceTuples = new ArrayList(1);

    public class Tuple {
      public Object client;
      public Object clientProxy;
      public Class serviceClass;
      public Object service;
      public Object serviceProxy;
      public Tuple(Object r, Object cp, Class sc, Object s, Object sp) {
        client = r;
        clientProxy = cp;
        serviceClass = sc;
        service = s;
        serviceProxy = sp;
      }
      public Tuple(Object r, Class sc) { client=r; serviceClass = sc; }
      public Object getClient() { return client; }
      public Object getClientProxy() { return clientProxy; }
      public Class getServiceClass() { return serviceClass; }
      public Object getService() { return service; }
      public Object getServiceProxy() { return serviceProxy; }
      public Object getRequestedClient() {
        return (clientProxy!=null)?clientProxy:client;
      }
      public Object getReturnedService() {
        return (serviceProxy!=null)?serviceProxy:service;
      }
      public boolean equals(Object o) {
        return ((o instanceof Tuple) &&
                client.equals(((Tuple)o).client) &&
                serviceClass.equals(((Tuple)o).serviceClass));
      }
    }
      
    /**
     * This implementation of getService calls allowService(serviceClass).  If allowService 
     * returns true, will then call getClientProxy to proxy the client, delegates to the real broker
     * to request the service, then calls getServiceProxy to wrap the service. <p>
     **/
    public Object getService(Object requestor, Class serviceClass, ServiceRevokedListener srl) {
      if (allowService(serviceClass)) {
        Object clientProxy = getClientProxy(requestor, serviceClass);
        Object rc = (clientProxy != null)?clientProxy:requestor;

        Object service = super.getService(rc, serviceClass, srl);
        if (service == null) return null;

        Object serviceProxy = getServiceProxy(service, serviceClass, rc);
        Object rs = (serviceProxy != null)?serviceProxy:service;
        
        synchronized (serviceTuples) {
          serviceTuples.add(new Tuple(requestor,clientProxy,serviceClass,service,serviceProxy));
        }
        return rs;
      } else {
        // service request vetoed
        return null;
      }
    }

    /** Override to control if a given service class is allowed or now **/
    protected boolean allowService(Class serviceClass) {
      return true;
    }
    /** Override to specify an alternative instance to use as the client
     * (requestor) object when actually requesting the service.
     * A return value of null is interpreted as "no proxy".
     * This implementation always returns null.
     **/
    protected Object getClientProxy(Object client, Class serviceClass) {
      return null;
    }
    /** Override to specify an alternative instance to use as the service
     * implementation passed back to the client component.
     * A return value of null is interpreted as "no proxy".
     * This implementation always returns null.
     * @param client is the client object passed up to the real service broker.  
     * This is usually the requestor, but may be a proxy for the requestor if
     * getClientProxy was exercised.
     **/
    protected Object getServiceProxy(Object service, Class serviceClass, Object client) {
      return null;
    }

    /** As usual, releases a service previously requested from the ServiceBroker.
     * here, we do additional work to make sure that the service implementation
     * is actually released, regardless of the combination of proxies the
     * binder may have provided.
     * Calls releaseServiceProxy and releaseClientProxy as appropriate.
     **/
    public void releaseService(Object requestor, Class serviceClass, Object service) {
      Tuple t;
      synchronized (serviceTuples) {
        int i = serviceTuples.indexOf(new Tuple(requestor, serviceClass));
        if (i == -1) {
          // no such service, ignore the request.
          return;
        }
        t = (Tuple)serviceTuples.remove(i);
      }
      
      // release our service proxy
      Object sp = t.getServiceProxy();
      if (sp != null) releaseServiceProxy(sp, t.getService(), t.getServiceClass());

      // really release the service
      super.releaseService(t.getRequestedClient(), t.getServiceClass(), t.getReturnedService());

      // release our client proxy
      Object cp = t.getClientProxy();
      if (cp != null) releaseClientProxy(cp, t.getClient(), t.getServiceClass());
    }
    
    /** Called to release a serviceProxy previously constructed by the binder.
     * Override to change the default behavior of "do nothing".  Note that this
     * method is called <em>before</em> the real service is released.
     **/
    protected void releaseServiceProxy(Object serviceProxy, Object service, Class serviceClass) {
    }

    /** Called to release a clientProxy previously constructed by the binder.
     * Override to change the default behavior of "do nothing".  Note that this
     * method is called <em>after</em> the service is released.
     **/
    protected void releaseClientProxy(Object clientProxy, Object client, Class serviceClass) {
    }
  }
}

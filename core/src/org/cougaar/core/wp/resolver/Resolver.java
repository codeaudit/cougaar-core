/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp.resolver;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.wp.WhitePages; // inlined

/**
 * This is the client-side white pages resolver, which includes
 * subcomponents to:<ul>
 *   <li>cache fetched entries and lists</li>
 *   <li>bootstrap the WP</li>
 *   <li>batch requests</li>
 *   <li>renew binding leases</li>
 *   <li>send and receive messages</li>
 * </ul>
 * <p>
 * The subcomponents are pluggable to simply the configuration
 * and allow future enhancements.
 */
public class Resolver
extends ContainerSupport
{
  public static final String INSERTION_POINT = 
    WhitePages.INSERTION_POINT + ".Resolver";

  private ServiceBroker rootsb;
  private LoggingService logger;
  private MessageAddress agentId;
  private AgentIdentificationService agentIdService;

  private ServiceProvider wpsProxySP;

  private ServiceProvider handlerRegistrySP;

  // QuickSet<Handler>
  private final QuickSet handlers = new QuickSet();

  public void setNodeControlService(NodeControlService ncs) {
    if (ncs != null) {
      this.rootsb = ncs.getRootServiceBroker();
    }
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais != null) {
      this.agentId = ais.getMessageAddress();
    }
  }

  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    List l = new ArrayList();

    // add defaults -- order is very important!
    l.add(new ComponentDescription(
            "CacheEntries",
            INSERTION_POINT+".CacheEntries",
            "org.cougaar.core.wp.resolver.CacheEntriesHandler",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));
    l.add(new ComponentDescription(
            "CacheLists",
            INSERTION_POINT+".CacheLists",
            "org.cougaar.core.wp.resolver.CacheListsHandler",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));
    l.add(new ComponentDescription(
            "RMIBoot",
            INSERTION_POINT+".RMIBoot",
            "org.cougaar.core.wp.resolver.rmi.RMIBootstrapLookup",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));
    l.add(new ComponentDescription(
            "Lease",
            INSERTION_POINT+".Lease",
            "org.cougaar.core.wp.resolver.LeaseHandler",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));
    l.add(new ComponentDescription(
            "Config",
            INSERTION_POINT+".Config",
            "org.cougaar.core.wp.resolver.ConfigReader",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));
    l.add(new ComponentDescription(
            "Remote",
            INSERTION_POINT+".Remote",
            "org.cougaar.core.wp.resolver.RemoteHandler",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));


    // read config
    ServiceBroker sb = getServiceBroker();
    ComponentInitializerService cis = (ComponentInitializerService)
      sb.getService(this, ComponentInitializerService.class, null);
    try {
      ComponentDescription[] descs =
        cis.getComponentDescriptions(
            agentId.toString(),
            specifyContainmentPoint());
      int n = (descs == null ? 0 : descs.length);
      for (int i = 0; i < n; i++) {
        l.add(descs[i]);
      }
    } catch (ComponentInitializerService.InitializerException cise) {
      if (logger.isInfoEnabled()) {
        logger.info("\nUnable to add "+agentId+"'s components", cise);
      }
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    }

    return new ComponentDescriptions(l);
  }

  public void load() {
    if (logger.isDebugEnabled()) {
      logger.debug("Loading resolver");
    }

    handlerRegistrySP = new HandlerRegistrySP();
    getChildServiceBroker().addService(
        HandlerRegistryService.class, handlerRegistrySP);

    // advertise the white pages service
    wpsProxySP = new WPSProxySP();
    if (!rootsb.addService(WhitePagesService.class, wpsProxySP)) {
      if (logger.isErrorEnabled()) {
        logger.error("Failed WPS replace");
      }
    }

    super.load();

    if (logger.isInfoEnabled()) {
      logger.info("Loaded white pages resolver");
    }
  }

  public void unload() {
    super.unload();

    // release services
    ServiceBroker sb = getServiceBroker();

    if (agentIdService != null) {
      sb.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }

    if (handlerRegistrySP != null) {
      getChildServiceBroker().revokeService(
          HandlerRegistryService.class, handlerRegistrySP);
      handlerRegistrySP = null;
    }

    // revoke WPS override
    //
    // note: does this restore the root's WPS?  For now
    // we don't care
    if (wpsProxySP != null) {
      rootsb.revokeService(WhitePagesService.class, wpsProxySP);
      wpsProxySP = null;
    }
  }

  private void addHandler(Handler h) {
    if (logger.isDebugEnabled()) {
      logger.debug("Add handler: "+h);
    }
    handlers.add(h);
  }

  private void removeHandler(Handler h) {
    handlers.remove(h);
  }

  private Response mySubmit(Request req) {
    if (logger.isDetailEnabled()) {
      logger.detail("Resolver intercept wp request: "+req);
    }
    Response origRes = req.createResponse();
    Response res = origRes;
    for (Iterator iter = handlers.iterator();
        iter.hasNext();
        ) {
      Handler h = (Handler) iter.next();
      res = h.submit(res);
      if (res == null || res.isAvailable()) {
        if (logger.isDetailEnabled()) {
          // get class name
          String id = h.getClass().getName();
          int idx = id.lastIndexOf('.');
          if (idx > 0) {
            id = id.substring(idx+1);
          }
          logger.detail(id+" handled "+res);
        }
        return origRes;
      }
    }
    if (logger.isErrorEnabled()) {
      logger.error("Unhandled request: "+req);
    }
    return origRes;
  }

  private void myExecute(Request req, Object result, long ttl) {
    // validate
    if (req instanceof Request.Get) {
      if (logger.isErrorEnabled()) {
        logger.error(
            "Invalid response message!"+
            "  All outgoing \"get\" requests"+
            " are upgraded to \"getAll\": "+req);
      }
      return;
    }
    for (Iterator iter = handlers.iterator();
        iter.hasNext();
        ) {
      Handler h = (Handler) iter.next();
      h.execute(req, result, ttl);
    }
  }

  private class HandlerRegistrySP 
    implements ServiceProvider {
      private final HandlerRegistryService hrs =
        new HandlerRegistryService() {
          public void register(Handler h) {
            Resolver.this.addHandler(h);
          }
          public void unregister(Handler h) {
            Resolver.this.removeHandler(h);
          }
          public void execute(
              Request req, Object result, long ttl) {
            Resolver.this.myExecute(req, result, ttl);
          }
        };
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (HandlerRegistryService.class.isAssignableFrom(
              serviceClass)) {
          return hrs;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }

  private class WPSProxySP 
    implements ServiceProvider {
      private final WhitePagesService proxy = 
        new WhitePagesService() {
          public Response submit(Request req) {
            return Resolver.this.mySubmit(req);
          }
          public String toString() {
            return "proxy wps";
          }
        };
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (WhitePagesService.class.isAssignableFrom(serviceClass)) {
          return proxy;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
      public String toString() {
        return "WPS proxy ("+System.identityHashCode(proxy)+")";
      }
    }

  /**
   * A set that's cheap for thread-safe iterators.
   * <p>
   * I'm not sure if this is a good idea or not...
   */
  private class QuickSet {
    private final Object lock = new Object();
    private List l = Collections.EMPTY_LIST;
    public Iterator iterator() {
      synchronized (lock) {
        return l.iterator();
      }
    }
    public boolean add(Object o) {
      synchronized (lock) {
        int n = l.size();
        for (int i = 0; i < n; i++) {
          Object oi = l.get(i);
          if (o == null ?
              oi == null :
              o.equals(oi)) {
            return false;
          }
        }
        List t = new ArrayList(n+1);
        t.addAll(l);
        t.add(o);
        l = t; 
        return true;
      }
    }
    public boolean remove(Object o) {
      synchronized (lock) {
        int n = l.size();
        int i = -1;
        while (true) {
          if (++i >= n) {
            return false;
          }
          Object oi = l.get(i);
          if (o == null ?
              oi == null :
              o.equals(oi)) {
            break;
          }
        }
        List t = new ArrayList(n-1);
        for (int j = 0; j < n; j++) {
          if (j != i) {
            t.add(l.get(j));
          }
        }
        l = t; 
        return true;
      }
    }
    // etc Set methods if we cared...
  }

}

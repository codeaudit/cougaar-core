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

import java.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.node.*;
import org.cougaar.core.service.*;
import org.cougaar.core.service.wp.*;
import org.cougaar.core.wp.*;
import org.cougaar.core.wp.resolver.bootstrap.*;

/**
 * This is the client-side white pages resolver, which includes
 * subcomponents to:<ul>
 *   <li>bootstrap the WP</li>
 *   <li>send and receive messages</li>
 *   <li>batch requests</li>
 *   <li>renew binding leases</li>
 *   <li>cache fetched entries with a TTL</li>
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
  private WhitePagesService rootwps;
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

    // add defaults
    l.add(new ComponentDescription(
            "Boot",
            Bootstrap.INSERTION_POINT,
            "org.cougaar.core.wp.resolver.bootstrap.Bootstrap",
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

    rootwps = (WhitePagesService)
      rootsb.getService(this, WhitePagesService.class, null);
    if (rootwps == null) {
      throw new RuntimeException("Unable to obtain root WP");
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Got rootwps ("+rootwps+")");
    }

    handlerRegistrySP = new HandlerRegistrySP();
    getChildServiceBroker().addService(
        HandlerRegistryService.class, handlerRegistrySP);

    super.load();

    // override the root WPS
    wpsProxySP = new WPSProxySP();
    ServiceProvider dummySP = new ServiceProvider() {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        throw new InternalError();
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
        throw new InternalError();
      }
    };
    rootsb.revokeService(WhitePagesService.class, dummySP);
    if (!rootsb.addService(WhitePagesService.class, wpsProxySP)) {
      if (logger.isErrorEnabled()) {
        logger.error("Failed WPS replace");
      }
    }

    if (logger.isInfoEnabled()) {
      logger.info("Replaced root wp with resolver proxy: "+wpsProxySP);
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
    if (rootwps != null) {
      rootsb.releaseService(
          this, WhitePagesService.class, rootwps);
      rootwps = null;
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
    if (logger.isDebugEnabled()) {
      logger.debug("Resolver intercept wp request: "+req);
    }
    Response res = req.createResponse();
    for (Iterator iter = handlers.iterator();
        iter.hasNext();
        ) {
      Handler h = (Handler) iter.next();
      res = h.submit(res);
      if (res == null || res.isAvailable()) {
        return res;
      }
    }
    // throw away res, ask NS
    if (logger.isDebugEnabled()) {
      logger.debug("Delegate request to root wps");
    }
    return rootwps.submit(req);
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

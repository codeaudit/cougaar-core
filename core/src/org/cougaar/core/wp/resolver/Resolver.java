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
import java.util.List;
import org.cougaar.core.agent.Agent; // inlined
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.wp.RarelyModifiedList;

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
    Agent.INSERTION_POINT + ".WPClient";

  private ServiceBroker rootsb;
  private LoggingService logger;
  private MessageAddress agentId;

  private CacheService cacheService;
  private LeaseService leaseService;
  private ServiceProvider whitePagesSP;

  private BindObserverSP bindObserverSP;

  private RarelyModifiedList bindObservers = 
    new RarelyModifiedList();


  public void setNodeControlService(NodeControlService ncs) {
    rootsb = (ncs == null ? null : ncs.getRootServiceBroker());
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    List l = new ArrayList();

    // add defaults -- order is very important!
    l.add(new ComponentDescription(
            "ServerPool",
            INSERTION_POINT+".ServerPool",
            "org.cougaar.core.wp.resolver.SelectManager",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_INTERNAL));
    l.add(new ComponentDescription(
            "ClientTransport",
            INSERTION_POINT+".ClientTransport",
            "org.cougaar.core.wp.resolver.ClientTransport",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_INTERNAL));
    l.add(new ComponentDescription(
            "Lease",
            INSERTION_POINT+".Lease",
            "org.cougaar.core.wp.resolver.LeaseManager",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_INTERNAL));
    l.add(new ComponentDescription(
            "Cache",
            INSERTION_POINT+".Cache",
            "org.cougaar.core.wp.resolver.CacheManager",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_INTERNAL));
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
            "Config",
            INSERTION_POINT+".Config",
            "org.cougaar.core.wp.resolver.ConfigLoader",
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

    ServiceBroker sb = getServiceBroker();

    // which agent are we in?
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);

    ServiceBroker csb = getChildServiceBroker();

    // advertize our bind-observer service
    bindObserverSP = new BindObserverSP();
    csb.addService(BindObserverService.class, bindObserverSP);

    super.load();

    // now we can advertise to the node, since our bootstrappers
    // are now in place.
    rootsb.addService(WhitePagesService.class, whitePagesSP);

    if (logger.isInfoEnabled()) {
      logger.info("Loaded white pages resolver");
    }
  }

  protected void loadInternalPriorityComponents() {
    super.loadInternalPriorityComponents();

    ServiceBroker csb = getChildServiceBroker();

    // get the key services that should be created by our
    // subcomponents.
    cacheService = (CacheService)
      csb.getService(this, CacheService.class, null);
    if (cacheService == null) {
      throw new RuntimeException(
          "Unable to obtain CacheService");
    }
    leaseService = (LeaseService)
      csb.getService(this, LeaseService.class, null);
    if (leaseService == null) {
      throw new RuntimeException(
          "Unable to obtain LeaseService");
    }

    // we can advertize our white pages to our subcomponents,
    // to allow bootstrapping
    //
    // we shouldn't advertize this to the root service broker
    // yet, since we haven't finished loading ('though it'd
    // probably work).
    whitePagesSP = new WhitePagesSP();
    csb.addService(WhitePagesService.class, whitePagesSP);
  }

  public void unload() {
    super.unload();

    // release services
    ServiceBroker csb = getChildServiceBroker();

    // revoke white pages service
    if (whitePagesSP != null) {
      rootsb.revokeService(WhitePagesService.class, whitePagesSP);
      whitePagesSP = null;
    }

    if (leaseService != null) {
      csb.releaseService(
          this, LeaseService.class, leaseService);
      leaseService = null;
    }
    if (cacheService != null) {
      csb.releaseService(
          this, CacheService.class, cacheService);
      cacheService = null;
    }

    // revoke bind observers
    if (bindObserverSP != null) {
      csb.revokeService(BindObserverService.class, bindObserverSP);
      bindObserverSP = null;
    }

    ServiceBroker sb = getServiceBroker();

    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }
  }

  private Response submit(Request req, String agent) {
    if (logger.isDetailEnabled()) {
      logger.detail("Resolver intercept wp request: "+req);
    }
    Response res = req.createResponse();

    cacheService.submit(res);

    boolean bind = 
      (req instanceof Request.Bind ||
       req instanceof Request.Unbind);
    if (bind || req instanceof Request.Flush) {
      tellObservers(req);
    }

    if (bind) {
      leaseService.submit(res, agent);
    }
    return res;
  }

  private void register(BindObserverService.Client bosc) {
    bindObservers.add(bosc);
  }
  private void unregister(BindObserverService.Client bosc) {
    bindObservers.remove(bosc);
  }
  private void tellObservers(Request req) {
    List l = bindObservers.getList();
    for (int i = 0, n = l.size(); i < n; i++) {
      BindObserverService.Client bosc =
        (BindObserverService.Client) l.get(i);
      bosc.submit(req);
    }
  }

  private class WhitePagesSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!WhitePagesService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        String s;
        if (requestor instanceof ResolverClient) {
          s = ((ResolverClient) requestor).getAgent();
        } else {
          s = null; // assume it's our node-agent
        }
        final String agent = s;
        if (logger.isDetailEnabled()) {
          logger.detail(
              "giving root WP (agent="+agent+") to "+requestor);
        }
        return new WhitePagesService() {
          public Response submit(Request req) {
            return Resolver.this.submit(req, agent);
          }
        };
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }

  private class BindObserverSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!BindObserverService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        if (!(requestor instanceof BindObserverService.Client)) {
          throw new IllegalArgumentException(
              "BindObserverService"+
              " requestor must implement "+
              "BindObserverService.Client");
        }
        BindObserverService.Client client = (BindObserverService.Client) requestor;
        BindObserverServiceImpl usi = new BindObserverServiceImpl(client);
        Resolver.this.register(client);
        return usi;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
        if (!(service instanceof BindObserverServiceImpl)) {
          return;
        }
        BindObserverServiceImpl usi = (BindObserverServiceImpl) service;
        BindObserverService.Client client = usi.client;
        Resolver.this.unregister(client);
      }
      private class BindObserverServiceImpl 
        implements BindObserverService {
          private final Client client;
          public BindObserverServiceImpl(Client client) {
            this.client = client;
          }
        }
    }
 
}

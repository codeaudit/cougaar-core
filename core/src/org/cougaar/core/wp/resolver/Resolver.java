/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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
  private LoggingService log;

  private CacheService cacheService;
  private LeaseService leaseService;
  private ServiceProvider whitePagesSP;

  private BindObserverSP bindObserverSP;

  private RarelyModifiedList bindObservers = 
    new RarelyModifiedList();


  public void setNodeControlService(NodeControlService ncs) {
    rootsb = (ncs == null ? null : ncs.getRootServiceBroker());
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    List l = new ArrayList();

    // add defaults -- order is very important!
    l.add(new ComponentDescription(
            "org.cougaar.core.wp.resolver.SelectManager",
            INSERTION_POINT+".Component",
            "org.cougaar.core.wp.resolver.SelectManager",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_INTERNAL));
    l.add(new ComponentDescription(
            "org.cougaar.core.wp.resolver.ClientTransport",
            INSERTION_POINT+".Component",
            "org.cougaar.core.wp.resolver.ClientTransport",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_INTERNAL));
    l.add(new ComponentDescription(
            "org.cougaar.core.wp.resolver.LeaseManager",
            INSERTION_POINT+".Component",
            "org.cougaar.core.wp.resolver.LeaseManager",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_INTERNAL));
    l.add(new ComponentDescription(
            "org.cougaar.core.wp.resolver.CacheManager",
            INSERTION_POINT+".Component",
            "org.cougaar.core.wp.resolver.CacheManager",
            null,
            "",
            null,
            null,
            null,
            ComponentDescription.PRIORITY_INTERNAL));
    l.add(new ComponentDescription(
            "org.cougaar.core.wp.resolver.rmi.RMIBootstrapLookup",
            INSERTION_POINT+".Component",
            "org.cougaar.core.wp.resolver.rmi.RMIBootstrapLookup",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));
    l.add(new ComponentDescription(
            "org.cougaar.core.wp.resolver.ConfigLoader",
            INSERTION_POINT+".Component",
            "org.cougaar.core.wp.resolver.ConfigLoader",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));

    ServiceBroker sb = getServiceBroker();

    // find our local agent
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    MessageAddress localAgent = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);

    // read config
    ComponentInitializerService cis = (ComponentInitializerService)
      sb.getService(this, ComponentInitializerService.class, null);
    try {
      ComponentDescription[] descs =
        cis.getComponentDescriptions(
            localAgent.toString(),
            specifyContainmentPoint());
      int n = (descs == null ? 0 : descs.length);
      for (int i = 0; i < n; i++) {
        l.add(descs[i]);
      }
    } catch (ComponentInitializerService.InitializerException cise) {
      if (log.isInfoEnabled()) {
        log.info("\nUnable to add "+localAgent+"'s components", cise);
      }
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    }

    return new ComponentDescriptions(l);
  }

  public void load() {
    if (log.isDebugEnabled()) {
      log.debug("Loading resolver");
    }

    ServiceBroker sb = getServiceBroker();

    ServiceBroker csb = getChildServiceBroker();

    // advertize our bind-observer service
    bindObserverSP = new BindObserverSP();
    csb.addService(BindObserverService.class, bindObserverSP);

    super.load();

    // now we can advertise to the node, since our bootstrappers
    // are now in place.
    rootsb.addService(WhitePagesService.class, whitePagesSP);

    if (log.isInfoEnabled()) {
      log.info("Loaded white pages resolver");
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

    if (log != null) {
      sb.releaseService(
          this, LoggingService.class, log);
      log = null;
    }
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
        if (WhitePagesService.class.isAssignableFrom(serviceClass)) {
          String agent =
            ((requestor instanceof ResolverClient) ?
             ((ResolverClient) requestor).getAgent() :
             null); // assume it's our node-agent
          return new WhitePagesS(agent);
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }
  private class WhitePagesS
    extends WhitePagesService {

      private final String agent;

      public WhitePagesS(String agent) {
        this.agent = agent;
      }

      public Response submit(Request req) {
        if (log.isDetailEnabled()) {
          log.detail("Resolver intercept wp request: "+req);
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

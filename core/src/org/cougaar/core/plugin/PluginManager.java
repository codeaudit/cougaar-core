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

package org.cougaar.core.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.Binder;
import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BinderFactorySupport;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BoundComponent;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ComponentFactory;
import org.cougaar.core.component.ContainedBinderSupport;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.PropagatingServiceBroker;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ComponentRuntimeException;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.util.ConfigFinder;

/** A container for Plugin Components.
 **/
public class PluginManager 
  extends ContainerSupport
  implements StateObject
{
  /** The insertion point for a PluginManager, defined relative to its parent, Agent. **/
  public static final String INSERTION_POINT = Agent.INSERTION_POINT + ".PluginManager";

  private Object loadState = null;
  private ServiceBroker sb;
  private LoggingService logger;
  private MessageAddress agentId;
  private AgentIdentificationService agentIdService;

  public PluginManager() {
    if (!attachBinderFactory(new DefaultPluginBinderFactory())) {
      throw new RuntimeException("Failed to load the DefaultPluginBinderFactory");
    }
  }
  
  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    sb = bs.getServiceBroker();
    setChildServiceBroker(new PluginManagerServiceBroker(bs));
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais == null) {
      // Revocation
    } else {
      this.agentId = ais.getMessageAddress();
    }
  }

  public void setState(Object loadState) {
    this.loadState = loadState;
  }

  /** Get the components from the ComponentInitializerService or the state **/
  protected ComponentDescriptions findExternalComponentDescriptions() {
    if (loadState instanceof StateTuple[]) {
      StateTuple[] ls = (StateTuple[])loadState;
      int len = ls.length;
      List l = new ArrayList(ls.length);
      for (int i=0; i<len; i++) {
        l.add(ls[i].getComponentDescription());
      }
      return new ComponentDescriptions(l);
    } else {
      // display the agent id
      String cname = agentId.toString();

      ServiceBroker sb = getServiceBroker();
      ComponentInitializerService cis = (ComponentInitializerService)
        sb.getService(this, ComponentInitializerService.class, null);
      try {
        return new ComponentDescriptions(
            cis.getComponentDescriptions(cname, specifyContainmentPoint()));
      } catch (ComponentInitializerService.InitializerException cise) {
        if (logger.isInfoEnabled()) {
          logger.info("\nUnable to add "+cname+"'s plugins ",cise);
        }
        return null;
      } finally {
        sb.releaseService(this, ComponentInitializerService.class, cis);
      }
    }
  }

  public boolean add(Object o) {
    try {
      if (logger.isInfoEnabled()) {
        logger.info("Agent "+agentId+" adding plugin "+o);
      }
      boolean result = super.add(o);
      if (logger.isInfoEnabled()) {
        logger.info("Agent "+agentId+" added plugin "+o);
      }
      return result;
    } catch (ComponentRuntimeException cre) {
      Throwable cause = cre.getCause();
      if (cause == null) cause = cre;
      logger.error("Failed to add "+o+" to "+this, cause);
      throw cre;
    } catch (RuntimeException re) {
      //logger.error("Failed to add "+o+" to "+this, re);
      throw re;
    }
  }

  public Object getState() {
    synchronized (boundComponents) {
      int n = boundComponents.size();
      StateTuple[] tuples = new StateTuple[n];
      for (int i = 0; i < n; i++) {
        BoundComponent bc = (BoundComponent)boundComponents.get(i);
        Object comp = bc.getComponent();
        if (comp instanceof ComponentDescription) {
          ComponentDescription cd = (ComponentDescription)comp;
          Binder b = bc.getBinder();
          Object state = b.getState();
          tuples[i] = new StateTuple(cd, state);
        } else {
          // error?
        }
      }
      return tuples;
    } 
  }

  //
  // binding services
  //

  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  private ContainerAPI containerProxy = 
    new ContainerAPI() {
        public ServiceBroker getServiceBroker() {
          return PluginManager.this.getServiceBroker();
        }
        public boolean remove(Object childComponent) {
          return PluginManager.this.remove(childComponent);
        }
        public void requestStop() {}
      };

  protected ContainerAPI getContainerProxy() {
    return containerProxy;
  }

  //
  // typical implementations of state transitions --
  //   these might be moved into a base class...
  //
  // We really need a "container.lock()" to make these
  //   operations safe.  Mobility would like to lock down
  //   multiple steps, e.g. "suspend(); stop(); ..", without
  //   another Thread calling "add(..)" in between.
  //   

  public void suspend() {
    super.suspend();

    // suspend all children
    List childBinders = listBinders();
    for (int i = childBinders.size() - 1; i >= 0; i--) {
      Binder b = (Binder) childBinders.get(i);
      b.suspend();
    }
  }

  public void resume() {
    super.resume();

    // resume all children
    List childBinders = listBinders();
    for (int i = 0, n = childBinders.size(); i < n; i++) {
      Binder b = (Binder) childBinders.get(i);
      b.resume();
    }
  }

  public void stop() {
    super.stop();

    // stop all children
    List childBinders = listBinders();
    for (int i = childBinders.size() - 1; i >= 0; i--) {
      Binder b = (Binder) childBinders.get(i);
      b.stop();
    }
  }

  public void halt() {
    // this seems reasonable:
    suspend();
    stop();
  }

  public void unload() {
    super.unload();

    // unload children
    List childBinders = listBinders();
    for (int i = childBinders.size() - 1; i >= 0; i--) {
      Binder b = (Binder) childBinders.get(i);
      b.unload();
    }
    // release services
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
  }

  //
  // support classes
  //

  private static class DefaultPluginBinderFactory
    extends BinderFactorySupport {

      public Binder getBinder(Object child) {
        return new DefaultPluginBinder(this, child);
      }

      // publish the ContainedService to the subcomponent
      private static class DefaultPluginBinder 
        extends ContainedBinderSupport {
          /** All subclasses must implement a matching constructor. **/
          public DefaultPluginBinder(BinderFactory bf, Object child) {
            super(bf, child);
          }

          protected BindingSite getBinderProxy() {
            return new PluginBindingSiteImpl();
          }

          /** Implement the binding site delegate **/
          protected class PluginBindingSiteImpl implements BindingSite {
            public final ServiceBroker getServiceBroker() {
              return DefaultPluginBinder.this.getServiceBroker();
            }
            public final void requestStop() {
              DefaultPluginBinder.this.requestStop();
            }
          }
        }
      /*
      // old stateless "Plugin" support, now disabled!
      private final static ComponentFactory pluginCF = new PurePluginFactory();
      public final ComponentFactory getComponentFactory() {
      return pluginCF;
      }
       */
    }

  private static class PluginManagerServiceBroker 
    extends PropagatingServiceBroker 
    {
      public PluginManagerServiceBroker(BindingSite bs) {
        super(bs);
    }
  }
  
  // 
  // other services
  //
  
  public MessageAddress getMessageAddress() {
    return agentId;
  }
  public MessageAddress getAgentIdentifier() {
    return agentId;
  }
  public ConfigFinder getConfigFinder() {
    return ConfigFinder.getInstance(); // FIXME replace with service
  }
  public String toString() {
    return agentId+"/PluginManager";
  }

}

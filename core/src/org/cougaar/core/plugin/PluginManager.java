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
package org.cougaar.core.plugin;

import java.io.InputStream;
import java.util.*;
import org.cougaar.util.*;
import org.cougaar.util.log.*;
import org.cougaar.core.agent.AgentChildBindingSite;
import org.cougaar.core.component.*;
import org.cougaar.core.node.InitializerService;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.mts.MessageAddress;
import java.beans.*;
import java.lang.reflect.*;


/** A container for Plugin Components.
 * <p>
 * A PluginManager expects all subcomponents to be bound with 
 * implementations of PluginBinder.  In return, the PluginManager
 * offers the PluginManagerForBinder to each PluginBinder.
 **/
public class PluginManager 
  extends ContainerSupport
  implements StateObject
{

  private Object loadState = null;
  
  private final Logger logger;


  public PluginManager() {
    if (!attachBinderFactory(new DefaultPluginBinderFactory())) {
      throw new RuntimeException("Failed to load the DefaultPluginBinderFactory");
    }
    logger = Logging.getLogger(this.getClass());
  }

  protected final Logger getLogger() { return logger; }

  private AgentChildBindingSite bindingSite = null;
  
  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    if (bs instanceof AgentChildBindingSite) {
      bindingSite = (AgentChildBindingSite) bs;
      setChildServiceBroker(new PluginManagerServiceBroker(bs));
    } else {
      throw new RuntimeException("Tried to load "+this+"into " + bs);
    }
  }

  public void setState(Object loadState) {
    this.loadState = loadState;
  }

  /** Get the components from the InitializerService or the state **/
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
      MessageAddress cid = getBindingSite().getAgentIdentifier();
      String cname = cid.toString();

      ServiceBroker sb = getServiceBroker();
      InitializerService is = (InitializerService) sb.getService(this, InitializerService.class, null);
      try {
        return new ComponentDescriptions(is.getComponentDescriptions(cname, specifyContainmentPoint()));
      } catch (Exception e) {
        System.err.println("\nUnable to add "+cname+"'s child Components: "+e);
        e.printStackTrace();
        return null;
      } finally {
        sb.releaseService(this, InitializerService.class, is);
      }
    }
  }

  public boolean add(Object o) {
    try {
      return super.add(o);
    } catch (RuntimeException re) {
      getLogger().error("Failed to add "+o+" to "+this, re);
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

  protected final AgentChildBindingSite getBindingSite() {
    return bindingSite;
  }
  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return "Node.AgentManager.Agent.PluginManager";
  }

  private PluginManagerForBinder containerProxy = 
    new PluginManagerForBinder() {
        public ServiceBroker getServiceBroker() {
          return PluginManager.this.getServiceBroker();
        }
        public boolean remove(Object childComponent) {
          return PluginManager.this.remove(childComponent);
        }
        public void requestStop() {}
        public MessageAddress getAgentIdentifier() {
          return PluginManager.this.getAgentIdentifier();
        }
        public ConfigFinder getConfigFinder() {
          return PluginManager.this.getConfigFinder();
        }
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
  }

  //
  // support classes
  //

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
  
  public ClusterIdentifier getClusterIdentifier() {
    return (ClusterIdentifier)getAgentIdentifier();
  }
  public MessageAddress getAgentIdentifier() {
    return getBindingSite().getAgentIdentifier();
  }
  public ConfigFinder getConfigFinder() {
    return getBindingSite().getConfigFinder();
  }
  public String toString() {
    return getAgentIdentifier().toString()+"/PluginManager";
  }

}

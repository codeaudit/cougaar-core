/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.plugin;

import java.io.InputStream;
import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.agent.PluginManagerBindingSite;
import org.cougaar.core.component.*;
import org.cougaar.core.cluster.ClusterIdentifier;
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

  public PluginManager() {
    if (!attachBinderFactory(new DefaultPluginBinderFactory())) {
      throw new RuntimeException("Failed to load the DefaultPluginBinderFactory");
    }
  }

  private PluginManagerBindingSite bindingSite = null;
  
  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    if (bs instanceof PluginManagerBindingSite) {
      bindingSite = (PluginManagerBindingSite) bs;
      setChildServiceBroker(new PluginManagerServiceBroker(bs));
    } else {
      throw new RuntimeException("Tried to load "+this+"into " + bs);
    }
  }

  public void setState(Object loadState) {
    this.loadState = loadState;
  }

  public void load() {
    super.load();
    // add services here (none for now)

    // display the agent id
    ClusterIdentifier cid = getBindingSite().getAgentIdentifier();
    String cname = cid.toString();
    System.err.println("\n PluginManager "+this+" loading Plugins for agent "+cname);
    
    // get an array of child Components
    Object[] children;
    if (loadState instanceof StateTuple[]) {
      // use the existing state
      children = (StateTuple[])loadState;
      loadState = null;
    } else {
      try {
        // parse the cluster properties
        // currently assume ".ini" files
        InputStream in = ConfigFinder.getInstance().open(cname+".ini");
        children = 
          org.cougaar.core.society.INIParser.parse(
              in, 
              "Node.AgentManager.Agent.PluginManager");
      } catch (Exception e) {
        System.err.println(
            "\nUnable to add "+cname+"'s child Components: "+e);
        e.printStackTrace();
        children = null;
      }
    }

    // load the child Components (Plugins, etc)
    int n = ((children != null) ? children.length : 0);
    for (int i = 0; i < n; i++) {
      add(children[i]);
    }
  }

  //
  //
  //

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

  protected final PluginManagerBindingSite getBindingSite() {
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
        public ClusterIdentifier getAgentIdentifier() {
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
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
      b.suspend();
    }
  }

  public void resume() {
    super.resume();
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
      b.resume();
    }
  }

  public void stop() {
    super.stop();
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
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
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
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
    return getAgentIdentifier();
  }
  public ClusterIdentifier getAgentIdentifier() {
    return getBindingSite().getAgentIdentifier();
  }
  public ConfigFinder getConfigFinder() {
    return getBindingSite().getConfigFinder();
  }
  public String toString() {
    return getAgentIdentifier().toString()+"/PluginManager";
  }

}

















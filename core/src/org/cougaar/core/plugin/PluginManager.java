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

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.cluster.*;
import org.cougaar.core.mts.MessageTransportService;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
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
{
  private ClusterImpl agent = null; // stopgap hack

  ClusterImpl getAgent() { return agent; }

  public PluginManager() {
    if (!attachBinderFactory(new DefaultPluginBinderFactory())) {
      throw new RuntimeException("Failed to load the DefaultPluginBinderFactory");
    }
  }

  /** this constructor used for backwards compatability mode.  Goes away
   * when we are a contained by an Agent component
   **/
  public PluginManager(ClusterImpl agent) {
    this.agent = agent;
    if (!attachBinderFactory(new DefaultPluginBinderFactory())) {
      throw new RuntimeException("Failed to load the DefaultPluginBinderFactory");
    }
    
  }
  
  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    setChildServiceBroker(new PluginManagerServiceBroker(bs));
  }

  public void initialize() {
    super.initialize();
    ServiceBroker sb = getServiceBroker();
    //add services here (none for now)
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
        public UIDServer getUIDServer() {
          return PluginManager.this.getUIDServer();
        }
        public ConfigFinder getConfigFinder() {
          return PluginManager.this.getConfigFinder();
        }
      };

  protected ContainerAPI getContainerProxy() {
    return containerProxy;
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
  

  // standin API for LDMService called by PluginBinder for temporary support
  void addPrototypeProvider(PrototypeProvider plugin) {
    agent.addPrototypeProvider(plugin);
  }
  void addPropertyProvider(PropertyProvider plugin) {
    agent.addPropertyProvider(plugin);
  }
  void addLatePropertyProvider(LatePropertyProvider plugin) {
    agent.addLatePropertyProvider(plugin);
  }



  // 
  // other services
  //
  
  public ClusterIdentifier getClusterIdentifier() {
    return agent.getClusterIdentifier();
  }

  public ClusterIdentifier getAgentIdentifier() {
    return agent.getClusterIdentifier();
  }

  public UIDServer getUIDServer() {
    return agent.getUIDServer();
  }

  public LDMServesPlugIn getLDM() {
    return agent.getLDM();
  }

  public ConfigFinder getConfigFinder() {
    return agent.getConfigFinder();
  }

  public String toString() {
    return getAgentIdentifier().toString()+"/PluginManager";
  }

}


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
import org.cougaar.core.society.MessageTransportServer;
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
    
    /*
    if (! attachBinderFactory(new PluginServiceFilter())) {
      throw new RuntimeException("Failed to load the PluginServiceFilter");
    }
    */

    ServiceBroker sb = childServiceBroker;
    //ServiceBroker sb = testsb; // test
    // add some services for the plugins.
    sb.addService(MetricsService.class, new MetricsServiceProvider(agent));
    sb.addService(AlarmService.class, new AlarmServiceProvider(agent));
    sb.addService(BlackboardService.class, new BlackboardServiceProvider(agent.getDistributor()) );
    sb.addService(MessageTransportServer.class, new MessageTransportServiceProvider(agent));
    sb.addService(SharedThreadingService.class, new SharedThreadingServiceProvider(agent.getClusterIdentifier()));
    // hack service for demo control
    sb.addService(DemoControlService.class, new DemoControlServiceProvider(agent));

    // scheduler for new plugins
    sb.addService(SchedulerService.class, new SchedulerServiceProvider(agent));

    // placeholder for LDM Services
    sb.addService(LDMService.class, new LDMServiceProvider(agent));
  }

  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return "Node.AgentManager.Agent.PluginManager";
  }

  //private ServiceBroker testsb;
  protected ServiceBroker specifyChildServiceBroker() {
    //testsb=new PluginManagerServiceBroker();
    //return new DelegatingServiceBroker(new PropagatingServiceBroker(testsb));

    // We should really be doing something like:
    //    return new PropagatingServiceBroker(getBindingSite().getServiceBroker());
    return new PluginManagerServiceBroker();
  }

  protected Class specifyChildBindingSite() {
    return PluginBindingSite.class;
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

  private static class PluginManagerServiceBroker extends ServiceBrokerSupport {}
  
  //
  // support for direct loading of old-style plugins
  //
  
  public boolean add(Object o) {
    // this could go away if we turned the message into a ComponentDescription object.
    if (o instanceof AddPlugInMessage ) {
      AddPlugInMessage message = (AddPlugInMessage) o;
      String pluginclass = message.getPlugIn();
      PlugInServesCluster newPlugIn = null;
      Vector args = message.getArguments();
      System.err.println("Got AddPluginMessage: "+o);
      ComponentDescription cd = new ComponentDescription("Plugin-"+pluginclass, 
                                                         "Node.AgentManager.Agent.PluginManager.plugin",
                                                         pluginclass,
                                                         null,
                                                         args,
                                                         null,
                                                         null,
                                                         null);
      return super.add(cd);
    } else {
      return super.add(o);
    }
  }

  public boolean remove(Object o) {
    if (o instanceof RemovePlugInMessage) {
      String theClassName = ((RemovePlugInMessage)o).getPlugIn();
      System.err.println("RemovePlugin Message is disabled: "+theClassName);
      return false;
    } else {
      return super.remove(o);
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


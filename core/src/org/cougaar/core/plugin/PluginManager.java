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
    if (!loadComponent(new DefaultPluginBinderFactory())) {
      throw new RuntimeException("Failed to load the DefaultPluginBinderFactory");
    }
  }

  /** this constructor used for backwards compatability mode.  Goes away
   * when we are a contained by an Agent component
   **/
  public PluginManager(ClusterImpl agent) {
    this.agent = agent;
    if (!loadComponent(new DefaultPluginBinderFactory())) {
      throw new RuntimeException("Failed to load the DefaultPluginBinderFactory");
    }

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
        public ServiceBroker getChildServiceBroker() {
          return PluginManager.this.getChildServiceBroker();
        }
        public boolean remove(Object childComponent) {
          return PluginManager.this.remove(childComponent);
        }
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
  // implement the API needed by plugin binders
  //

  /** Makes the child services available to child binders.
   * should use package protection to give access only to PluginBinderSupport,
   * but makes it public for use by Test example.
   **/
  public final ServiceBroker getChildServiceBroker() {
    return childServiceBroker;
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
      try {
        Vector args = message.getArguments();

        // get the class of the plugin
        Class pc = Class.forName(pluginclass);
        if (PlugIn.class.isAssignableFrom(pc)) {
          // is a stateless plugin
          newPlugIn = new StatelessPlugInAdapter(getPurePlugIn(pc));
        } else {
          //newPlugIn = (PlugInServesCluster)Beans.instantiate(getClass().getClassLoader(), pluginclass);
          Class c = Class.forName(pluginclass);
          newPlugIn = (PlugInServesCluster) c.newInstance();
        }

        if (newPlugIn instanceof ParameterizedPlugIn) {
          if (args != null) {
            Class pic = newPlugIn.getClass();
            Method m = pic.getMethod("setParameter", new Class[]{Object.class});
            m.invoke(newPlugIn, new Object[]{args});
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Could not instantiate "+pluginclass+": "+e);
      }

      if (newPlugIn == null) 
        throw new RuntimeException("Could not instantiate "+pluginclass);
      try {
        boolean succ =  super.add(newPlugIn);
        //System.err.println("Loaded a "+pluginclass);
        return succ;
      } catch(Throwable ex) {
        synchronized (System.err) {
          System.err.println("Failed to load "+pluginclass+":\n"+ex);
          if (!(ex instanceof ClassNotFoundException))
            ex.printStackTrace();
        }
      }
      return false;
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
  // class hackery for old-style pure plugin caching
  //

  private static final HashMap purePlugIns = new HashMap(11);
  private static PlugIn getPurePlugIn(Class c) {
    synchronized (purePlugIns) {
      PlugIn plugin = (PlugIn)purePlugIns.get(c);
      if (plugin == null) {
        try {
          plugin = (PlugIn) c.newInstance();
          purePlugIns.put(c, plugin);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      return plugin;
    }
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

}


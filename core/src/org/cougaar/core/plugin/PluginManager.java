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
{

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

  public void initialize() {
    super.initialize();
    ServiceBroker sb = getServiceBroker();
    //add services here (none for now)

    //try to load this agents clusters from the ini files
    ClusterIdentifier cid = getBindingSite().getAgentIdentifier();
    String cname = cid.toString();
    System.err.println("\n PluginManager "+this+" loading Plugins for agent "+cname);
    
    try {
      // parse the cluster properties
      // currently assume ".ini" files
      InputStream in = ConfigFinder.getInstance().open(cname+".ini");
      ComponentDescription[] cDescs = 
        org.cougaar.core.society.INIParser.parse(in, "Node.AgentManager.Agent.PluginManager");

      for (int j = 0; j < cDescs.length; j++) {
        add(cDescs[j]);
      }
    } catch (Exception e) {
      System.err.println("\nUnable to add "+cname+"'s child omponents: "+e);
      e.printStackTrace();
    }
  }

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

















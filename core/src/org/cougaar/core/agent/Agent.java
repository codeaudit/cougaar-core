/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.agent;

import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.util.*;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.UIDServer;


/** 
 * This is the basic class required for
 * implementing an Agent.
 **/
public abstract class Agent 
  extends ContainerSupport
  implements ContainerAPI 
{

  public Agent() {
    if (!loadComponent(new PluginManagerBinderFactory())) {
      throw new RuntimeException("Failed to load the PluginManagerBinderFactory");
    }
  }

   /** this constructor used for backwards compatability mode.  Goes away
   * when we are a contained by a Node component
   **/
  public Agent(ComponentDescription comdesc) {
    if (!loadComponent(new PluginManagerBinderFactory())) {
      throw new RuntimeException("Failed to load the PluginManagerBinderFactory");
    }
    //no agent services for now... all are loaded from cluster (specific type of agent)
  
  }

  // Do we need state model stuff here???
  protected void initialize() {}

  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return "Node.AgentManager.Agent";
  }
  protected ServiceBroker specifyChildContext() {
    return new AgentServiceBroker();
  }

  protected ServiceBroker specifyChildServiceBroker() {
    return new AgentServiceBroker();
  }
  protected Class specifyChildBindingSite() {
    return PluginManagerBindingSite.class;
  }

  protected Object getBinderFactoryProxy() {
    return this;
  }
  protected ContainerAPI getContainerProxy() {
    return this;
  }

  //backwards compatability to attach to Cluster Agents for now
  abstract public ClusterIdentifier getAgentIdentifier();
  abstract public UIDServer getUIDServer();
  abstract public ConfigFinder getConfigFinder();

  //
  // implement the API needed by agent binders
  //

  public final void requestStop() {} // used to satisfy ContainerAPI of BindingSite 
  //
  // support classes
  //

  private static class AgentServiceBroker extends ServiceBrokerSupport {
  
    public boolean add(Object o) {
      //??where we add pluginmanager and others??
      return true;
    }

    public boolean remove(Object o) {
      //stubbed for now
      return true;
    }
  }


}

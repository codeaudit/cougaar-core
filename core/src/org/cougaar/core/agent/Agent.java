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


/** 
 * This is the basic class required for
 * implementing an Agent.
 **/
public abstract class Agent 
  extends ContainerSupport
  implements ContainerAPI 
{

  public Agent() {
    BinderFactory pmbf = new PluginManagerBinderFactory();
    if (!attachBinderFactory(pmbf)) {
      throw new RuntimeException("Failed to load the PluginManagerBinderFactory");
    }
  }

   /** this constructor used for backwards compatability mode.  Goes away
   * when we are a contained by a Node component
   **/
  public Agent(ComponentDescription comdesc) {
    BinderFactory pmbf = new PluginManagerBinderFactory();
    if (!attachBinderFactory(pmbf)) {
      throw new RuntimeException("Failed to load the PluginManagerBinderFactory");
    }
    //no agent services for now... all are loaded from cluster (specific type of agent)
  
  }

  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    setChildServiceBroker(new AgentServiceBroker(bs));
  }

  // Do we need state model stuff here???
  //protected void initialize() {}

  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return "Node.AgentManager.Agent";
  }

  protected Object getBinderFactoryProxy() {
    return this;
  }
  protected ContainerAPI getContainerProxy() {
    return this;
  }

  //backwards compatability to attach to Cluster Agents for now
  abstract public ClusterIdentifier getAgentIdentifier();
  abstract public ConfigFinder getConfigFinder();

  //
  // implement the API needed by agent binders
  //

  public final void requestStop() {} // used to satisfy ContainerAPI of BindingSite 
  //
  // support classes
  //

  private static class AgentServiceBroker extends PropagatingServiceBroker {
    public AgentServiceBroker(BindingSite bs) {
      super(bs);
    }
  }
}




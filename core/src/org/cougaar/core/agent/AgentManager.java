/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.agent;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.cluster.*;
import org.cougaar.core.society.MessageTransportServer;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.domain.planning.ldm.DomainServiceProvider;
import org.cougaar.domain.planning.ldm.DomainService;
import org.cougaar.domain.planning.ldm.PrototypeRegistryService;
import org.cougaar.domain.planning.ldm.PrototypeRegistryServiceProvider;
import org.cougaar.core.cluster.UIDService;
import org.cougaar.core.cluster.UIDServiceProvider;

import java.beans.*;
import java.lang.reflect.*;


/** A container for Agent Components.
 **/
public class AgentManager 
  extends ContainerSupport
  implements ContainerAPI
{
    public AgentManager() {
    if (!loadComponent(new AgentBinderFactory())) {
      throw new RuntimeException("Failed to load the AgentBinderFactory");
    }
  }

  /** this constructor used for backwards compatability mode.  Goes away
   * when we are a contained by a Node component
   **/
  public AgentManager(ComponentDescription comdesc) {
    if (!loadComponent(new AgentBinderFactory())) {
      throw new RuntimeException("Failed to load the AgentBinderFactory");
    }

    // add some services for the agents (clusters).
    //childServiceBroker.addService(MetricsService.class, new MetricsServiceProvider(agent));
    //childServiceBroker.addService(BlackboardService.class, new BlackboardServiceProvider(agent.getDistributor()) );
    //childServiceBroker.addService(MessageTransportServer.class, new MessageTransportServiceProvider(agent));
    //childServiceBroker.addService(UIDService.class, new UIDServiceProvider(clustercontext));
    childServiceBroker.addService(PrototypeRegistryService.class, new PrototypeRegistryServiceProvider());
    childServiceBroker.addService(DomainService.class, new DomainServiceProvider());
    
  }

  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return "agent";
  }
  protected ServiceBroker specifyChildServiceBroker() {
    return new AgentServiceBroker();
  }

  protected Class specifyChildBindingSite() {
    return AgentBindingSite.class;
  }

  protected ContainerAPI getContainerProxy() {
    return this;
  }

  //
  // implement the API needed by agent binders
  //

  /** Makes the child services available to child binders.
   * should use package protection to give access only to AgentBinderSupport,
   * but makes it public for use by Test example.
   **/
  public final ServiceBroker getChildServiceBroker() {
    return childServiceBroker;
  }

  //
  // support classes
  //

  private static class AgentServiceBroker extends ServiceBrokerSupport {}
  
  //need this or something else???
  // support for direct loading of agents
  //
  
  public boolean add(Object o) {
    // this could go away if we turned the message into a ComponentDescription object.
    return true;
  }

  public boolean remove(Object o) {
    return true;
  }
  
  //is this node level or agent level???
  //public ConfigFinder getConfigFinder() {
  //  return agent.getConfigFinder();
  //}


}


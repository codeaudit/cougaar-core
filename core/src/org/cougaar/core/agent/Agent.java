/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.core.agent;

import org.cougaar.core.blackboard.*;

import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.util.*;
import org.cougaar.core.mts.MessageAddress;


/** 
 * This is the basic class required for
 * implementing an Agent.
 **/
public abstract class Agent 
  extends ContainerSupport
  implements ContainerAPI 
{

  private ServiceBroker childServiceBroker;

  public Agent() {
    BinderFactory pmbf = new AgentChildBinderFactory();
    if (!attachBinderFactory(pmbf)) {
      throw new RuntimeException("Failed to load the AgentChildBinderFactory");
    }
  }

   /** this constructor used for backwards compatability mode.  Goes away
   * when we are a contained by a Node component
   **/
  public Agent(ComponentDescription comdesc) {
    BinderFactory pmbf = new AgentBinderFactory();
    if (!attachBinderFactory(pmbf)) {
      throw new RuntimeException("Failed to load the AgentChildBinderFactory");
    }
    //no agent services for now... all are loaded from cluster (specific type of agent)
    System.err.println("Delete me");
  }

  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    childServiceBroker = specifyAgentServiceBroker(bs);
    setChildServiceBroker(childServiceBroker);
  }

  public void unload() {
    if (childServiceBroker != null) {
      destroyAgentServiceBroker(childServiceBroker);
      childServiceBroker = null;
    }

    super.unload();
  }

  protected ServiceBroker specifyAgentServiceBroker(BindingSite bs) {
    return new AgentServiceBroker(bs);
  }

  protected void destroyAgentServiceBroker(ServiceBroker sb) {
    if (sb instanceof AgentServiceBroker) {
      ((AgentServiceBroker) sb).myDestroy();
    }
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
  abstract public MessageAddress getAgentIdentifier();
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
    private void myDestroy() {
      super.destroy();
    }
  }
}




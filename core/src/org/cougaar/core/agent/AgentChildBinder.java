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

package org.cougaar.core.agent;

import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerBinderSupport;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.ConfigFinder;

/** 
 * The standard Binder for PluginManagers and others children of agent.
 **/
public class AgentChildBinder 
extends ContainerBinderSupport 
implements AgentChildBindingSite
{
  /** All subclasses must implement a matching constructor. **/
  public AgentChildBinder(BinderFactory bf, Object child) {
    super(bf, child);
  }

  protected final Agent getAgent() {
    return (Agent)getContainer();
  }
  public MessageAddress getAgentIdentifier() {
    return getAgent().getAgentIdentifier();
  }
  public ConfigFinder getConfigFinder() {
    return getAgent().getConfigFinder();
  }

  public ClusterServesLogicProvider getCluster() { 
    //throw new RuntimeException("Call to getCluster()");
    return ((ClusterServesLogicProvider)getContainer());
  }

  /** Defines a pass-through insulation layer to ensure that the plugin cannot 
   * downcast the BindingSite to the Binder and gain control via introspection
   * and/or knowledge of the Binder class.  This is neccessary when Binders do
   * not have private channels of communication to the Container.
   **/
  protected BindingSite getBinderProxy() {
    // do the right thing later
    return this;
  }

  public String toString() {
    return getComponent()+"/AgentChildBinder";
  }


}

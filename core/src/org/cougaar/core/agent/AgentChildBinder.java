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

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.cluster.*;
import org.cougaar.core.component.*;
import org.cougaar.core.plugin.PluginManager;

/** The standard Binder for PluginManagers and others children of agent.
 **/
public class AgentChildBinder 
extends BinderSupport 
implements AgentChildBindingSite, ContainerBinder
{
  /** All subclasses must implement a matching constructor. **/
  public AgentChildBinder(BinderFactory bf, Object child) {
    super(bf, child);
  }

  public boolean add(Object o) {
    Object c = getComponent();
    if (c instanceof Container) {
      return ((Container)c).add(o);
    } else {
      return false;
    }
  }

  public boolean remove(Object o) {
    Object c = getComponent();
    if (c instanceof Container) {
      return ((Container)c).remove(o);
    } else {
      return false;
    }
  }

  protected final Agent getAgent() {
    return (Agent)getContainer();
  }
  public ClusterIdentifier getAgentIdentifier() {
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

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
import org.cougaar.core.cluster.ClusterServesClusterManagement;
import org.cougaar.core.component.*;
import org.cougaar.core.society.ClusterManagementServesCluster;
import org.cougaar.core.society.Node;
import org.cougaar.core.society.NodeForBinder;
import org.cougaar.core.society.Message;

/** The standard Binder for AgentManagers and possibly others attaching to a Node.
 **/
public class AgentManagerBinder extends BinderSupport 
  implements AgentManagerBindingSite
{
  /** All subclasses must implement a matching constructor. **/
  public AgentManagerBinder(BinderFactory parentInterface, Object child) {
    super(parentInterface, child);
  }

  //child
  protected final AgentManager getAgentManager() {
    return (AgentManager) getComponent();
  }

  //parent
  protected final NodeForBinder getNode() {
    return (NodeForBinder)getContainer();
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
    return "AgentManagerBinder for "+getAgentManager();
  }

  //backwards compatability pass thrus
  public String getIdentifier() {
    return getNode().getIdentifier();
  }
  public String getName() {
    return getNode().getName();
  }
  public void registerCluster(ClusterServesClusterManagement cluster) {
    getNode().registerCluster(cluster);
  }

}

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
import org.cougaar.core.cluster.ClusterServesClusterManagement;
import org.cougaar.core.component.*;
import org.cougaar.core.mts.MessageTransportException;
import org.cougaar.core.mts.MessageTransportService;
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
  public MessageTransportService getMessageTransportServer() {
    return getNode().getMessageTransportServer();
  }
  public void sendMessage(Message message) throws MessageTransportException {
    getNode().sendMessage(message);
  }
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

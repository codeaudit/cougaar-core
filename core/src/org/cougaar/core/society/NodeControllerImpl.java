/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import java.rmi.RemoteException;
import java.util.List;

import org.cougaar.util.PropertyTree;

/**
 * Implementation of <code>NodeController</code>.
 *
 * @see NodeController
 */
public class NodeControllerImpl 
implements NodeController {

  private final Node node;

  public NodeControllerImpl(Node node) {
    this.node = node;
  }

  public String getHostName() throws RemoteException {
    try {
      return node.findHostName();
    } catch (java.net.UnknownHostException e) {
      return null;
    }
  }

  public NodeIdentifier getNodeIdentifier() throws RemoteException {
    return node.getNodeIdentifier();
  }

  public List getClusterIdentifiers() throws RemoteException {
    return node.getClusterIdentifiers();
  }

  public void addClusters(PropertyTree pt) throws RemoteException {
    AddClustersMessage myMessage = new AddClustersMessage();
    myMessage.setOriginator(null);  // from RMI!
    myMessage.setTarget(node.getNodeIdentifier());
    myMessage.setPropertyTree(pt);
    // bypass the message system to send the message directly.
    node.receiveMessage(myMessage);
  }

}

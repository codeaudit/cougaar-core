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
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import org.cougaar.util.PropertyTree;

/**
 * Implementation of <code>ExternalNodeController</code>.
 *
 * @see ExternalNodeController
 */
public class ExternalNodeControllerImpl 
extends UnicastRemoteObject 
implements ExternalNodeController {

  private ExternalNodeActionListener eListener;

  private final Node node;

  public ExternalNodeControllerImpl(Node node) throws RemoteException {
    this.node = node;
  }

  public ExternalNodeActionListener getExternalNodeActionListener() {
    return eListener;
  }

  public void setExternalNodeActionListener(
      ExternalNodeActionListener eListener) {
    this.eListener = eListener;
  }

  public String getHostName() {
    try {
      return node.findHostName();
    } catch (java.net.UnknownHostException e) {
      return null;
    }
  }

  public NodeIdentifier getNodeIdentifier() {
    return node.getNodeIdentifier();
  }

  public List getClusterIdentifiers() {
    return node.getClusterIdentifiers();
  }

}

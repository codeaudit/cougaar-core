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

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.cougaar.core.cluster.ClusterIdentifier;

/**
 * The <code>ExternalNodeActionListener</code> is the external API for a 
 * listener of Node activity within an existing
 * <code>ExternalNodeController</code>.
 * <p>
 * Currently uses RMI, but could be modified to use another protocol
 * (e.g. HTTP server).
 * <p>
 * Could easily replace this with a more Swing-like event listener model.
 */
public interface ExternalNodeActionListener extends Remote {

  /**
   * Notifies the listener that a cluster has been added.
   *
   * @param enc the "source" external-node-controller
   * @param cid the "event-details" of the added Cluster's identifier
   */
  public void handleClusterAdd(
      ExternalNodeController enc,
      ClusterIdentifier cid) throws RemoteException;

  //
  // can add lots of new capabilities here!  
  //
  // could additionally create an "ExternalClusterActionListener" and 
  //   return an "ExternalClusterController" upon cluster creation, thus
  //   creating intra-Node listening...
  //

}

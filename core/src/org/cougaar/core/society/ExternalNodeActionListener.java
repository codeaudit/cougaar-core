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
  void handleClusterAdd(
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

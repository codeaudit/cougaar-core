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

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageTransportException;
import org.cougaar.core.society.MessageTransportService;

/** 
 * Services provided to Clusters by ClusterManagement.
 **/

public interface ClusterManagementServesCluster {
  
  /**
   * Send a Message to another entity on behalf of the (calling) Cluster.
   *
   * @param message Message to send
   * @exception MessageTransportException Raised when message only when message is malformed.
   * Transmission errors are handled by ClusterManagement via other means.
   **/
  void sendMessage(Message message) throws MessageTransportException;

  /**
   * The MessageTransportServer for the Node in which this Cluster resides.
   **/
  MessageTransportService getMessageTransportServer();

  /**
   * The name of this ClusterManager (Node).
   **/
  String getName();
}

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

import org.cougaar.core.mts.MessageTransportException;
import org.cougaar.core.mts.MessageTransportService;

/** 
 * Services provided to Clusters by ClusterManagement.
 **/

public interface ClusterManagementServesCluster {
  

  /**
   * The name of this ClusterManager (Node).
   **/
  String getName();
}

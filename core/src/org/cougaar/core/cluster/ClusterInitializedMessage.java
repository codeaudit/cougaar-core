/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

/** 
 * Private message sent from ClusterManagement to Cluster 
 * indicating that the Cluster Initialization is complete, and
 * that the Cluster is free to go about it's business.
 **/

public class ClusterInitializedMessage extends ClusterMessage
{
  public ClusterInitializedMessage() { super(); }
}

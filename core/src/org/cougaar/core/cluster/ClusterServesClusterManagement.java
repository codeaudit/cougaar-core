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

import org.cougaar.util.StateModelException;
import org.cougaar.core.cluster.ClusterMessage;
import org.cougaar.core.cluster.ClusterIdentifier;

/** 
 * Services provided to ClusterManagement by Cluster.
 * Currently, the only service in addition to ClusterStateModel is
 * message reception capability.
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: ClusterServesClusterManagement.java,v 1.2 2001-04-05 19:26:54 mthome Exp $
 **/

public interface ClusterServesClusterManagement extends ClusterStateModel, ClusterServesMessageTransport
{

  /**
   * Set the Cluster's ClusterIdentifier.
   * This will be called by ClusterManagement exactly once prior to calling
   * initialize (from the ClusterStateModel).  This will be included in a
   * java-beans resource initialization phase at a later time.
   * @param id The new identifier for this cluster.
   **/
  public void setClusterIdentifier(ClusterIdentifier id);
}

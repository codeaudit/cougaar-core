/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.ClusterMessage;
import org.cougaar.domain.planning.ldm.LDMServesClient;

/**
 * @author ALPINE <alpine-software@bbn.com>
 **/
public interface ClusterServesLogicProvider extends LDMServesClient
{
  public ClusterIdentifier getClusterIdentifier();

  /** Send an asynchronous message.
   **/
  public void sendMessage(ClusterMessage message);

  /** @return current scenario time in milliseconds **/
  long currentTimeMillis();
}

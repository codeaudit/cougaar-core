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

import org.cougaar.core.society.Message;

/** The following interface maybe merged in time with ClusterServesClusterManagement.*/

public interface ClusterServesMessageTransport
{
  /** 
   * Primary entry point for Messages delivered by MessageTransport.
   * Answer by metabolizing the message.
   * Expected message types include Query, ClusterManagement, Directives
   * etc.
   **/
  public void receiveMessage(Message aMessage); 
}

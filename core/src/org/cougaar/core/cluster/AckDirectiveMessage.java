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

import org.cougaar.core.cluster.ClusterMessage;
import org.cougaar.core.cluster.DirectiveMessage;
import org.cougaar.core.cluster.persist.NotPersistable;
import org.cougaar.core.cluster.ClusterIdentifier;

/**
 * A AckDirectiveMessage  provides a basic implementation of 
 *  AckDirectiveMessage
 */

public class AckDirectiveMessage extends ClusterMessage
  implements NotPersistable
{
  /** 
   *	no-arg Constructor.
   *  	@return org.cougaar.core.cluster.DirectiveMessage
   */
  public AckDirectiveMessage(ClusterIdentifier theDirectiveSource,
                             ClusterIdentifier theDirectiveDestination,
                             int theSequenceNumber,
                             long anIncarnationNumber)
  {
    super(theDirectiveDestination, theDirectiveSource, anIncarnationNumber);
    setContentsId(theSequenceNumber);
  }

  public String toString() {
    return "<AckDirectiveMessage (" + super.toString() + ")" + ">";
  }
}

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

import org.cougaar.core.society.Message;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.society.MessageAddress;

/**
 * A org.cougaar.core.cluster.ClusterMessage  provides a basic implementation of 
 *  ClusterMessage
 */

public class AdvanceClockMessage extends Message
{
  private ExecutionTimer.Parameters theParameters;

  /**
   * Advance the society's clock to a fixed (stopped) time.
   **/
  public AdvanceClockMessage(MessageAddress s, ExecutionTimer.Parameters parameters) {
    super(s, MessageAddress.SOCIETY);
    theParameters = parameters;
  }

  public ExecutionTimer.Parameters getParameters() {
    return theParameters;
  }

  public String toString() {
    return "<AdvanceClockMessage "
      + getOriginator()
      + " "
      + theParameters
      + ">";
  }
}

/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.blackboard;

import org.cougaar.core.agent.ClusterMessage;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;

/**
 * A AckDirectiveMessage  provides a basic implementation of 
 *  AckDirectiveMessage
 */

class AckDirectiveMessage extends ClusterMessage
  implements NotPersistable
{
  /** 
   */
  public AckDirectiveMessage(MessageAddress theDirectiveSource,
                             MessageAddress theDirectiveDestination,
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

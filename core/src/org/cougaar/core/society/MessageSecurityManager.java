
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

import org.cougaar.core.mts.MessageTransport;

public interface MessageSecurityManager {
  /** Returns a message which has been cryptologically secured.
   * Implementations may destructively modify the original message
   * in order to actually send the original message.
   * @returns a SecureMessage marked Message
   **/
  Message secureMessage(Message m);

  /** reverse the transform that secureMessage does, using whatever
   * techniques are needed to verify the integrity and source of the 
   * message.
   * @return the unsecured message iff validated.  Will return null if
   * validation fails.
   **/
  Message unsecureMessage(SecureMessage m);

  /** set the MessageTransport instance to be used 
   * if the MessageSecurityManager needs to negotiate connections.
   * It will be called exactly once immediately after instantiation
   * of the MSM instance.
   **/
   void setMessageTransport(MessageTransport mts);

}

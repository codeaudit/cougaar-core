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

public interface EnvelopeReceiver 
{
  /** Receive (and eventually process) an Envelope of collection
   * updates.
   * @param subscription The target subscription.
   * @param envelope An atomic set of collection updates.
   **/
  void receiveEnvelope(Subscription subscription, Envelope envelope);
}

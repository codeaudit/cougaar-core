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

/** An Exception related to Subscriber functionality.
 **/

public class SubscriberException extends RuntimeException {
  public SubscriberException() { super(); }
  public SubscriberException(String s) { super(s); }
}

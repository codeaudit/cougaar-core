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

/** Exception thrown when there is a problem detected by
 * subscription mechanisms.
 **/

public class SubscriptionException extends RuntimeException {
  public SubscriptionException(String s) { super(s); }
  public SubscriptionException() { super(); }
}

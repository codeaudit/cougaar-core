/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.util;

/** 
 * DynamicUnaryPredicate marks a UnaryPredicate as testing
 * for mutable features of objects.  Implementing this
 * as opposed to UnaryPredicate is a hint to the COUGAAR subscription
 * mechanism that it needs to do more expensive processing to
 * detect changes in subscription membership.
 */

public interface DynamicUnaryPredicate extends UnaryPredicate {
}

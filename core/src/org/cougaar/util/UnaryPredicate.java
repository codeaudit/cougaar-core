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
 * UnaryPredicate is an interface for implementing Unary testing operations.
 * It is used for testing membership in sets of various sorts.
 *
 * The choice of symbol names is intended to provide signature-level compatability
 * with Objectspace's JGL package.
 */

public interface UnaryPredicate extends java.io.Serializable {
  /** @return true iff the object "passes" the predicate */
  boolean execute(Object o);
}

/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

/** 
 * A Mapping is an interface for transforming an element of a set
 * into another value.
 * @see Mappings
 */

package org.cougaar.util;

public interface Mapping {
  /** @return the result of applying the mapping to the argument object */
  Object map(Object o);
}

/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.asset;

/** marker class for indicating that a PG is locked.
 * Implemented by all locked (inner) classes.
 **/
public interface LockedPG {
  /** returns the Class to introspect on to get the accessors for BeanInfo. **/
  Class getIntrospectionClass();
}

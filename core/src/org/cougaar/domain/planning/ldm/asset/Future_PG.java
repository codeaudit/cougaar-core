/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.asset;

/** A Future_PG is an instance which represents a promise of information
 * at some point in the future.  Any use of the accessors of a Future_PG
 * will block until the properties are actually available.
 * An LDM plugin may initially supply a Future_PG of the appropriate class
 * rather than a real PG in order to back-fill later.
 **/
public interface Future_PG 
  extends LockedPG
{
  /** Indicate to anyone blocked on the accessors of the Future
   * that the real values are available.
   **/
  void finalize(PropertyGroup realPG);
}

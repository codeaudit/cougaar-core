/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.asset;

/** marker class for indicating that a PG is unlikely to ever be filled.
 * The "null" version of a PG is available only via the static data member
 * WhateverPG.nullPG, where WhateverPG is the base PG interface.
 * If an Asset has an actual null as the value for a given PG, it is
 * interpreted as <em> unspecified </em>, and potentially available for
 * late-binding.  A value of a Null_PG instance prevents late-binding,
 * activation.
 * All accessors will throw an UndefinedValueException if called.
 **/
public interface Null_PG 
  extends LockedPG
{
}

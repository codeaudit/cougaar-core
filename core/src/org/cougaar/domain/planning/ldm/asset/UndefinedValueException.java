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

/** A RuntimeException thrown by Properties when the value is
 * undefined or unknowable.
 * The most common causes are calling a Parameterized property 
 * accessor when no handler has been installed or somehow
 * calling any accessor of a Null_PG.
 **/
public class UndefinedValueException extends RuntimeException {
  public UndefinedValueException() {}
  public UndefinedValueException(String s) { super(s); }
}

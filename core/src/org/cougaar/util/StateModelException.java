/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

/** 
 * Base (java) exception for StateModel-specific exception classes.
 **/

public class StateModelException extends RuntimeException {
  public StateModelException() { super(); }
  public StateModelException(String s) { super(s); }
}


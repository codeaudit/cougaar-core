/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.domain.planning.ldm;

/**
 * A runtime exception thrown by Factory.
 **/

public class FactoryException extends RuntimeException {
  private Exception inner = null;

  public FactoryException(String s) {
    super(s);
  }

  public FactoryException(String s, Exception e) {
    super(s);
    inner = e;
  }

  public FactoryException(Exception e) {
    super("FactoryException("+e+")");
    inner = e;
  }
  
  public Exception getException() { return inner; }
}

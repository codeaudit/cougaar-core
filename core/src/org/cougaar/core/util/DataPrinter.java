/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.util;

import java.io.ObjectOutputStream;

/** 
 * Override all of AbstractPrinter for ObjectOutputStream! <br>
 * Even though this class is a subclass of PrintStream, this class
 * won't use that superclass for Object printing!
 * <p>
 * @see AbstractPrinter
 **/
public class DataPrinter extends AbstractPrinter {

  public static void main(String[] args) {
    testMain("data");
  }

  protected ObjectOutputStream objOut;
  public DataPrinter(java.io.OutputStream out) {
    super(makeObjOut(out));
    objOut = (ObjectOutputStream)super.out;
  }

  protected static ObjectOutputStream makeObjOut(java.io.OutputStream out) {
    try {
      return new ObjectOutputStream(out);
    } catch (Exception e) {
      throw new RuntimeException("ObjectOutputStream failed????");
    }
  }

  /** <code>o</code> shouldn't be null! **/
  public void printObject(Object o) {
    try {
      objOut.writeObject(o);
      objOut.flush();
    } catch (Exception e) {
      throw new RuntimeException(
        "ObjectOutputStream writeObject("+o+") failed??");
    }
  }

}

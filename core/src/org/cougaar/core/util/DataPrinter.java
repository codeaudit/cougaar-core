/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
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

/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.adaptivity;

/**
 * Holds a range specification for an operating mode or condition
 * having a single value. That is, the range has exactly one value.
 **/
public class OMCPoint extends OMCRange {
  /**
   * Constructor from an int
   **/
  public OMCPoint(int v) {
    this(new Integer(v));
  }

  /**
   * Constructor from an double
   **/
  public OMCPoint(double v) {
    this(new Double(v));
  }

  /**
   * Constructor from an arbitray Comparable
   **/
  public OMCPoint(Comparable v) {
    super(v, v);
  }

  /**
   * Return a String representation of the single value in the range.
   **/
  public String toString() {
    return min.toString();
  }
}

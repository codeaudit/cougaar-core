/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;


/**
 * Holds a range specification for an operating mode or condition
 * value expressed as an half-open interval (min is included, but max
 * is not).
 **/
public class OMCToRange extends OMCRange {
  private Comparable undecrementedMax;

  /**
   * Constructor from ints
   **/
  public OMCToRange(int min, int max) {
    this(new Integer(min), new Integer(max));
  }

  /**
   * Constructor from doubles
   **/
  public OMCToRange(double min, double max) {
    this(new Double(min), new Double(max));
  }

  /**
   * Constructor from Comparables
   **/
  public OMCToRange(Comparable min, Comparable max) {
    super(min, ComparableHelper.decrement(max));
    undecrementedMax = max;
  }

  public String toString() {
    return min.toString() + " to " + undecrementedMax.toString();
  }
}

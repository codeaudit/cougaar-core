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
 * value. Ranges are half-open intervals. The value of max _must_
 * exceed low.
 **/
public class OMCRange {
  protected Comparable min, max;

  /**
   * Constructor for int values
   **/
  protected OMCRange(int min, int max) {
    this(new Integer(min), new Integer(max));
  }

  /**
   * Constructor for double values
   **/
  protected OMCRange(double min, double max) {
    this(new Double(min), new Double(max));
  }

  /**
   * Constructor for Comparable values
   **/
  protected OMCRange(Comparable min, Comparable max) {
    if (min.getClass() != max.getClass()) {
      throw new IllegalArgumentException("Min and max have different classes");
    }
    if (min.compareTo(max) > 0) {
      throw new IllegalArgumentException("Min must not exceed max");
    }
    this.min = min;
    this.max = max;
  }

  /**
   * Test if a value is in this range.
   * @return true if the value is in the (closed) interval between min
   * and max.
   * @param v The value to compare.
   **/
  public boolean contains(Comparable v) {
    return min.compareTo(v) <= 0 && max.compareTo(v) >= 0;
  }

  /**
   * Gets the minimum value in the range.
   **/
  public Comparable getMin() {
    return min;
  }

  /**
   * Gets the maximum value in the range.
   **/
  public Comparable getMax() {
    return max;
  }
}

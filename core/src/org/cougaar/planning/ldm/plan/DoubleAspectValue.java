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

package org.cougaar.planning.ldm.plan;

/** An AspectValue implementation which stores a double.
 */
 
public class DoubleAspectValue extends TypedAspectValue {
  protected double value;

  protected DoubleAspectValue(int type, double value) {
    super(type);
    if (Double.isNaN(value) || Double.isInfinite(value))
      throw new IllegalArgumentException("The value of a DoubleAspectValue must be a finite, non-NaN");
    this.value = value;
  }

  public static AspectValue create(int type, Object o) {
    double value;
    if (o instanceof Number) {
      value = ((Number)o).doubleValue();
    } else if (o instanceof AspectValue) {
      value = ((AspectValue)o).doubleValue();
    } else {
      throw new IllegalArgumentException("Cannot construct a DoubleAspectValue from "+o);
    }
    return new DoubleAspectValue(type,value);
  }

  public final double doubleValue() {
    return value;
  }
  public final long longValue() {
    return Math.round(value);
  }
  public final float floatValue() {
    return (float) value;
  }
  public final int intValue() {
    return (int) Math.round(value);
  }


  public boolean equals(Object v) {
    if (v instanceof DoubleAspectValue) {
      return (getType() == ((AspectValue)v).getType() &&
              doubleValue() == ((AspectValue)v).doubleValue());
    } else {
      return false;
    }
  }

  public int hashCode() {
    return getType()+((int)(doubleValue()*128));
  }

  public String toString() {
    return Double.toString(doubleValue())+"["+getType()+"]";
  }

}



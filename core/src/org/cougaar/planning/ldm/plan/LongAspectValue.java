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

/** An AspectValue implementation which stores a long.
 */
 
public class LongAspectValue extends TypedAspectValue {
  private long value;

  protected LongAspectValue(int type, long value) {
    super(type);
    this.value = value;
  }

  public static AspectValue create(int type, Object o) {
    long value;
    if (o instanceof Number) {
      value = ((Number)o).longValue();
    } else if (o instanceof AspectValue) {
      value = ((AspectValue)o).longValue();
    } else {
      throw new IllegalArgumentException("Cannot construct a LongAspectValue from "+o);
    }
    return new LongAspectValue(type,value);
  }

  public final double doubleValue() {
    return (double) value;
  }
  public final long longValue() {
    return value;
  }
  public final float floatValue() {
    return (float) value;
  }
  public final int intValue() {
    return (int) value;
  }

  public boolean equals(Object v) {
    if (v instanceof LongAspectValue) {
      return (getType() == ((AspectValue)v).getType() &&
              longValue() == ((AspectValue)v).longValue());
    } else {
      return false;
    }
  }

  public int hashCode() {
    return getType()+((int)(longValue()*128));
  }

  public String toString() {
    return Long.toString(longValue())+"["+getType()+"]";
  }

}



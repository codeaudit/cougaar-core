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

import java.io.Serializable;
import org.cougaar.util.MoreMath;

/** AspectValue is the essential "value" abstraction with respect to
 * evaluation of the goodness or correctness of a particular solution.
 * @see AllocationResult
 */
 
public class AspectValue implements AspectType, Serializable, Cloneable {
  protected int type;
  protected double value;

  public AspectValue(int type, double value) {
    if (Double.isNaN(value))
      throw new IllegalArgumentException("The value of an AspectValue may not be NaN");
    this.type = type;
    this.value = value;
  }

  public String toString() {
    return ""+value+"["+type+"]";
  }

  public Object clone() {
    return new AspectValue(type, value);
  }

  /**
   * Clone an array of AspectValue. Clones the elements as well as the
   * array.
   * @param avs an array of AspectValue
   * @return a copy of the array with copies of array element values.
   **/
  public static AspectValue[] clone(AspectValue[] avs) {
    AspectValue[] result = new AspectValue[avs.length];
    for (int i = 0; i < avs.length; i++) {
      result[i] = (AspectValue) avs[i].clone();
    }
    return result;
  }

  /**
   * Compare two arrays of AspectValues. Since the values are not
   * necessarily in the same order, we first check assuming they are
   * in the same order. If that fails because they are not in the same
   * order, we try again reordering the values as needed. Arrays
   * having repeated AspectTypes produce unspecified results. Such
   * arrays are intrinsically ambiguous.
   **/
  public static boolean equals(AspectValue[] avs1, AspectValue[] avs2) {
    int len = avs1.length;
    if (len != avs2.length) return false; // Can't be equal if different length
  outer:
    for (int i = 0; i < len; i++) {
      AspectValue av1 = avs1[i];
      int type1 = av1.getAspectType();
    inner:
      for (int j = 0; j < len; j++) {
        int k = (i + j) % len;
        AspectValue av2 = avs2[k];
        int type2 = av2.getAspectType();
        if (type1 == type2) {
          if (av1.equals(av2)) continue outer;
          break inner;
        }
      }
      return false;             // Found no match
    }
    return true;                // Found a match for every aspect
  }

  public static boolean nearlyEquals(AspectValue[] avs1, AspectValue[] avs2) {
    int len = avs1.length;
    if (len != avs2.length) return false; // Can't be equal if different length
  outer:
    for (int i = 0; i < len; i++) {
      AspectValue av1 = avs1[i];
      int type1 = av1.getAspectType();
    inner:
      for (int j = 0; j < len; j++) {
        int k = (i + j) % len;
        AspectValue av2 = avs2[k];
        int type2 = av2.getAspectType();
        if (type1 == type2) {
          if (av1.nearlyEquals(av2)) continue outer;
          break inner;
        }
      }
      return false;             // Found no match
    }
    return true;                // Found a match for every aspect
  }

  /** @return int The Aspect Type.
   * @see org.cougaar.planning.ldm.plan.AspectType
   */
  public int getAspectType() { return type;}
  
  /** Reset the value after creation.  Useful for AllocationResultAggregators
    * that sum AspectValues.
    * @param newvalue
    */
  public void setValue(double newvalue) {
    this.value = newvalue;
  }
   
  /** @return double The value of the aspect.
    */
  public double getValue() { return value;}
   
  public long longValue() {
    return Math.round(value);
  }

  public boolean nearlyEquals(Object o) {
    if (o instanceof AspectValue) {
      AspectValue that = (AspectValue) o;
      if (this.getAspectType() == that.getAspectType()) {
        return MoreMath.nearlyEquals(this.getValue(), that.getValue());
      }
    }
    return false;
  }

  public boolean equals(AspectValue v) {
    return (v.value == value &&
            v.type == type);
  }
  
  public boolean equals(Object v) {
    if (v instanceof AspectValue) {
      return (value == ((AspectValue)v).value &&
              type == ((AspectValue)v).type);
    } else {
      return false;
    }
  }

  public boolean isLessThan(AspectValue v) {
    return (value < v.value);
  }
  public boolean isGreaterThan(AspectValue v) {
    return (value > v.value);
  }

  public double minus(AspectValue v) {
    return value - v.value;
  }

  public boolean isBetween(AspectValue low, AspectValue hi) {
    return (! ( isLessThan(low) ||
                isGreaterThan(hi) ));
  } 

  public int hashCode() {
    return type+((int)value*10);
  }

  /**
   * This should be in AspectType, but that's an interface and can't
   * have methods. This is the closest place that makes any sense and
   * avoids creating a new class just to convert aspect types into
   * strings.
   **/
  public static String aspectTypeToString(int aspectType) {
    if (aspectType >=0 && aspectType < AspectType.ASPECT_STRINGS.length) {
      return AspectType.ASPECT_STRINGS[aspectType];
    } else {
      return String.valueOf(aspectType);
    }
  }
}

/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import java.io.Serializable;
import org.cougaar.util.MoreMath;

/*
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AspectValue.java,v 1.3 2001-05-15 15:23:38 tomlinso Exp $
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
   * @see org.cougaar.domain.planning.ldm.plan.AspectType
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

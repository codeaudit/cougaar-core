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

/*
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AspectValue.java,v 1.2 2001-04-05 19:27:13 mthome Exp $
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

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
import org.cougaar.planning.ldm.measure.CostRate;
import org.cougaar.planning.ldm.measure.CountRate;
import org.cougaar.planning.ldm.measure.FlowRate;
import org.cougaar.planning.ldm.measure.MassTransferRate;
import org.cougaar.planning.ldm.measure.Rate;
import org.cougaar.planning.ldm.measure.Speed;
import org.cougaar.planning.ldm.measure.TimeRate;

/**
 * An AspectValue represented by a rate measure
 */
 
public class AspectRate extends TypedAspectValue {
  private Rate rate_value;

  protected AspectRate(int type, Rate new_rate_value) {
    super(type);
    rate_value = new_rate_value;
  }

  public static AspectValue create(int type, Object o) {
    long l;
    if (o instanceof Rate) {
      return new AspectRate(type, (Rate) o);
    } else {
      throw new IllegalArgumentException("Cannot create an AspectRate from "+o);
    }
  }
   
  public static AspectValue create(int type, Rate r) {
    return new AspectRate(type,r);
  }

  /** Non-preferred alias for #create(int, Rate).
   **/
  public static final AspectValue newAspectRate(int type, Rate r) {
    return new AspectRate(type, r);
  }
   
  /** The rate value of the aspect.
   * @note the preferred accessor is #rateValue()
   */
  public final Rate getRateValue() {
    return rate_value;
  }

  public final Rate rateValue() {
    return rate_value;
  }

  /** return the common-unit value of the rate.
   * @note A better solution is to use rateValue and specify the unit of measure.
   **/
  public final double doubleValue() {
    return (double) rateValue().getValue(rateValue().getCommonUnit());
  }
  /** return the common-unit value of the rate.
   * @note A better solution is to use rateValue and specify the unit of measure.
   **/
  public final long longValue() {
    return (long) doubleValue();
  }
  /** return the common-unit value of the rate.
   * @note A better solution is to use rateValue and specify the unit of measure.
   **/
  public final float floatValue() {
    return (float) doubleValue();
  }
  /** return the common-unit value of the rate.
   * @note A better solution is to use rateValue and specify the unit of measure.
   **/
  public final int intValue() {
    return (int) doubleValue();
  }

   
  public boolean equals(Object v) {
    if (!(v instanceof AspectRate)) {
      return false;
    } 
    AspectRate rate_v = (AspectRate) v;

    return (rate_v.getAspectType() == getAspectType() &&
            rate_v.rateValue().equals(rateValue()));
  }

  public String toString() {
    return rate_value.toString()+"["+getType()+"]";
  }
}

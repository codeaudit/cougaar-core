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

/*
 * @author  ALPINE <alpine-software@bbn.com>
 *
 */
 
public class AspectRate extends AspectValue {
  protected Rate rate_value;

  public AspectRate(int type, Rate new_rate_value) {
    super(type, 0.0);
    setValue(new_rate_value);
  }

  public Object clone() {
    return new AspectRate(type, rate_value);
  }

  /** Reset the value after creation.  Useful for AllocationResultAggregators
    * that sum AspectValues.
    * @param newvalue
    */
  public void setValue(Rate newvalue) {
    this.rate_value = newvalue;
    super.setValue(getValue());
  }

  /**
   * Change the value of the Rate of this AspectRate. Construct a new
   * Rate based on the actual class of rate_value. This should cover
   * all the implementations of Rate in the measure package.
   **/
  public void setValue(double newValue) {
    Class rateClass = rate_value.getClass();
    int units = rate_value.getCommonUnit();
    if (rateClass == CountRate.class) {
      setValue(new CountRate(newValue, units));
    } else if (rateClass == FlowRate.class) {
      setValue(new FlowRate(newValue, units));
    } else if (rateClass == CostRate.class) {
      setValue(new CostRate(newValue, units));
    } else if (rateClass == MassTransferRate.class) {
      setValue(new MassTransferRate(newValue, units));
    } else if (rateClass == Speed.class) {
      setValue(new Speed(newValue, units));
    } else if (rateClass == TimeRate.class) {
      setValue(new TimeRate(newValue, units));
    } else {
      throw new IllegalArgumentException("Unknown rate class: " + rateClass);
    }
  }
   
  /** 
    * @return Rate The value of the aspect.
    */
  public Rate getRateValue() {
    return rate_value;
  }

  /** 
    * @return double
    */
  public double getValue() {
    return rate_value.getValue(rate_value.getCommonUnit());
  }
   
/* Accessors for longitude + latitude?
   public long longValue() {
    return Math.round(value);
  }
*/

  public boolean equals(AspectValue v) {
    if (!(v instanceof AspectRate)) {
      return false;
    } 
    AspectRate rate_v = (AspectRate) v;

    return (rate_v.getAspectType() == getAspectType()
            && rate_v.getRateValue().equals(getRateValue()));
  }

  public String toString() {
    return "Rate-" + super.toString();
  }
}

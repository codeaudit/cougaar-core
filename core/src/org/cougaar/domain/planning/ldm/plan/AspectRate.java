/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import java.io.Serializable;
import org.cougaar.domain.planning.ldm.measure.CostRate;
import org.cougaar.domain.planning.ldm.measure.CountRate;
import org.cougaar.domain.planning.ldm.measure.FlowRate;
import org.cougaar.domain.planning.ldm.measure.MassTransferRate;
import org.cougaar.domain.planning.ldm.measure.Rate;
import org.cougaar.domain.planning.ldm.measure.Speed;
import org.cougaar.domain.planning.ldm.measure.TimeRate;

/*
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AspectRate.java,v 1.2 2001-04-03 14:00:47 tomlinso Exp $
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

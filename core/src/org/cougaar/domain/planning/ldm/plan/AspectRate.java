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
import org.cougaar.domain.planning.ldm.measure.Rate;

/*
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AspectRate.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
 */
 
public class AspectRate extends AspectValue {
  protected int type;
  protected Rate rate_value;

  public AspectRate(int type, Rate new_rate_value) {
    super(type, 0.0);

    this.rate_value = new_rate_value;
  }

  /** Reset the value after creation.  Useful for AllocationResultAggregators
    * that sum AspectValues.
    * @param newvalue
    */
  public void setValue(Rate newvalue) {
    this.rate_value = newvalue;
  }
   
  /** 
    * @return Rate The value of the aspect.
    */
  public Rate getRateValue() { return rate_value;}

  /** 
    * @return double
    */
  public double getValue() { return rate_value.getValue(rate_value.getCommonUnit()); }
   
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
 
}

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

/*
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AspectLocation.java,v 1.3 2001-04-05 00:48:32 gvidaver Exp $
 */
 
public class AspectLocation extends AspectValue {
  protected Location loc_value;

  public AspectLocation(int type, Location new_loc_value) {
    super(type, 0.0);

    this.loc_value = new_loc_value;
  }

  public Object clone() {
    return new AspectLocation(type, loc_value);
  }

  /** Reset the value after creation.  Useful for AllocationResultAggregators
    * that sum AspectValues.
    * @param newvalue
    */
  public void setValue(Location newvalue) {
    this.loc_value = newvalue;
  }
   
  /** 
    * @return Location The value of the aspect.
    */
  public Location getLocationValue() { return loc_value;}

  /** 
    * @return Meaningless return
    * The super class was initted with NaN, so it will return NaN
    */
 //   public double getValue() { return 0.0d;}
   
/* Accessors for longitude + latitude?
   public long longValue() {
    return Math.round(value);
  }
*/

  public boolean equals(AspectValue v) {
    if (!(v instanceof AspectLocation)) {
      return false;
    } 
    AspectLocation loc_v = (AspectLocation)v;

    return (loc_v.getLocationValue() == loc_value &&
            loc_v.getAspectType() == type);
  }
 
}

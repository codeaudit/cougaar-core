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

package org.cougaar.domain.planning.ldm.plan;

import java.io.Serializable;

/*
 * @author  ALPINE <alpine-software@bbn.com>
 *
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

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

/**
 * An AspectValue with a location instead of a value.
 *
 */
 
public class AspectLocation extends TypedAspectValue {
  private Location loc_value;

  protected AspectLocation(int type, Location new_loc_value) {
    super(type);
    this.loc_value = new_loc_value;
  }

  public static AspectValue create(int type, Object o) {
    if (o instanceof Location) {
      return new AspectLocation(type, (Location) o);
    } else {
      throw new IllegalArgumentException("Cannot construct an AspectLocation from "+o);
    }
  }

  public final double doubleValue() {
    throw new IllegalArgumentException("AspectLocations do not have numeric values");
  }
  public final long longValue() {
    throw new IllegalArgumentException("AspectLocations do not have numeric values");
  }
  public final float floatValue() {
    throw new IllegalArgumentException("AspectLocations do not have numeric values");
  }
  public final int intValue() {
    throw new IllegalArgumentException("AspectLocations do not have numeric values");
  }

  /** The location associated with the AspectValue.
   * @note locationValue is the preferred method.
    */
  public final Location getLocationValue() { return loc_value;}

  /** The location associated with the AspectValue. */
  public final Location locationValue() { return loc_value;}

  public int hashCode() {
    return getType()+loc_value.hashCode();
  }

  public String toString() {
    return Float.toString(floatValue())+"["+getType()+"]";
  }

  public boolean equals(AspectValue v) {
    if (v instanceof AspectLocation) {
      AspectLocation loc_v = (AspectLocation)v;
      return (loc_v.getAspectType() == getType() &&
              loc_v.getLocationValue() == getLocationValue());
    } else {
      return false;
    }
  }
}

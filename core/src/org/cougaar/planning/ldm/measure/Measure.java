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
package org.cougaar.planning.ldm.measure;

import java.io.Serializable;

/** Base interface for all Measure classes.
 *
 * All concrete subclasses of AbstractMeasure are required to 
 * implement the method:
 *   public static AbstractMeasure newMeasure(String s, int unit);
 * Other AbstractMeasure-level constructors may be defined, depending
 * on each the primative-types that each concrete class can handle.
 * For instance, Distance is based on a double value, so in addition to
 * the above, Distance.newMeasure(double d, int unit) will be defined.
 *
 * The allowed values of the unit specifier depends on (and is defined
 * in) each Measure type.  Example: Distance.FEET is a static final int
 * with the correct value for use in constructing Distances in Feet.
 * 
 * Each Measure class will also define a getValue(int unit) method 
 * returning whatever primitive type it is based on (usually double).
 *
 * All Measure classes are equals() comparable (exact internal value 
 * equality), define toString() (the value in a standard unit and a unit 
 * abbreviation, e.g. "4.5m"), and define hashCode() (for completeness).
 *
 * @see AbstractMeasure for base implementation class of all measures.
 **/

public interface Measure extends Serializable {
  /** @return a commonly-used unit used by this measure.  NOTE that this
   * is only a convenience and should not be depended on for computational
   * use, since the notion of a common unit is extremely dependent on
   * context.
   **/
  int getCommonUnit();

  /** @return the value of the highest-valued unit known by this measure.
   * There is no implied relationship between "highest valued" unit and "size" 
   * of that unit.
   **/
  int getMaxUnit();

  /** @return the name of the unit specified.
   *  The result is undefined if the unit is not valid for this measure class.
   **/
  String getUnitName(int unit);

  /** @return the value of the measure in terms of the specified units.
   *  @param unit must be in the range from 0 to getMaxUnit.
   **/
  double getValue(int unit);
}

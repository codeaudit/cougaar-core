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

/** Interface for all Derivative or "Rate" Measures.
 **/

public interface Derivative extends Measure {
  /** @return the numerator class of the derivative measure (dx of dx/dy) **/
  Class getNumeratorClass();

  /** @return the denominator class of the derivative measure (dy of dx/dy) **/
  Class getDenominatorClass();

  /** The value of the canonical instance will have no relationship to 
   * the value of the Derivative Measure, but is to be used for introspection
   * purposes.
   * @return a canonical instance of the numerator class.
   **/
  Measure getCanonicalNumerator();

  /** The value of the canonical instance will have no relationship to 
   * the value of the Derivative Measure, but is to be used for introspection
   * purposes.
   * @return a canonical instance of the denominator class.
   **/
  Measure getCanonicalDenominator();

  /** Get the value of the derivative measure by specifying both numerator and
   * denominator units.
   **/
  double getValue(int numerator_unit, int denominator_unit);
  
  /** integrate the denominator, resulting in a non-derivative numerator.
   * For example, computes a Distance given a Speed and a Duration.
   * @return a newly created Numerator measure.
   **/
  Measure computeNumerator(Measure denominator);

  /** integrate the numerator, resulting in a non-derivative denominator.
   * For example, compute a Duration given a Speed and a Distance.
   * @return a newly created Denominator measure.
   **/
  Measure computeDenominator(Measure numerator);
}

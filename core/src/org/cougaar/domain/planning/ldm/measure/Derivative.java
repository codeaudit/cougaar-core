/*
 * <copyright>
 *  Copyright 2000-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.domain.planning.ldm.measure;

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

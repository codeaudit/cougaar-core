/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
/** New interface as part of new Constraint API
 * containing setValue method to adjust aspect value
 * of constrained task
 **/

package org.cougaar.domain.planning.ldm.plan;

public interface SettableConstraintEvent
	extends ConstraintEvent
{
  /**
   * Sets (preferences for) the aspect value needed to satisfy the
   * constraint placed on the task of this event.
   * @param value the constraining value
   * @param constraintOrder specifies whether the constrained value
   * must be BEFORE (LESSTHAN), COINCIDENT (EQUALTO), or AFTER
   * (GREATERTHAN) the constraining value. The score function of the
   * preference is selected to achieve the constraint.
   * @param slope specifies the rate at which the score function
   * degrades on the allowed side of the constraint. The disallowed
   * side always has a failing score.
   **/
  void setValue(double value, int constraintOrder, double slope);
  /**
   * Sets (preferences for) the aspect value needed to satisfy the
   * constraint placed on the task of this event.
   * @param value the constraining value
   **/
  void setValue(double value);
}

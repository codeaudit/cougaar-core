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

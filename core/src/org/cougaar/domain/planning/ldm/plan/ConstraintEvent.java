/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
/* ConstraintEvent objects can be used to denote
 * either constraining or constrained events
 */

package org.cougaar.domain.planning.ldm.plan;

public interface ConstraintEvent
{
  /**
   * This value is used to denote an unknown aspect value in all
   * constraint events.
   **/
  public static final double NOVALUE = Double.NaN;

  /* getValue returns the allocation result of the
   * aspect when the task is constraining or
   * the preferred value of the aspect when the
   * task is constrained. isConstraining is true
   * when task is constraining, false when task is
   * constrained.
   * @return the value of this ConstrainEvent. NOVALUE is returned if
   * the value is not known. For example, the value for a constrained
   * task that has not yet been disposed will be NOVALUE.
   */
  double getValue();

  /* getResultValue returns the allocation result of the
   * aspect without regard to whether the event isConstraining()
   * @return the value of this ConstrainEvent. NOVALUE is returned if
   * the value is not known. For example, the value for a constrained
   * task that has not yet been disposed will be NOVALUE.
   */
  double getResultValue();

  /**
   * The aspect involved in this end of the constraint.
   * @return the aspect type of the preference or allocation result.
   **/
  int getAspectType();

  /**
   * Return the task, if any. AbsoluteConstraintEvents have no task.
   * @return the task. null is returned for absolute constraints.
   **/
  Task getTask();

  /**
   * Tests if this is a constraining (vs. constrained) event.
   **/
  boolean isConstraining();
}

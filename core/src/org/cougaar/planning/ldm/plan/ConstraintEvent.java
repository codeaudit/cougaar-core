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
/* ConstraintEvent objects can be used to denote
 * either constraining or constrained events
 */

package org.cougaar.planning.ldm.plan;

public interface ConstraintEvent
{
  /**
   * This value is used to denote an unknown aspect value in all
   * constraint events.
   **/
  double NOVALUE = Double.NaN;

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

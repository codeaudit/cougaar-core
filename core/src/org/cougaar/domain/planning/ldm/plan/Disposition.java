/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

/**
 * A PlanElement which represents an end-state disposition of
 * a task.  That is, the task is either failed or is 
 * complete by some measure: e.g. "Do your transport tasks" is 
 * successfully complete if you have no transport tasks.
 **/

public interface Disposition extends PlanElement {
  /** @return true IFF disposition represents a success **/
  boolean isSuccess();
}

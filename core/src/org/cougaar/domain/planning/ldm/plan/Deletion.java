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

import org.cougaar.core.society.UID;

import java.util.Enumeration;

/** Deletion Interface
 * Deletion is a message that a task sent to another cluster has been deleted.
 * The Deletion will task and parent task uids
 **/

public interface Deletion extends Directive {

  /**
   * Returns the task the deletion is in reference to.
   * @return Task
   **/
  UID getTaskUID();
}

/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.core.society.UID;

/** TaskRescind Interface
 * TaskRescind allows a task to be rescinded from the Plan. 
 **/

public interface TaskRescind extends Directive {
  
  /**
   * @return the UID of the task to be rescinded.
   **/
  UID getTaskUID();
  
}

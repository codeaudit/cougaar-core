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

/** NewTaskRescind Interface
 * Provides setter methods for object creation 
 **/

public interface NewTaskRescind extends TaskRescind, NewDirective 
{
  /**
   * Sets the UID of the task to be rescinded
   * @param atask - The Task to be rescinded.
   **/
  void setTaskUID(UID uid);
}

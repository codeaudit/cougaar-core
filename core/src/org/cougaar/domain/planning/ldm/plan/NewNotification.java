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

import java.util.Enumeration;

/** NewNotification Interface
 * provides setter methods to create a Notification object
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: NewNotification.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
 **/

public interface NewNotification extends Notification, NewDirective {
		
  /** 
   * Sets the task the notification is in reference to.
   * @param uid The UID of the Task to be referenced in the Notification. 
   **/
  void setTaskUID(UID uid);
		
  /** Sets the combined estiamted allocationresult from below
   * @param ar - The AllocationResult for the Task.
   **/
  void setAllocationResult(AllocationResult ar);
    
  /** Sets the child task's UID that was disposed.  It's parent task is getTask();
   * Useful for keeping track of which subtask of an Expansion caused
   * the re-aggregation of the Expansion's reported allocationresult.
   * @param thechildUID
   */
  void setChildTaskUID(UID thechildUID);
		
}

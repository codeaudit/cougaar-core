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

package org.cougaar.planning.ldm.plan;

import org.cougaar.core.util.UID;

import java.util.Enumeration;

/** NewNotification Interface
 * provides setter methods to create a Notification object
 * @author  ALPINE <alpine-software@bbn.com>
 *
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

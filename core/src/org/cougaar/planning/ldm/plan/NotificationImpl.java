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

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.NewNotification;
import org.cougaar.planning.ldm.plan.Notification;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.core.util.UID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/** An implementation of org.cougaar.Notification
 */
public class NotificationImpl extends PlanningDirectiveImpl
  implements Notification, NewNotification
{
                
  private UID taskUID; 
  private AllocationResult allocresult;
  private UID childUID;
                
  //no-arg constructor
  public NotificationImpl () {
    super();
  }

  //constructor that takes the Task, AllocationResult, and Plan
  public NotificationImpl (Task t, AllocationResult ar, Plan plan) {
    taskUID = t.getUID();
    allocresult = ar;
    setPlan(plan);
  }
                
  public NotificationImpl (UID tuid, AllocationResult ar, Plan plan) {
    taskUID = tuid;
    allocresult = ar;
    setPlan(plan);
  }

  /** implementations of the Notification interface */
                
  /** 
   * Returns the task UID the notification is in reference to.
   * @return Task 
   **/
                
  public UID getTaskUID() {
    return taskUID;
  }
                
  /**
   * Returns the estimated allocation result from below
   * @return AllocationResult
   **/
  public AllocationResult getAllocationResult() {
    return allocresult;
  }
  
  // implementation methods for the NewNotification interface

  /** 
   * Sets the task the notification is in reference to.
   * @param t 
   **/
                
  public void setTask(Task t) {
    taskUID = t.getUID();
  }
  public void setTaskUID(UID tuid) {
    taskUID = tuid;
  }
                
  /** Sets the combined estiamted allocationresult from below
    * @param ar - The AllocationResult for the Task.
    */
  public void setAllocationResult(AllocationResult ar) {
    allocresult = ar;
  }
  
  /** Sets the child task's UID that was disposed.  It's parent task is getTask();
    * Useful for keeping track of which subtask of an Expansion caused
    * the re-aggregation of the Expansion's reported allocationresult.
    * @param thechildUID
    */
  public void setChildTaskUID(UID thechildUID) {
    childUID = thechildUID;
  }
  
  /** Get the child task's UID that was disposed.  It's parent task is getTask();
    * Useful for keeping track of which subtask of an Expansion caused
    * the re-aggregation of the Expansion's reported allocationresult.
    * @return UID
    */
  public UID getChildTaskUID() {
    return childUID;
  }
    
                    
  /** Always serialize Notifications with TaskProxy
   */
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
  }

  public String toString() {
    return "<Notification for child " + childUID + " of " + taskUID+">";
  }
}

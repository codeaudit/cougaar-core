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

import org.cougaar.domain.planning.ldm.plan.AllocationResult;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.NewNotification;
import org.cougaar.domain.planning.ldm.plan.Notification;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.core.cluster.persist.PersistenceOutputStream;
import org.cougaar.core.society.UID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/** An implementation of org.cougaar.Notification
 */
public class NotificationImpl extends DirectiveImpl
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

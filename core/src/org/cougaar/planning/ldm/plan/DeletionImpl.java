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
import org.cougaar.planning.ldm.plan.NewDeletion;
import org.cougaar.planning.ldm.plan.Deletion;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.core.util.UID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/** An implementation of org.cougaar.Deletion
 */
public class DeletionImpl extends PlanningDirectiveImpl
  implements Deletion, NewDeletion
{
                
  private UID taskUID; 
                
  //no-arg constructor
  public DeletionImpl () {
    super();
  }

  //constructor that takes the Task, AllocationResult, and Plan
  public DeletionImpl (Task t, Plan plan) {
    taskUID = t.getUID();
    setPlan(plan);
  }
                
  public DeletionImpl (UID tuid, Plan plan) {
    taskUID = tuid;
    setPlan(plan);
  }

  /** implementations of the Deletion interface */
                
  /** 
   * Returns the task UID the deletion is in reference to.
   * @return Task 
   **/
                
  public UID getTaskUID() {
    return taskUID;
  }
  
  // implementation methods for the NewDeletion interface

  /** 
   * Sets the uid of the task the deletion is in reference to.
   * @param tuid
   **/
                
  public void setTaskUID(UID tuid) {
    taskUID = tuid;
  }

  /** Always serialize Deletions with TaskProxy
   */
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
  }

  public String toString() {
    return "<Deletion for " + taskUID+">";
  }
}

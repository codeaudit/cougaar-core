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

import org.cougaar.domain.planning.ldm.plan.AllocationResult;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.NewDeletion;
import org.cougaar.domain.planning.ldm.plan.Deletion;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.core.cluster.persist.PersistenceOutputStream;
import org.cougaar.core.society.UID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/** An implementation of org.cougaar.Deletion
 */
public class DeletionImpl extends DirectiveImpl
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

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

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.society.UID;
import org.cougaar.domain.planning.ldm.plan.NewTaskRescind;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.TaskRescind;
import org.cougaar.core.cluster.persist.PersistenceOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** TaskRescind implementation
 * TaskRescind allows a task to be rescinded from the Plan. 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: TaskRescindImpl.java,v 1.2 2001-04-05 19:27:21 mthome Exp $
 **/

public class TaskRescindImpl 
  extends DirectiveImpl
  implements TaskRescind, NewTaskRescind
{

  private UID taskUID = null;
        
  /** @param src
   * @param dest
   * @param t
   * @return TaskRescindImpl
   **/
  public TaskRescindImpl(ClusterIdentifier src, ClusterIdentifier dest, Plan plan, Task t) {
    super.setSource(src);
    super.setDestination(dest);
    super.setPlan(plan);
    taskUID = t.getUID();
  }

  public TaskRescindImpl(ClusterIdentifier src, ClusterIdentifier dest, Plan plan, UID tuid) {
    super.setSource(src);
    super.setDestination(dest);
    super.setPlan(plan);
    taskUID = tuid;
  }

  /**
   * Returns the task to be rescinded
   * @return Task
   */

  public UID getTaskUID() {
    return taskUID;
  }
     
  /**
   * Sets the task to be rescinded
   * @param Task
   * @deprecated
   */

  public void setTask(Task atask) {
    taskUID = atask.getUID();
  }

  public void setTaskUID(UID tuid) {
    taskUID = tuid;
  }
       
   
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
  }

  public String toString() {
    return "<TaskRescind for " + taskUID + ">";
  }

}

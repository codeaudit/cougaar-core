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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.plan.NewTaskRescind;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TaskRescind;
import org.cougaar.core.persist.PersistenceOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** TaskRescind implementation
 * TaskRescind allows a task to be rescinded from the Plan. 
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/

public class TaskRescindImpl 
  extends DirectiveImpl
  implements TaskRescind, NewTaskRescind
{

  private UID taskUID = null;
        
  /** @param src
   * @param dest
   * @param t
   **/
  public TaskRescindImpl(MessageAddress src, MessageAddress dest, Plan plan, Task t) {
    super.setSource(src);
    super.setDestination(dest);
    super.setPlan(plan);
    taskUID = t.getUID();
  }

  public TaskRescindImpl(MessageAddress src, MessageAddress dest, Plan plan, UID tuid) {
    super.setSource(src);
    super.setDestination(dest);
    super.setPlan(plan);
    taskUID = tuid;
  }

  /**
   * Returns the task to be rescinded
   */

  public UID getTaskUID() {
    return taskUID;
  }
     
  /**
   * Sets the task to be rescinded
   * @deprecated
   */

  public void setTask(Task atask) {
    taskUID = atask.getUID();
  }

  /**
   * Sets the task UID to be rescinded
   * @deprecated
   */
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

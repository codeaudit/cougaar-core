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

package org.cougaar.domain.planning.ldm.plan;

import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import org.cougaar.util.Enumerator;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.NewTask;
import org.cougaar.domain.planning.ldm.plan.MPTask;
import org.cougaar.domain.planning.ldm.plan.NewMPTask;
import org.cougaar.domain.planning.ldm.plan.Composition;
import org.cougaar.core.society.UID;
import org.cougaar.core.society.UniqueObject;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.lang.Long;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;


/** MPTask implementation - MPTasks should only be created or used if they are
 * attached to MPWorkflows.  MPTasks always have multiple ParentTasks.
 **/
        
public final class MPTaskImpl 
  extends TaskImpl 
  implements MPTask, NewMPTask
{
        
  //private transient  Vector parenttasks = new Vector();
  private transient List parenttasks = new ArrayList();
  private Composition comp;
        
  /** Constructor that takes no args */
  public MPTaskImpl(UID uid) {
    super(uid);
  }
  
  /** 
   * Returns the base or parent tasks of
   * a given task, where the given task is
   * an aggregation expansion of the base tasks. The
   * These tasks are members of MPWorkflows.
   * @return Enumeration{Task} that are the "parenttasks"
   **/
                
  public Enumeration getParentTasks() {
    synchronized(parenttasks) {
      List copy = new ArrayList(parenttasks);
      return new Enumerator(copy);
    }
    //return parenttasks.elements();
  }
  
  /**
   * Override getParentTask in Task.java and return null.
   * Dump stack as this shouldn't be called for MPTask, because
   * MPTasks have more than one parent.
   **/
  public Task getParentTask() {
    Thread.dumpStack();
    return null;
  }
  
  /**
    * The Composition object that created this task.
    * @return Composition
    * @see org.cougaar.domain.planning.ldm.plan.Composition
    */
  public Composition getComposition() {
    if (comp == null) {
      System.err.println("MPTask is MALFORMED - The composition is null!");
      Thread.dumpStack();
    }
    return comp;
  }
        
  /**
   * Do not allow this to be called on an MPTask, since there are by 
   * definition more than one Parent Task of an MPTask.
   **/
  public void setParentTask(Task parentTask) {
    throw new RuntimeException("This method is NOT to be called for an MPTask. MPTasks have more than one Parent Task, use: setParentTasks(Enum)");
  }

  public void setParentTaskUID(UID parentTaskuid) {
    throw new RuntimeException("This method is NOT to be called for an MPTask. MPTasks have more than one Parent Task, use: setParentTasks(Enum)");
  }

                
  /** 
   * Sets the base or parent tasks of
   * a given task, where the given task is
   * an aggregation expansion of the base tasks. 
   * Note that an MPTask contains multiple ParentTasks.
   * If the Context of the MPTask hasn't been set, the
   * Context will be set the be the union of Contexts of the parenttasks
   * @param pt - Enumeration of Tasks which are the "parenttasks"
   **/
                
  public void setParentTasks(Enumeration pt) {
    synchronized(parenttasks) {
      parenttasks.clear();
      while (pt.hasMoreElements()) {
        Task t = (Task) pt.nextElement();
        if ( t instanceof Task ) {
          parenttasks.add(t);
        } else {
          // buzzz... wrong answer - tryed to pass in a null!
          throw new IllegalArgumentException("MPTask.setParentTask(Enumeration pt): all elements of pt must be Tasks");
        }
      }
    }
    if (getContext() == null) {
      setMPContext();
    }
  }
  
  /**
   * Set the Composition that created this task.
   * @param aComposition
   */
  public void setComposition(Composition aComposition) {
    comp = aComposition;
  }
  
  /* for infrastructure only */
  protected void removeParentTask(Task ptask) {
    synchronized(parenttasks) {
      parenttasks.remove(ptask);
    }
  }
  
  //
  // serialization
  //

  /** serialize tasks making certain that references to other tasks and
   * workflows are appropriately proxied.
   */
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();

    stream.writeObject(parenttasks);
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
    parenttasks = (ArrayList) stream.readObject();
  }

  // for UI

  public String[] getParentTaskIDs() {
    synchronized(parenttasks) {
      String UUIDs[] = new String[parenttasks.size()];
      for (int i = 0; i < parenttasks.size(); i++)
        UUIDs[i] = ((UniqueObject)(parenttasks.get(i))).getUID().toString();
      return UUIDs;
    }
  }

  public String getParentTaskID(int i) {
    String UUIDs[] = getParentTaskIDs();
    if (i < UUIDs.length)
      return UUIDs[i];
    else
      return null;
  }

  /**
   * Set the Context of the MPTask to be the union of the Contexts in the parenttasks
   * This works only if the Contexts are instances of ContextOfUIDs
   * Any Context that is not a ContextOfUIDs will not be included 
   */
  private void setMPContext() {
    synchronized(parenttasks) {
      HashSet union = new HashSet(5);
      for (int i = 0; i < parenttasks.size(); i++) {
	Context c = ((Task)parenttasks.get(i)).getContext();
	if (c instanceof ContextOfUIDs) {
	  UID[] uids = (UID[])((ContextOfUIDs)c).toArray();
	  for (int j=0; j<uids.length; j++) {
            union.add(uids[j]);
	  }
	}
	ContextOfUIDs newContext = new ContextOfUIDs(union);
	if (newContext.size() > 0 )
	  setContext(newContext);
      }
    }
  }
}

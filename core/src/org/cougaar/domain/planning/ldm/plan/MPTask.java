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

import java.util.Enumeration;

/** MPTask Interface
  * MPTask is a subclass of Task which has a reference to and is derived from
  * multiple ParentTasks instead of one ParentTask
  * @see org.cougaar.domain.planning.ldm.plan.Task
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: MPTask.java,v 1.2 2001-04-05 19:27:15 mthome Exp $
  **/
	
public interface MPTask extends Task
{
		
  /** 
   * Returns the base or parent tasks of
   * a given task, where the given task is
   * an aggregation expansion of the base tasks. The
   * These tasks are members of MPWorkflows.
   * @return Enumeration{UID} UIDs of the tasks that are the "parenttasks"
   **/
		
  Enumeration getParentTasks();
  
  /**
    * The Composition object that created this task.
    * @return Composition
    * @see org.cougaar.domain.planning.ldm.plan.Composition
    */
    
  Composition getComposition();
  
  }

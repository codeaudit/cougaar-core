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

/** MPTask Interface
  * MPTask is a subclass of Task which has a reference to and is derived from
  * multiple ParentTasks instead of one ParentTask
  * @see org.cougaar.domain.planning.ldm.plan.Task
  * @author  ALPINE <alpine-software@bbn.com>
  *
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

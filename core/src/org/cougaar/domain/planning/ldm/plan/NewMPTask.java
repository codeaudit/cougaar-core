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

import java.util.Enumeration;

/** NewMPTask Interface
	* Provide setters for building MPTask objects.
  * MPTask is a subclass of Task
  * @see org.cougaar.domain.planning.ldm.plan.NewTask
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: NewMPTask.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
  **/
	
public interface NewMPTask extends MPTask, NewTask
{
		
  /** 
   * Sets the base or parent tasks of
   * a given task, where the given task is
   * an aggregation expansion of the base tasks. 
   * Note that an MPTask contains multiple ParentTasks.
   * @param pt - Enumeration of Tasks which are the "parenttasks"
   **/
		
  void setParentTasks(Enumeration pt);
  
  /**
   * Set the Composition that created this task.
   * @param aComposition
   */
  void setComposition(Composition aComposition);
  
  }
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


package org.cougaar.domain.planning.ldm.trigger;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.core.plugin.PlugInDelegate;

/**
 * Trigger action to generate a new task when fired - abstract method
 * for task generation
 */

public abstract class TriggerMakeTaskAction implements TriggerAction {

  // Provide TriggerAction method : publish generated task
  public void Perform(Object[] objects, PlugInDelegate pid) {
    Task task = GenerateTask(objects, pid);
    if (task != null) {
      pid.publishAdd(task);
    }
  }

  /** Abstract method to generate a new task from the set of objects provided
    * @param objects  The objects to work from
    * @param pid  The PlugInDelegate to use for things like getClusterObjectFactory.
    * @return Task  The new task.
    */
  public abstract Task GenerateTask(Object[] objects, PlugInDelegate pid);
    
 

}


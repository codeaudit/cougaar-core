/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
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


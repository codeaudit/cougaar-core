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

package org.cougaar.planning.ldm.lps;

import org.cougaar.core.blackboard.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.domain.LogPlanLogicProvider;
import org.cougaar.core.domain.MessageLogicProvider;

import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.TaskImpl;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import java.util.Collection;

/**
 * Sample LogicProvider for use by ClusterDispatcher to
 * take an incoming Task (excepting Rescind task) and
 * add to the LogPlan w/side-effect of also disseminating to
 * other subscribers.
 * Only adds tasks that haven't been seen before, allowing stability
 * in the face of wire retransmits.
 **/

public class ReceiveTaskLP extends LogPlanLogicProvider implements MessageLogicProvider
{
  private static Logger logger = Logging.getLogger(ReceiveTaskLP.class);

  public ReceiveTaskLP(LogPlanServesLogicProvider logplan,
                       ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }

  /**
   * Adds Task to LogPlan... Side-effect = other subscribers also
   * updated. If the task is already in the logplan, then there is
   * probably a change in task preferences. If there is no change in
   * task preferences, then it might be the case that the sending
   * cluster has undergone a restart and is trying to resynchronize
   * its tasks. We need to activate the NotificationLP to send the
   * estimated allocation result for the plan element of the task. We
   * do this by publishing a change of the plan element (if it
   * exists).
   **/
  public void execute(Directive dir, Collection changes)
  {
    if (dir instanceof Task) {
      Task tsk = (Task) dir;

      try {
        Task existingTask = logplan.findTask(tsk);
        if (existingTask == null) {
          // only add if it isn't already there.
	  //System.err.print("!");
          logplan.add(tsk);
        } else if (tsk == existingTask) {
          logplan.change(existingTask, changes);
        } else {
          Preference[] newPreferences = ((TaskImpl) tsk).getPreferencesAsArray();
          Preference[] existingPreferences = ((TaskImpl) existingTask).getPreferencesAsArray();
          if (java.util.Arrays.equals(newPreferences, existingPreferences)) {
            PlanElement pe = existingTask.getPlanElement();
            if (pe != null) {
              logplan.change(pe, changes);	// Cause estimated result to be resent
            }
          } else {
            ((NewTask) existingTask).setPreferences(tsk.getPreferences());
            logplan.change(existingTask, changes);
          }
        }
      } catch (SubscriberException se) {
        logger.error("Could not add Task to LogPlan: "+tsk);
        se.printStackTrace();
      }
    }
  }
}

/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.lps;
import org.cougaar.core.cluster.*;

import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.NewTask;
import org.cougaar.domain.planning.ldm.plan.Preference;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.TaskImpl;

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
        System.err.println("Could not add Task to LogPlan: "+tsk);
        se.printStackTrace();
      }
    }
  }
}

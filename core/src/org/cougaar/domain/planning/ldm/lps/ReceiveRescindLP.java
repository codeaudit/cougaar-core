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

import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.core.cluster.*;
import org.cougaar.core.society.UID;
import org.cougaar.domain.planning.ldm.asset.Asset;
import java.util.*;

import org.cougaar.domain.planning.ldm.plan.TaskImpl;

/**
  * LogicProvider for use by ClusterDispatcher to
  * take an incoming Rescind Directive and
  * perform Modification to the LOGPLAN
  * 
  *  1. Rescind Task - removes the task and any plan elements which
  *   address the that task.  Any cascade effect is then handled by
  *   RescindLP.
  **/

public class ReceiveRescindLP extends LogPlanLogicProvider implements MessageLogicProvider
{

  public ReceiveRescindLP(LogPlanServesLogicProvider logplan,
                          ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }

  /**
   *  perform updates -- per Rescind ALGORITHM --
   *
   **/
  public void execute(Directive dir, Collection changes) {
    // drop changes
    if (dir instanceof TaskRescind) {
      receiveTaskRescind((TaskRescind) dir);
    }
  }

  private void receiveTaskRescind(TaskRescind tr) {
    UID tuid = tr.getTaskUID();
    System.err.print("R");

    // just rescind the task; let the RescindLP handle the rest
    //
    Task t = logplan.findTask(tuid);
    if (t != null) {
      logplan.remove(t);
    } else {
      // System.err.println("Couldn't find task to rescind: " + rt.getUID());
      logplan.add(new RescindLP.DeferredRescind(tr));
    }
  }
}
 

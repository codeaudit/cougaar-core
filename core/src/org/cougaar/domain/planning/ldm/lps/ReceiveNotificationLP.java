/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.lps;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.ClusterServesLogicProvider;
import org.cougaar.core.cluster.LogPlanLogicProvider;
import org.cougaar.core.cluster.LogPlanServesLogicProvider;
import org.cougaar.core.cluster.MessageLogicProvider;


import org.cougaar.domain.planning.ldm.plan.Aggregation;
import org.cougaar.domain.planning.ldm.plan.Allocation;
import org.cougaar.domain.planning.ldm.plan.AllocationResult;
import org.cougaar.domain.planning.ldm.plan.AssetTransfer;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.Disposition;
import org.cougaar.domain.planning.ldm.plan.Expansion;
import org.cougaar.domain.planning.ldm.plan.ExpansionImpl;
import org.cougaar.domain.planning.ldm.plan.MPTask;
import org.cougaar.domain.planning.ldm.plan.NewNotification;
import org.cougaar.domain.planning.ldm.plan.NewTask;
import org.cougaar.domain.planning.ldm.plan.Notification;
import org.cougaar.domain.planning.ldm.plan.PEforCollections;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.SubTaskResult;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.TaskRescind;
import org.cougaar.domain.planning.ldm.plan.TaskScoreTable;
import org.cougaar.domain.planning.ldm.plan.Workflow;
import org.cougaar.domain.planning.ldm.plan.WorkflowImpl;
import org.cougaar.core.society.UID;

import java.util.*;

import java.lang.InterruptedException;

import org.cougaar.util.UnaryPredicate;

/**
 * Sample LogicProvider for use by ClusterDispatcher to
 * take an incoming Notification Directive and
 * perform Modification to the LOGPLAN
 **/

public class ReceiveNotificationLP
  extends LogPlanLogicProvider
  implements MessageLogicProvider
{
  public ReceiveNotificationLP(LogPlanServesLogicProvider logplan,
                               ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }

  /**
   *  perform updates -- per Notification ALGORITHM --
   *
   **/
  public void execute(Directive dir, Collection changes) {
    if (dir instanceof Notification) {
      processNotification((Notification) dir, changes);
    }
  }
     
  private void processNotification(Notification not, Collection changes) {
    UID tuid = not.getTaskUID();
    UID childuid = not.getChildTaskUID();
    PlanElement pe = logplan.findPlanElement(tuid);
    if (pe == null) {
      TaskRescind trm = ldmf.newTaskRescind(childuid, not.getSource());
      logplan.sendDirective(trm, changes);
      return;
    }
    AllocationResult ar = not.getAllocationResult();
    propagateNotification(logplan, tuid, ar, childuid, changes);
  }

  // default protection so that NotificationLP can call this method
  static final void propagateNotification(LogPlanServesLogicProvider logplan,
                                          UID tuid, AllocationResult result, 
                                          UID childuid, Collection changes) {
    PlanElement pe = logplan.findPlanElement(tuid.toString());
    if (pe == null) {
//    System.out.println("Received notification about unknown task: " + tuid);
      return;
    }
    
    if ((pe instanceof Allocation) ||
        (pe instanceof AssetTransfer) ||
        (pe instanceof Aggregation)) {
      ((PEforCollections) pe).setReceivedResult(result);
      logplan.change(pe, changes);
    } else if (pe instanceof Expansion) {
      Workflow wf = ((Expansion)pe).getWorkflow();
      AllocationResult ar = wf.aggregateAllocationResults();
      if (ar != null) {
	// get the TaskScoreTable used in the aggregation
	TaskScoreTable aggTST = ((WorkflowImpl)wf).getCurrentTST();
	// get the UID of the child task that caused this aggregation
	((ExpansionImpl)pe).setSubTaskResults(aggTST,childuid);
        ((PEforCollections) pe).setReceivedResult(ar);
	logplan.change(pe, changes);
      } // if we can't successfully aggregate the results - don't send a notification
    /*
    } else if (pe instanceof Disposition) {
      // drop the notification on the floor - cannot possibly be valid
    } 
    */
    } else {
      System.err.print("Got a Notification for an inappropriate PE:\n"+
                       "\tTask="+tuid+"\n"+
                       "\tFrom="+childuid+"\n"+
                       "\tResult="+result+"\n"+
                       "\tPE="+pe);
    }
  }
}

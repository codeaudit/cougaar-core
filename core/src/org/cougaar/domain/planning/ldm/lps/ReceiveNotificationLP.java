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

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.ClusterServesLogicProvider;
import org.cougaar.core.cluster.LogPlanLogicProvider;
import org.cougaar.core.cluster.LogPlanServesLogicProvider;
import org.cougaar.core.cluster.MessageLogicProvider;
import org.cougaar.core.cluster.DelayedLPAction;

import org.cougaar.domain.planning.ldm.plan.Aggregation;
import org.cougaar.domain.planning.ldm.plan.Allocation;
import org.cougaar.domain.planning.ldm.plan.AllocationforCollections;
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
    // verify that the pe matches the task
    if (pe instanceof AllocationforCollections) {
      Task remoteT = ((AllocationforCollections)pe).getAllocationTask();
      UID remoteTUID = remoteT.getUID();
      if (!(remoteTUID.equals(childuid))) {
        // this was likely due to a race condition...
        System.err.print(
            "Got a Notification for the wrong allocation:"+
            "\n\tTask="+tuid+
            "  ("+pe.getTask().getUID()+")"+
            "\n\tFrom="+childuid+
            "  ("+remoteTUID+")"+
            "\n\tResult="+not.getAllocationResult()+"\n"+
            "\n\tPE="+pe);
        // rescind the remote task? 
        return;
      }
    }
    AllocationResult ar = not.getAllocationResult();
    propagateNotification(logplan, pe, tuid, ar, childuid, changes);
  }

  // default protection so that NotificationLP can call this method
  static final void propagateNotification(LogPlanServesLogicProvider logplan,
                                          UID tuid, AllocationResult result,
                                          UID childuid, Collection changes) {
    PlanElement pe = logplan.findPlanElement(tuid);
    if (pe != null) {
      propagateNotification(logplan, pe, tuid, result, childuid, changes);
    } else {
      // System.out.println("Received notification about unknown task: "+tuid);
    }
  }

  // default protection so that NotificationLP can call this method
  static final void propagateNotification(LogPlanServesLogicProvider logplan,
                                          PlanElement pe,
                                          UID tuid, AllocationResult result,
                                          UID childuid, Collection changes) {
    if ((pe instanceof Allocation) ||
        (pe instanceof AssetTransfer) ||
        (pe instanceof Aggregation)) {
      ((PEforCollections) pe).setReceivedResult(result);
      logplan.change(pe, changes);
    } else if (pe instanceof Expansion) {
      logplan.delayLPAction(
          new DelayedAggregateResults((Expansion)pe, childuid));

      /*
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
      */
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

  /** delay the results aggregation of an expansion until the end in case
   * we have lots of them to do.
   **/
  private final static class DelayedAggregateResults
    implements DelayedLPAction
  {
    private final Expansion pe;
    private final ArrayList ids = new ArrayList(1);
    DelayedAggregateResults(Expansion pe, UID id) {
      this.pe = pe;
      ids.add(id);
    }

    public void execute(LogPlanServesLogicProvider logplan) {
      Workflow wf = pe.getWorkflow();

      // compute the new result from the subtask results.
      AllocationResult ar = wf.aggregateAllocationResults();
      if (ar != null) {         // if the aggragation is defined:

	// get the TaskScoreTable used in the aggregation
	TaskScoreTable aggTST = ((WorkflowImpl)wf).getCurrentTST();

	// get the UID of the child task that caused this aggregation
        int l = ids.size();
        for (int i = 0; i<l; i++) {
          UID childuid = (UID) ids.get(i);
          // yuck! another n^2 operation.  sigh.
          // surely we should be able to do better...
          ((ExpansionImpl)pe).setSubTaskResults(aggTST,childuid);
        }

        // set the result on the
        ((PEforCollections) pe).setReceivedResult(ar);

        // publish the change to the blackboard.
	logplan.change(pe, null); // drop the change details.
	//logplan.change(pe, changes);
        //System.err.print("=");
      }
    }

    /** hashcode is the hashcode of the expansion **/
    public int hashCode() {
      return pe.hashCode();
    }

    /** these guys are equal iff the they have the same PE **/
    public boolean equals(Object e) {
      return (e instanceof DelayedAggregateResults &&
              ((DelayedAggregateResults)e).pe == pe);
    }

    /** merge another one into this one **/
    public void merge(DelayedLPAction e) {
      // don't bother to check, since we will only be here if this.equals(e).
      DelayedAggregateResults other = (DelayedAggregateResults) e;
      ids.addAll(other.ids);
    }
  }
}

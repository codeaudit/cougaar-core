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

import org.cougaar.core.cluster.*;

import org.cougaar.domain.planning.ldm.asset.Asset;

import org.cougaar.domain.planning.ldm.plan.Aggregation;
import org.cougaar.domain.planning.ldm.plan.Allocation;
import org.cougaar.domain.planning.ldm.plan.AllocationResult;
import org.cougaar.domain.planning.ldm.plan.AssetTransfer;
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.Disposition;
import org.cougaar.domain.planning.ldm.plan.Expansion;
import org.cougaar.domain.planning.ldm.plan.MPTask;
import org.cougaar.domain.planning.ldm.plan.NewNotification;
import org.cougaar.domain.planning.ldm.plan.NewTask;
import org.cougaar.domain.planning.ldm.plan.PEforCollections;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.ScheduleElement;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.TaskScoreTable;

import org.cougaar.core.society.UID;

import org.cougaar.util.UnaryPredicate;

import java.util.Enumeration;
import java.util.Collection;

/** RescindLogicProvider class provides the logic to capture 
 * rescinded PlanElements (removed from collection)
  *
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: NotificationLP.java,v 1.4 2001-04-05 19:14:44 mthome Exp $
  **/

public class NotificationLP
  extends LogPlanLogicProvider
  implements EnvelopeLogicProvider, RestartLogicProvider
{
  private ClusterIdentifier cid;

  public NotificationLP(LogPlanServesLogicProvider logplan,
                        ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
    cid = cluster.getClusterIdentifier();
  }

  /**
   *  @param Object an Envelope.Tuple.object is an ADDED 
   * PlanElement which contains an Allocation to an Organization.
   * Do something if the test returned true i.e. it was an Allocation
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    if ( ( o.isAdd() && (obj instanceof PlanElement)) ||
         ( o.isChange() &&
           ( obj instanceof PlanElement ) &&
           ((PEforCollections) obj).shouldDoNotification())
         ) {
      PlanElement pe = (PlanElement) obj;
      checkValues(pe, changes);
    } 
  }

  public void restart(final ClusterIdentifier cid) {
    System.out.println("Verifying received tasks");
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof PlanElement) {
          PlanElement pe = (PlanElement) o;
          return cid.equals(pe.getTask().getSource());
        }
        return false;
      }
    };
    Enumeration enum = logplan.searchBlackboard(pred);
    while (enum.hasMoreElements()) {
      checkValues((PlanElement) enum.nextElement(), null);
    }
  }
  	
  private void checkValues(PlanElement pe, Collection changes) {
    AllocationResult ar = pe.getEstimatedResult();
    if (ar != null) {
      Task task = pe.getTask();
      //System.err.println( "\n" + cid + ": task = " + task );
      ((PEforCollections)pe).setNotification(false);
      if (task instanceof MPTask) {
        TaskScoreTable resultsbytask = ((MPTask)task).getComposition().calculateDistribution();
        if (resultsbytask != null) {
          Enumeration etasks = ((MPTask)task).getParentTasks();
          while (etasks.hasMoreElements()) {
            Task pt = (Task) etasks.nextElement();
            if (pt != null) {
              AllocationResult result = resultsbytask.getAllocationResult(pt);
              if (result != null) {
		createNotification(pt.getUID(), task, result, changes);
              }
            } // else no notification need be generated
          }
        }
      } else {
        UID ptuid = task.getParentTaskUID();
        if (ptuid != null) {
          createNotification(ptuid, task, ar, changes);
        } // else no notification need be generated
      }
    }
    // do nothing if the estimated allocation result was null
  }
  
  private void createNotification(UID ptuid, Task t, AllocationResult ar, Collection changes) {
    ClusterIdentifier dest = t.getSource();
    if (cid == dest || cid.equals(dest)) {
      // deliver intra-cluster notifications directly
      ReceiveNotificationLP.propagateNotification(logplan,ptuid,ar,t.getUID(), changes);
    } else {
      // need to send an actual notification
      NewNotification nn = ldmf.newNotification();
      nn.setTaskUID(ptuid);
      nn.setPlan(t.getPlan());
      nn.setAllocationResult(ar);
      // set the UID of the child task for Expansion aggregation change purposes
      nn.setChildTaskUID(t.getUID());
      if (ptuid == null) System.out.println("***DebugNotification-LP: pt is null");
      ClusterIdentifier newDest = t.getSource();
      //ClusterIdentifier newDest = pt.getDestination();
      ClusterIdentifier newSource =  cid;
      
      nn.setSource(newSource);
      nn.setDestination(newDest);
      logplan.sendDirective(nn, changes);
    }
  }
}

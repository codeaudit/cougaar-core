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

import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Allocation;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.ClusterPG;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.MPTask;
import org.cougaar.domain.planning.ldm.plan.NewMPTask;
import org.cougaar.domain.planning.ldm.plan.NewTask;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.domain.planning.ldm.plan.AllocationforCollections;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;


/** RemoteClusterAllocationLogicProvider class provides the logic to capture 
 * Allocations against remote Clusters 
 *
 **/

public class RemoteClusterAllocationLP extends LogPlanLogicProvider
  implements EnvelopeLogicProvider, RestartLogicProvider
{
        
  public RemoteClusterAllocationLP(LogPlanServesLogicProvider logplan,
                                   ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }

  private void examine(Object obj, Collection changes) {

    if (! (obj instanceof Allocation)) return;
    Allocation all = (Allocation) obj;
    Task task = all.getTask();
    Asset asset = all.getAsset();
    ClusterPG cpg = asset.getClusterPG();
    if (cpg == null) return;
    ClusterIdentifier destination = cpg.getClusterIdentifier();
    if (destination == null) return;

    // see if we're reissuing the task... if so, we'll just use it.
    Task copytask = ((AllocationforCollections)all).getAllocationTask();
    if (copytask == null) {
      // if not, make a new task to send.
      copytask = prepareNewTask(cluster, task, destination);
      ((AllocationforCollections)all).setAllocationTask(copytask);
      logplan.change(all, changes); 
    }

    // Give the task directive to the logplan for transmission
    logplan.sendDirective(copytask, changes);
  }


  /**
   * Handle one EnvelopeTuple. Call examine to check for objects that
   * are Allocations to a remote Cluster.
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    if (o.isAdd()) {
      examine(obj, changes);
    } else if (o.isBulk()) {
      Collection c = (Collection) obj;
      for (Iterator e = c.iterator(); e.hasNext(); ) {
        examine(e.next(), changes);
      }
    }
  }

  /**
   * If a cluster restarts, we resend all the tasks we sent before in
   * case they have been lost or are out of date.
   **/
  public void restart(final ClusterIdentifier cid) {
    System.out.println("Resending tasks to " + cid);
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Allocation alloc = (Allocation) o;
          Task task = alloc.getTask();
          Asset asset = alloc.getAsset();
          ClusterPG cpg = asset.getClusterPG();
          if (cpg == null) return false;
          ClusterIdentifier destination = cpg.getClusterIdentifier();
          return cid.equals(destination);
        }
        return false;
      }
    };
    Enumeration enum = logplan.searchALPPlan(pred);
    while (enum.hasMoreElements()) {
      AllocationforCollections alloc = (AllocationforCollections) enum.nextElement();
      System.out.println("Resending " + alloc);
      Task remoteTask = alloc.getAllocationTask();
      if (remoteTask != null) logplan.sendDirective(remoteTask);
    }
    System.out.println("Resending finished");
  }

  private Task prepareNewTask(ClusterServesLogicProvider cluster, Task task, ClusterIdentifier dest) {
    NewTask nt;
    /*
    if (task instanceof MPTask) {
      nt = ldmf.newMPTask();
      ((NewMPTask)nt).setParentTasks(((MPTask)task).getParentTasks());
    }
    */
    nt = ldmf.newTask();
    nt.setParentTask(task);             // set ParenTask to original task

    // redundant: ldmf initializes it.
    //nt.setSource(cluster.getClusterIdentifier());

    // FIXME MIK WARNING! WARNING!
    // as a hack, we've made setDestination bark if it isn't the current
    // cluster (suspicious use).  In order to prevent the below from 
    // generating barkage, we've got a (privately) muzzle...
    //nt.setDestination(dest);
    // 
    ((org.cougaar.domain.planning.ldm.plan.TaskImpl)nt).privately_setDestination(dest);
    nt.setVerb(task.getVerb());
    nt.setDirectObject(task.getDirectObject());
    nt.setPrepositionalPhrases(task.getPrepositionalPhrases());
    // no workflow
    nt.setPreferences(task.getPreferences());
    nt.setPriority(task.getPriority());
    nt.setPlan(task.getPlan());
    nt.setAuxiliaryQueryTypes(task.getAuxiliaryQueryTypes());
    nt.setContext(task.getContext());

    /*
      NewTask nt = ldmf.shadowTask(task);
      nt.setSource(cluster.getClusterIdentifier());
      nt.setDestination(dest);
    */
    return nt;
  }
}

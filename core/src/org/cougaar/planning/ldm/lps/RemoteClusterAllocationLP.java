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
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogPlanLogicProvider;
import org.cougaar.core.domain.RestartLogicProvider;


import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.NewMPTask;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
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
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Allocation alloc = (Allocation) o;
          Asset asset = alloc.getAsset();
          ClusterPG cpg = asset.getClusterPG();
          if (cpg == null) return false;
          ClusterIdentifier destination = cpg.getClusterIdentifier();
          return cid.equals(destination);
        }
        return false;
      }
    };
    Enumeration enum = logplan.searchBlackboard(pred);
    while (enum.hasMoreElements()) {
      AllocationforCollections alloc = (AllocationforCollections) enum.nextElement();
      Task remoteTask = alloc.getAllocationTask();
      if (remoteTask != null) logplan.sendDirective(remoteTask);
    }
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
    ((org.cougaar.planning.ldm.plan.TaskImpl)nt).privately_setDestination(dest);
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

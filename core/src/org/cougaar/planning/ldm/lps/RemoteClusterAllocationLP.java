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
import org.cougaar.planning.ldm.*;
import org.cougaar.core.domain.*;

import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.NewMPTask;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;


/** RemoteClusterAllocationLogicProvider class provides the logic to capture 
 * Allocations against remote Clusters 
 *
 **/

public class RemoteClusterAllocationLP
implements LogicProvider, EnvelopeLogicProvider, RestartLogicProvider
{
        
  private static final Logger logger = 
    LoggerFactory.getInstance().createLogger(RemoteClusterAllocationLP.class);

  private final RootPlan rootplan;
  private final PlanningFactory ldmf;
  private final MessageAddress self;

  public RemoteClusterAllocationLP(
      RootPlan rootplan,
      PlanningFactory ldmf,
      MessageAddress self) {
    this.rootplan = rootplan;
    this.ldmf = ldmf;
    this.self = self;
  }

  public void init() {
  }

  private void examine(Object obj, Collection changes) {

    if (! (obj instanceof Allocation)) return;
    Allocation all = (Allocation) obj;
    Task task = all.getTask();
    Asset asset = all.getAsset();
    ClusterPG cpg = asset.getClusterPG();
    if (cpg == null) return;
    MessageAddress destination = cpg.getMessageAddress();
    if (destination == null) return;

    // see if we're reissuing the task... if so, we'll just use it.
    Task copytask = ((AllocationforCollections)all).getAllocationTask();
    if (copytask == null) {
      // if not, make a new task to send.
      copytask = prepareNewTask(task, destination);
      ((AllocationforCollections)all).setAllocationTask(copytask);
      rootplan.change(all, changes); 
    }

    // Give the task directive to the blackboard for transmission
    rootplan.sendDirective(copytask, changes);
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
   * If a agent restarts, we resend all the tasks we sent before in
   * case they have been lost or are out of date.
   **/
  public void restart(final MessageAddress cid) {
    if (logger.isInfoEnabled()) {
      logger.info(
        self+": Reconcile with "+
        (cid==null?"all agents":cid.toString()));
    }
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Allocation alloc = (Allocation) o;
          Asset asset = alloc.getAsset();
          ClusterPG cpg = asset.getClusterPG();
          if (cpg == null) return false;
          MessageAddress destination = cpg.getMessageAddress();
          return 
            RestartLogicProviderHelper.matchesRestart(
              self, cid, destination);
        }
        return false;
      }
    };
    Enumeration enum = rootplan.searchBlackboard(pred);
    while (enum.hasMoreElements()) {
      AllocationforCollections alloc = (AllocationforCollections) enum.nextElement();
      Task remoteTask = alloc.getAllocationTask();
      if (remoteTask != null) {
        if (logger.isInfoEnabled()) {
          Task localTask = alloc.getTask();
          logger.info(
              self+": Resend"+(cid==null?"*":"")+
              " task to "+remoteTask.getDestination()+
              " with remoteUID="+remoteTask.getUID()+
              " "+localTask);
        }
        rootplan.sendDirective(remoteTask);
      }
    }
    if (logger.isInfoEnabled()) {
      logger.info(self+": Reconciled");
    }
  }

  private Task prepareNewTask(Task task, MessageAddress dest) {
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
    //nt.setSource(self);

    // FIXME MIK WARNING! WARNING!
    // as a hack, we've made setDestination bark if it isn't the current
    // agent (suspicious use).  In order to prevent the below from 
    // generating barkage, we've got a (privately) muzzle...
    //nt.setDestination(dest);
    // 
    ((org.cougaar.planning.ldm.plan.TaskImpl)nt).privately_setDestination(dest);
    nt.setVerb(task.getVerb());
    nt.setDirectObject(task.getDirectObject());
    nt.setPrepositionalPhrases(task.getPrepositionalPhrases());
    // no workflow
    synchronized (task) {
      nt.setPreferences(task.getPreferences());
    }
    nt.setPriority(task.getPriority());
    nt.setPlan(task.getPlan());
    nt.setAuxiliaryQueryTypes(task.getAuxiliaryQueryTypes());
    nt.setContext(task.getContext());

    /*
      NewTask nt = ldmf.shadowTask(task);
      nt.setSource(self);
      nt.setDestination(dest);
    */
    return nt;
  }
}

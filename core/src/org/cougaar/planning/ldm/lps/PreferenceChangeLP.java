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
import org.cougaar.core.domain.*;
import org.cougaar.planning.ldm.*;

import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
import org.cougaar.planning.ldm.plan.TaskImpl;
import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

/** PreferenceChangeLogicProvider class provides the logic to propogate
 *  preference changes to tasks that have been sent to other clusters
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/

public class PreferenceChangeLP
implements LogicProvider, EnvelopeLogicProvider {

  private final RootPlan rootplan;

  public PreferenceChangeLP(RootPlan rootplan) {
    this.rootplan = rootplan;
  }
 
  public void init() {
  }

  /**
   * Do something if the test returned true i.e. it was an Allocation
   * to a remote Cluster 
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    if (o.isChange()) {
      Object obj = o.getObject();
      if ((obj instanceof Task)) {
        processTask((Task) obj, changes);
      }
    }
  }

  private void processTask(Task task, Collection changes) {
    PlanElement pe = task.getPlanElement();

    if (pe == null) return;
    
    // MAJOR HACK BY MIK AND BETH!!!!!  HACK HACK HACK 
    // Fixes a problem in multi-vm societies where the planelement
    // doesn't get re-matched up.  This code gets called when changes
    // are made to a task (preference changes)... other plugins and
    // this LP expect this task to already have a pe from the first
    // time around.

    if (! (pe instanceof Allocation)) return;
    Allocation all = (Allocation) pe;

    Asset asset = all.getAsset();
    ClusterPG cpg = asset.getClusterPG();
    if (cpg == null) return;
    MessageAddress destination = cpg.getMessageAddress();
    if (destination == null) return;
    Task senttask = ((AllocationforCollections)pe).getAllocationTask();

    if (senttask != null) {
      if (((TaskImpl)senttask).private_updatePreferences((TaskImpl)task)) {
        // we changed task, so:

        // Give the task to the blackboard for transmission
        rootplan.sendDirective(senttask, changes);
      }
    }
  }
}
        

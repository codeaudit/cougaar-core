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
import org.cougaar.domain.planning.ldm.plan.NewTask;
import org.cougaar.domain.planning.ldm.plan.Preference;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.domain.planning.ldm.plan.AllocationforCollections;
import org.cougaar.domain.planning.ldm.plan.TaskImpl;
import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

/** PreferenceChangeLogicProvider class provides the logic to propogate
 *  preference changes to tasks that have been sent to other clusters
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: PreferenceChangeLP.java,v 1.2 2001-01-03 14:33:06 mthome Exp $
 **/

public class PreferenceChangeLP extends LogPlanLogicProvider implements EnvelopeLogicProvider {
        
  public PreferenceChangeLP(LogPlanServesLogicProvider logplan,
                            ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
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
    ClusterIdentifier destination = cpg.getClusterIdentifier();
    if (destination == null) return;
    Task senttask = ((AllocationforCollections)pe).getAllocationTask();

    if (senttask != null) {
      if (((TaskImpl)senttask).private_updatePreferences((TaskImpl)task)) {
        // we changed task, so:

        // Give the task to the logplan for transmission
        logplan.sendDirective(senttask, changes);
      }
    }
  }
}
        

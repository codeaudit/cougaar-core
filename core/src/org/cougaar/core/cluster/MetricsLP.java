/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;
import org.cougaar.core.cluster.*;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.Workflow;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import java.util.Enumeration;
import java.util.Collection;
import java.util.Iterator;

/** MetricsLP is a "LogPlan Logic Provider":
 *
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version 
 **/

public class MetricsLP
  extends LogPlanLogicProvider
  implements EnvelopeLogicProvider, LogicProviderNeedingPersistenceEnvelopes
{

  public MetricsLP(LogPlanServesLogicProvider logplan,
                   ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }

  /**
   * @param Object Envelope.tuple,
   *          where tuple.object
   *             == PlanElement with an Allocation to an Organization  ADDED to LogPlan
   *
   * If the test returned true, incr/decr 1 from appropriate count in logplan
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    // drop changes
    if (! (o.isAdd() || o.isRemove() || o.isBulk())) return;

    Object obj = o.getObject();

    int inc = 1;
    
    // Removes cannot happen in bulk, so we use -1
    if (o.isRemove()) {
      inc = -1;
    }
    // Adds CAN happen in bulk so check for this and inc each obj in container
    // No helper predicate for bulk type, so have to check explicitly
    if (o.isBulk()) {
      Collection c = (Collection) obj;
      //System.out.println("Got BULK tuple of " + c.size() + " objects");
      for (Iterator e = c.iterator(); e.hasNext(); ) {
        checkAndIncCount(e.next(),inc);
      }
    } else {
      // Normal case - single add or single remove
      checkAndIncCount(obj,inc);
    }
  }

  private void checkAndIncCount(Object obj, int inc) {
    if (obj instanceof Task) {
      logplan.incTaskCount(inc);
    } else if (obj instanceof Workflow) {
      logplan.incWorkflowCount(inc);
    } else if (obj instanceof PlanElement) {
      logplan.incPlanElementCount(inc);
    } else if (obj instanceof Asset) {
      logplan.incAssetCount(inc);
    }
  }

}

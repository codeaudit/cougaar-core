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
import org.cougaar.core.cluster.*;

import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Aggregation;
import org.cougaar.domain.planning.ldm.plan.Composition;
import org.cougaar.domain.planning.ldm.plan.Task;

import java.util.*;

/** Note that this LP is currently "unplugged" from ClusterImpl.
  *  We expect the plugin to publish the new combined task.
  */

public class AggregationPublishLP extends LogPlanLogicProvider implements EnvelopeLogicProvider
{
  public AggregationPublishLP(LogPlanServesLogicProvider logplan,
                              ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }

  /** @param Object  Object Envelope.tuple
   *        where Envelope.Tuple.object
   *            == PlanElement object ADDED TO LOGPLAN containing Aggregation
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    // drop changes
    Object obj;
    if (o.isAdd() &&
        (obj = o.getObject()) != null &&
        obj instanceof Aggregation) {
      Aggregation agg = (Aggregation)obj;
      Task ctask = agg.getComposition().getCombinedTask();
      logplan.add(ctask);
    }
  }
}

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

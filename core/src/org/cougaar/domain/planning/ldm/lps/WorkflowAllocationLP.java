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
import org.cougaar.domain.planning.ldm.plan.Expansion;
import org.cougaar.domain.planning.ldm.plan.Workflow;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.NewTask;
import org.cougaar.core.cluster.ClusterMessage;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.DirectiveMessage;
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;

import java.util.*;

public class WorkflowAllocationLP extends LogPlanLogicProvider implements EnvelopeLogicProvider
{
  public WorkflowAllocationLP(LogPlanServesLogicProvider logplan,
                              ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }


  /** @param Object  Object Envelope.tuple
   *        where Envelope.Tuple.object
   *            == PlanElement object ADDED TO LOGPLAN containing Expansion
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    // drop changes
    if (o.isAdd()) {
      Object obj = o.getObject();
      if ((obj instanceof Expansion)) {
        processExpansion((Expansion) obj);
      }
    }
  }

  private void processExpansion(Expansion exp) {
    Workflow work = exp.getWorkflow();
    logplan.add(work);

      // Add each subtask of the workflow to the logplan so
      // that allocators can allocate against single tasks
      // (and we don't need pass-thru expanders
    Enumeration tasks = work.getTasks();
    while (tasks.hasMoreElements()) {
      Task t = (Task) tasks.nextElement();
      if (t != null) {
        if (logplan.findTask(t) == null)
          logplan.add(t);
      }
    }
  }
}

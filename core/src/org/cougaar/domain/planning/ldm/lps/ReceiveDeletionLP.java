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

import java.util.Collection;
import org.cougaar.core.cluster.ClusterServesLogicProvider;
import org.cougaar.core.cluster.LogPlanLogicProvider;
import org.cougaar.core.cluster.LogPlanServesLogicProvider;
import org.cougaar.core.cluster.MessageLogicProvider;
import org.cougaar.core.society.UID;
import org.cougaar.domain.planning.ldm.plan.AllocationforCollections;
import org.cougaar.domain.planning.ldm.plan.Deletion;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.NewTask;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;

/**
 * Sample LogicProvider for use by ClusterDispatcher to
 * take an incoming Deletion Directive and
 * perform Modification to the LOGPLAN
 **/

public class ReceiveDeletionLP
  extends LogPlanLogicProvider
  implements MessageLogicProvider
{
  public ReceiveDeletionLP(LogPlanServesLogicProvider logplan,
                           ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
  }

  /**
   *  perform updates -- per Deletion ALGORITHM --
   *
   **/
  public void execute(Directive dir, Collection changes) {
    if (dir instanceof Deletion) {
      processDeletion((Deletion) dir);
    }
  }
     
  private void processDeletion(Deletion del) {
    UID tuid = del.getTaskUID();
    PlanElement pe = logplan.findPlanElement(tuid);
    if (pe == null) {
      // Must have been rescinded, nothing to do
    } else {
      NewTask remoteTask = (NewTask) ((AllocationforCollections) pe).getAllocationTask();
      remoteTask.setDeleted(true); // The remote allocation is now a candidate for deletion
    }
  }
}

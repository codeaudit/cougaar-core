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

import org.cougaar.domain.planning.ldm.plan.NewDeletion;
import org.cougaar.domain.planning.ldm.plan.Task;

import org.cougaar.core.society.UID;

import org.cougaar.util.UnaryPredicate;

import java.util.Enumeration;
import java.util.Collection;

/** RescindLogicProvider class provides the logic to capture 
 * rescinded PlanElements (removed from collection)
  *
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: DeletionLP.java,v 1.3 2001-04-05 19:27:10 mthome Exp $
  **/

public class DeletionLP
  extends LogPlanLogicProvider
  implements EnvelopeLogicProvider
{
  private ClusterIdentifier cid;

  public DeletionLP(LogPlanServesLogicProvider logplan,
                    ClusterServesLogicProvider cluster) {
    super(logplan,cluster);
    cid = cluster.getClusterIdentifier();
  }

  /**
   *  @param Object an Envelope.Tuple.object is an ADDED 
   * PlanElement which contains an Allocation to an Organization.
   * Do something if the test returned true i.e. it was an Allocation
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    if (o.isRemove()) {
      Object obj = o.getObject();
      if (obj instanceof Task) {
        Task task = (Task) obj;
        if (task.isDeleted()) {
          UID ptuid = task.getParentTaskUID();
          if (ptuid != null) {
            ClusterIdentifier dst = task.getSource();
            if (!dst.equals(cid)) {
              NewDeletion nd = ldmf.newDeletion();
              nd.setTaskUID(ptuid);
              nd.setPlan(task.getPlan());
              nd.setSource(cid);
              nd.setDestination(dst);
//                System.out.println(cid + ": sendDeletion to " + dst + " for task " + ptuid);
              logplan.sendDirective(nd);
            }
          }
        }
      }
    }
  }
}

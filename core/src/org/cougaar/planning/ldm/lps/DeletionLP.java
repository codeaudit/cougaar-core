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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.*;
import org.cougaar.core.blackboard.*;

import org.cougaar.core.agent.*;

import org.cougaar.core.domain.*;

import org.cougaar.planning.ldm.plan.NewDeletion;
import org.cougaar.planning.ldm.plan.Task;

import org.cougaar.core.util.UID;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

import java.util.Enumeration;
import java.util.Collection;


/** RescindLogicProvider class provides the logic to capture 
 * rescinded PlanElements (removed from collection)
  *
  * @author  ALPINE <alpine-software@bbn.com>
  *
  **/

public class DeletionLP
implements LogicProvider, EnvelopeLogicProvider
{
  private static final Logger logger = Logging.getLogger(DeletionLP.class);

  private final RootPlan rootplan;
  private final PlanningFactory ldmf;
  private final MessageAddress self;

  public DeletionLP(
      RootPlan rootplan,
      PlanningFactory ldmf,
      MessageAddress self) {
    this.rootplan = rootplan;
    this.ldmf = ldmf;
    this.self = self;
  }

  public void init() {
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
            MessageAddress dst = task.getSource();
            if (!dst.equals(self)) {
              NewDeletion nd = ldmf.newDeletion();
              nd.setTaskUID(ptuid);
              nd.setPlan(task.getPlan());
              nd.setSource(self);
              nd.setDestination(dst);
	      if (logger.isDebugEnabled()) {
		logger.debug(self + ": sendDeletion to " + dst + " for task " + ptuid);
	      }

              rootplan.sendDirective(nd);
            }
          }
        }
      }
    }
  }
}

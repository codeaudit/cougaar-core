/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.blackboard;

import org.cougaar.core.cluster.Distributor;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;

/** A BlackboardMetricsService is an API which may be supplied by a 
 * ServiceProvider registered in a ServiceBroker that provides metrics for
 * the entire Blackboard.
 */

public class BlackboardMetricsServiceImpl implements BlackboardMetricsService {
  
  private Distributor distributor;

  public BlackboardMetricsServiceImpl(Distributor dist) {
    this.distributor = dist;
  }

  /** Get a count of objects currently in the Blackboard.
   * @param predicate Specify the objects to count in the Blackboard.
   * @return int The count of objects that match the predicate.
   **/
  public int getBlackboardCount(UnaryPredicate predicate) {
    return distributor.getBlackboardCount(predicate);
  }

  /** @return int A count of the Asset's currently found in the Blackboard **/
  public int getAssetCount() {
    return distributor.getBlackboardCount(assetpred);
  }

  /** @return int A count of the PlanElements currently found in the Blackbaord **/
  public int getPlanElementCount() {
    return distributor.getBlackboardCount(pepred);
  }

  /** @return int A count of the Tasks currently found in the Blackbaord **/
  public int getTaskCount() {
    return distributor.getBlackboardCount(taskpred);
  }

  /** @return int A count of the total number of objects currently 
   * found in the Blackbaord **/
  public int getBlackboardObjectCount() {
    return distributor.getBlackboardSize();
  }

  // various UnaryPredicates
  private final UnaryPredicate taskpred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        return true;
      } else {
        return false;
      }
    }
  };

  private final UnaryPredicate pepred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof PlanElement) {
        return true;
      } else {
        return false;
      }
    }
  };

  private final UnaryPredicate assetpred = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Asset) {
        return true;
      } else {
        return false;
      }
    }
  };

}

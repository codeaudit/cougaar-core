/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.blackboard;

import org.cougaar.core.component.Service;
import org.cougaar.util.UnaryPredicate;

/** A BlackboardMetricsService is an API which may be supplied by a 
 * ServiceProvider registered in a ServiceBroker that provides metrics for
 * the entire Blackboard.
 */

public interface BlackboardMetricsService extends Service {
  
  /** Get a count of objects currently in the Blackboard.
   * @param predicate Specify the objects to count in the Blackboard.
   * @return int The count of objects that match the predicate.
   **/
  int getBlackboardCount(UnaryPredicate predicate);

  /** @return int A count of the Asset's currently found in the Blackboard **/
  int getAssetCount();

  /** @return int A count of the PlanElements currently found in the Blackbaord **/
  int getPlanElementCount();

  /** @return int A count of the Tasks currently found in the Blackbaord **/
  int getTaskCount();

}


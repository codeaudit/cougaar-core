/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.core.cluster;

public interface ActiveSubscriptionObject {
  /** called by Subscriber.publishAdd().  
   * @return false iff the change is vetoed.
   **/
  boolean addingToLogPlan(Subscriber subscriber);
  /** called by Subscriber.publishChange().  
   * @return false iff the change is vetoed.
   **/
  boolean changingInLogPlan(Subscriber subscriber);
  /** called by Subscriber.publishRemove().  
   * @return false iff the change is vetoed.
   **/
  boolean removingFromLogPlan(Subscriber subscriber);
}

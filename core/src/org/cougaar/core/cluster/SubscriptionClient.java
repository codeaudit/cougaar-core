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

public interface SubscriptionClient {
  /**
   * Return a name for this SubscriptionClient. All clients in a
   * cluster should have distinct names.
   */
  public String getSubscriptionClientName();

  long currentTimeMillis( );

  /** Accept an event from an EventSubscription.
   * @param event The event to be accepted.
   * @return true IFF the event is actually accepted.
   **/
  boolean triggerEvent(Object event);

  static class Local extends ThreadLocal {
    public SubscriptionClient getClient() {
      return (SubscriptionClient) get();
    }
  }

  public static final Local current = new Local();
}

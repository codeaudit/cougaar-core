/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
/**
 * The public interface for persistence
 */
package org.cougaar.core.cluster.persist;

public interface Persistence {
  /**
   * End a persistence epoch by generating a persistence delta
   * @param undistributedEnvelopes Envelopes that the distribute is about to distribute
   * @param epochEnvelopes All envelopes from this epoch
   * @param subscriberStates The subscriber states to record
   **/
  void persist(java.util.List undistributedEnvelopes,
               java.util.List allEpochEnvelopes,
               java.util.List subscriberStates);

  /**
   * Get the rehydration envelope from the most recent persisted state.
   * @return null if there is no persisted state.
   **/
  java.util.List rehydrate(org.cougaar.core.cluster.PersistenceEnvelope oldObjects);
  org.cougaar.domain.planning.ldm.plan.Plan getRealityPlan();
  void setRealityPlan(org.cougaar.domain.planning.ldm.plan.Plan reality);
  org.cougaar.core.cluster.MessageManager getMessageManager();
  PersistenceSubscriberState getSubscriberState(org.cougaar.core.cluster.Subscriber subscriber);
  boolean hasSubscriberStates();
  void discardSubscriberState(org.cougaar.core.cluster.Subscriber subscriber);
  void disableWrite();
}

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

import org.cougaar.core.cluster.MessageManager;
import org.cougaar.core.cluster.Subscriber;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.core.cluster.PersistenceEnvelope;
import java.util.List;

public interface Persistence {
  /**
   * End a persistence epoch by generating a persistence delta
   * @param undistributedEnvelopes Envelopes that the distribute is about to distribute
   * @param epochEnvelopes All envelopes from this epoch
   * @param subscriberStates The subscriber states to record
   **/
    Object persist(List undistributedEnvelopes,
                   List allEpochEnvelopes,
                   List subscriberStates,
                   boolean returnBytes,
                   MessageManager messageManager);

  /**
   * Get the rehydration envelope from the most recent persisted state.
   * @return null if there is no persisted state.
   **/
  RehydrationResult rehydrate(PersistenceEnvelope oldObjects, Object state);
  Plan getRealityPlan();
  void setRealityPlan(Plan reality);
  PersistenceSubscriberState getSubscriberState(Subscriber subscriber);
  boolean hasSubscriberStates();
  void discardSubscriberState(Subscriber subscriber);
  void disableWrite();
}

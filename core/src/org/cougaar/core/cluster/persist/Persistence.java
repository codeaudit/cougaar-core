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

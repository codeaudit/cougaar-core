/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.persist;

import java.util.List;
import java.util.Set;

import org.cougaar.core.blackboard.MessageManager;
import org.cougaar.core.blackboard.PersistenceEnvelope;
import org.cougaar.core.blackboard.Subscriber;

/**
 * The public interface for persistence. This is the old interface to
 * persistence and is still used for blackboard persistence.
 * The implementation recasts these methods into the new
 * PersistenceService interface.
 */
public interface Persistence {
  /**
   * End a persistence epoch by generating a persistence delta
   * @param undistributedEnvelopes Envelopes that the distribute is about to distribute
   * @param allEpochEnvelopes All envelopes from this epoch
   * @param subscriberStates The subscriber states to record
   **/
    PersistenceObject persist(List undistributedEnvelopes,
                              List allEpochEnvelopes,
                              List subscriberStates,
                              boolean returnBytes,
                              boolean full,
                              MessageManager messageManager,
                              Object quiescenceMonitorState);

    /**
     * Get the rehydration envelope from the most recent persisted state.
     * @return null if there is no persisted state.
     **/
    RehydrationResult rehydrate(PersistenceEnvelope oldObjects, Object state);
    PersistenceSubscriberState getSubscriberState(Subscriber subscriber);
    boolean hasSubscriberStates();
    void discardSubscriberState(Subscriber subscriber);

  /**
   * Get a set of the Keys of the SubscriberStates in the rehydration info.
   * Used by the Distributor to track which subscribers have not
   * rehydrated.
   **/
  public Set getSubscriberStateKeys();

    java.sql.Connection getDatabaseConnection(Object locker);
    void releaseDatabaseConnection(Object locker);
    long getPersistenceTime();
}

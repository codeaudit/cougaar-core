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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.cougaar.core.blackboard.Envelope;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.MessageManager;
import org.cougaar.core.blackboard.PersistenceEnvelope;
import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;

public class BlackboardPersistence implements Persistence {
  private PersistenceServiceForBlackboard persistenceService;
  private LoggingService logger;
  private MessageAddress agentId;
  private PersistenceIdentity clientId = new PersistenceIdentity(getClass().getName());
  private List clientData;

  private List rehydrationSubscriberStates = null;

  private Object rehydrationSubscriberStatesLock = new Object();

  private PersistenceClient persistenceClient =
    new PersistenceClient() {
      public PersistenceIdentity getPersistenceIdentity() {
        return clientId;
      }
      public List getPersistenceData() {
        if (clientData == null) {
          logger.error("Persistence not initiated by blackboard");
          return Collections.EMPTY_LIST;
        }
        return clientData;
      }
    };

  private static class MyMetaData implements Serializable {
    List undistributedEnvelopes;
    List subscriberStates;
    MessageManager messageManager;
    Object quiescenceMonitorState;
  }

  public static Persistence find(ServiceBroker sb)
    throws PersistenceException
  {
    return new BlackboardPersistence(sb);
  }

  private BlackboardPersistence(ServiceBroker sb) {
    AgentIdentificationService agentIdService = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    agentId = (MessageAddress) agentIdService.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, agentIdService);
    logger = (LoggingService) sb.getService(this, LoggingService.class, null);
    persistenceService = (PersistenceServiceForBlackboard)
      sb.getService(persistenceClient, PersistenceServiceForBlackboard.class, null);
  }

  /**
   * Keeps all associations of objects that have been persisted.
   **/
 
  /**
   * Gets the system time when persistence should be performed. We do
   * persistence periodically with a period such that all the plugins
   * will, on the average create persistence deltas with their
   * individual periods. The average frequence of persistence is the
   * sum of the individual media frequencies. Frequency is the
   * reciprocal of period. The computation is:<p>
   *
   * &nbsp;&nbsp;T = 1/(1/T1 + 1/T2 + ... + 1/Tn)
   * <p>
   * @return the time of the next persistence delta
   **/
  public long getPersistenceTime() {
    return persistenceService.getPersistenceTime();
  }

  /**
   * Rehydrate a persisted cluster. Reads all the deltas in
   * order keeping the latest (last encountered) values from
   * every object.
   * @param oldObjects Changes recorded in all but the last delta.
   * @param newObjects Changes recorded in the last delta
   * @return List of all envelopes that have not yet been distributed
   */
  public RehydrationResult rehydrate(PersistenceEnvelope oldObjects, Object state) {
    RehydrationData rehydrationData = persistenceService.getRehydrationData();
    RehydrationResult result = new RehydrationResult();
    if (rehydrationData != null) {
      for (Iterator i = rehydrationData.getPersistenceEnvelope().getAllTuples(); i.hasNext(); ) {
        oldObjects.addTuple((EnvelopeTuple) i.next());
      }
      List list = rehydrationData.getObjects();
      assert list.size() == 1;  // We published a bunch of envelopes and a MyMetaData
      MyMetaData meta = (MyMetaData) list.get(0);
      result.messageManager = meta.messageManager;
      result.undistributedEnvelopes= meta.undistributedEnvelopes;
      result.quiescenceMonitorState = meta.quiescenceMonitorState;
      rehydrationSubscriberStates = meta.subscriberStates;
    }
    return result;
  }

  public boolean hasSubscriberStates() {
    synchronized (rehydrationSubscriberStatesLock) {
      return rehydrationSubscriberStates != null;
    }
  }

  public void discardSubscriberState(Subscriber subscriber) {
    synchronized (rehydrationSubscriberStatesLock) {
      if (rehydrationSubscriberStates != null) {
        for (Iterator subscribers = rehydrationSubscriberStates.iterator();
             subscribers.hasNext(); )
          {
            PersistenceSubscriberState pSubscriber =
              (PersistenceSubscriberState) subscribers.next();
            if (pSubscriber.isSameSubscriberAs(subscriber)) {
              subscribers.remove();
              if (rehydrationSubscriberStates.size() == 0) {
                rehydrationSubscriberStates = null;
              }
              return;
            }
          }
      }
    }
  }

  public PersistenceSubscriberState getSubscriberState(Subscriber subscriber) {
    synchronized (rehydrationSubscriberStatesLock) {
      if (rehydrationSubscriberStates != null) {
        for (Iterator subscribers = rehydrationSubscriberStates.iterator(); subscribers.hasNext(); ) {
          PersistenceSubscriberState pSubscriber = (PersistenceSubscriberState) subscribers.next();
          if (pSubscriber.isSameSubscriberAs(subscriber)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Found " + pSubscriber);
            }
            return pSubscriber;
          }
        }
        if (subscriber.shouldBePersisted() && logger.isInfoEnabled()) {
          logger.info("Failed to find " + new PersistenceSubscriberState(subscriber));
        }
        return null;
      }
      return null;
    }
  }

  private boolean isPersistable(Object o) {
    if (o instanceof NotPersistable) return false;
    if (o instanceof Persistable) {
      Persistable pbl = (Persistable) o;
      return pbl.isPersistable();
    }
    return true;
  }

  private ArrayList copyAndRemoveNotPersistable(List v) {
    if (v == null) return null;
    ArrayList result = new ArrayList(v.size());
    for (Iterator iter = v.iterator(); iter.hasNext(); ) {
      Envelope e = (Envelope) iter.next();
      Envelope copy = null;
      for (Iterator tuples = e.getAllTuples(); tuples.hasNext(); ) {
        EnvelopeTuple tuple = (EnvelopeTuple) tuples.next();
        Object o = tuple.getObject();
        if (isPersistable(o)) {
          if (copy == null) copy = new Envelope();
          copy.addTuple(tuple);
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("Removing not persistable " + o);
          }
        }
      }
      if (copy != null) result.add(copy);
    }
    return result;
  }

  /**
   * End a persistence epoch by generating a persistence delta.
   * @param epochEnvelopes All envelopes from this epoch that have
   * been distributed. The effect of these envelopes has already been
   * captured in the subscriber inboxes or in the consequential
   * outboxes which are in undistributedEnvelopes.
   * @param undistributedEnvelopes Envelopes that have not yet been distributed
   * @param subscriberStates The subscriber states to record
   **/
  public PersistenceObject persist(List epochEnvelopes,
                                   List undistributedEnvelopes,
                                   List subscriberStates,
                                   boolean returnBytes,
                                   boolean full,
                                   MessageManager messageManager,
                                   Object quiescenceMonitorState)
  {
    MyMetaData meta = new MyMetaData();
    meta.undistributedEnvelopes = copyAndRemoveNotPersistable(undistributedEnvelopes);
    for (Iterator iter = subscriberStates.iterator(); iter.hasNext(); ) {
      PersistenceSubscriberState ss = (PersistenceSubscriberState) iter.next();
      ss.pendingEnvelopes = copyAndRemoveNotPersistable(ss.pendingEnvelopes);
      ss.transactionEnvelopes = copyAndRemoveNotPersistable(ss.transactionEnvelopes);
    }
    meta.subscriberStates= subscriberStates;
    meta.messageManager = messageManager;
    meta.quiescenceMonitorState = quiescenceMonitorState;
    clientData = new ArrayList(2);
    clientData.addAll(copyAndRemoveNotPersistable(epochEnvelopes));
    clientData.add(meta);
    epochEnvelopes.clear();     // Allow gc
    PersistenceObject result = persistenceService.persist(returnBytes, full);
    clientData.clear();
    return result;
  }

  public List getPersistenceData() {
    List result = clientData;
    clientData = null;
    return result;
  }

  public java.sql.Connection getDatabaseConnection(Object locker) {
    return persistenceService.getDatabaseConnection(locker);
  }

  public void releaseDatabaseConnection(Object locker) {
    persistenceService.releaseDatabaseConnection(locker);
  }

  // More Persistence implementation
  public void registerServices(ServiceBroker sb) {
  }

  public void unregisterServices(ServiceBroker sb) {
  }

  public String toString() {
    return "Persist(" + agentId + ")";
  }
}

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

package org.cougaar.planning.plugin.legacy;

import java.util.Collection;
import java.util.Date;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.ClusterServesPlugin;
import org.cougaar.core.agent.MetricsSnapshot;
import org.cougaar.core.blackboard.SubscriberException;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.domain.Factory;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;

/** 
 * An interface for getting at the (normally) protected Plan API 
 * methods of a Plugin.  Essentially all the of the protected Plan 
 * API methods of PluginAdapter can be accessed via these public
 * methods.
 * @see PluginAdapter#getDelegate()
 **/

public interface PluginDelegate {
  BlackboardService getBlackboardService();
  /** Alias for getBlackboardService() **/
  BlackboardService getSubscriber();
  ClusterServesPlugin getCluster();
  LDMServesPlugin getLDM();
  PlanningFactory getFactory();
  Factory getFactory(String domainname);
  MessageAddress getMessageAddress();
  /** @deprecated use getMetricsSnapshot(MetricsSnapshot ms, boolean resetMsgStats) **/
  MetricsSnapshot getMetricsSnapshot();
  MetricsSnapshot getMetricsSnapshot(MetricsSnapshot ms, boolean resetMsgStats);
  void openTransaction();
  boolean tryOpenTransaction();
  void closeTransaction() throws SubscriberException;
  void closeTransactionDontReset() throws SubscriberException ;
  /** @deprecated Use {@link #closeTransactionDontReset closeTransactionDontReset}
   **/
  void closeTransaction(boolean resetp) throws SubscriberException ;
  boolean wasAwakened();
  void wake();
  long currentTimeMillis();
  Date getDate();
  Subscription subscribe(UnaryPredicate isMember);
  Subscription subscribe(UnaryPredicate isMember, Collection realCollection);
  Subscription subscribe(UnaryPredicate isMember, boolean isIncremental);
  Subscription subscribe(UnaryPredicate isMember, Collection realCollection, boolean isIncremental);
  void unsubscribe(Subscription collection);
  Collection query(UnaryPredicate isMember);
  void publishAdd(Object o);
  void publishRemove(Object o);
  void publishChange(Object o);
  void publishChange(Object o, Collection changes);
  Collection getParameters();
  boolean didRehydrate();
  ServiceBroker getServiceBroker();

  /** Attempt to stake a claim on a logplan object, essentially telling 
   * everyone else that you and only you will be disposing, modifying, etc.
   * it.
   * Calls Claimable.tryClaim if the object is Claimable.
   * @return true IFF success.
   **/
  boolean claim(Object o);

  /** Release an existing claim on a logplan object.  This is likely to
   * thow an exception if the object had not previously been (successfully) 
   * claimed by this plugin.
   **/
  void unclaim(Object o);
}

/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import org.cougaar.core.blackboard.BlackboardService;
import org.cougaar.core.cluster.Distributor;
import org.cougaar.core.cluster.ClusterServesPlugIn;
import org.cougaar.core.cluster.SubscriberException;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.MetricsSnapshot;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.domain.planning.ldm.RootFactory;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.MessageTransportService;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.util.UnaryPredicate;
import java.util.*; 

/** 
 * An interface for getting at the (normally) protected Plan API 
 * methods of a PlugIn.  Essentially all the of the protected Plan 
 * API methods of PlugInAdapter can be accessed via these public
 * methods.
 * @see org.cougaar.core.plugin.PlugInAdapter.getDelegate()
 **/

public interface PlugInDelegate {
  BlackboardService getBlackboardService();
  /** Alias for getBlackboardService() **/
  BlackboardService getSubscriber();
  Distributor getDistributor();
  ClusterServesPlugIn getCluster();
  LDMServesPlugIn getLDM();
  RootFactory getFactory();
  /** @deprecated Use getFactory() **/
  RootFactory getLdmFactory();
  Factory getFactory(String domainname);
  ClusterIdentifier getClusterIdentifier();
  /** @deprecated use getMetricsSnapshot(MetricsSnapshot ms, boolean resetMsgStats) **/
  MetricsSnapshot getMetricsSnapshot();
  MetricsSnapshot getMetricsSnapshot(MetricsSnapshot ms, boolean resetMsgStats);
  void openTransaction();
  boolean tryOpenTransaction();
  void closeTransaction() throws SubscriberException;
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

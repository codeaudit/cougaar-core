/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.blackboard.SubscriberException;
import org.cougaar.core.blackboard.SubscriptionWatcher;
import org.cougaar.core.persist.PersistenceNotEnabledException;
import org.cougaar.core.persist.Persistence;
import org.cougaar.util.UnaryPredicate;


import java.util.*;

/** A BlackboardService is an API which may be supplied by a 
 * ServiceProvider registered in a ServiceBroker that provides basic
 * blackboard publish, subscription and transaction services.
 **/
public interface BlackboardService extends Service {
  //
  //Subscriber/ Subscription stuff
  //

  Subscriber getSubscriber();
    
  /** Request a subscription to all objects for which 
   * isMember.execute(object) is true.  The returned Collection
   * is a transactionally-safe set of these objects which is
   * guaranteed not to change out from under you during run()
   * execution.
   * 
   * subscribe() may be called any time after 
   * load() completes.
   **/
  Subscription subscribe(UnaryPredicate isMember);

  /** like subscribe(UnaryPredicate), but allows specification of
   * some other type of Collection object as the internal representation
   * of the collection.
   * Alias for getSubscriber().subscribe(UnaryPredicate, Collection);
   **/
  Subscription subscribe(UnaryPredicate isMember, Collection realCollection);

  /**
   * Alias for getSubscriber().subscribe(UnaryPredicate, boolean);
   **/
  Subscription subscribe(UnaryPredicate isMember, boolean isIncremental);
  
  /**
   * Alias for <code>getSubscriber().subscribe(UnaryPredicate, Collection, boolean);</code>
   * @param isMember a predicate to execute to ascertain
   * membership in the collection of the subscription.
   * @param realCollection a container to hold the contents of the subscription.
   * @param isIncremental should be true if an incremental subscription is desired.
   * An incremental subscription provides access to the incremental changes to the subscription.
   * @return the Subsciption.
   * @see org.cougaar.core.blackboard.Subscriber#subscribe
   * @see org.cougaar.core.blackboard.Subscription
   **/
  Subscription subscribe(UnaryPredicate isMember, Collection realCollection, boolean isIncremental);

  /** Issue a query against the logplan.  Similar in function to
   * opening a new subscription, getting the results and immediately
   * closing the subscription, but can be implemented much more efficiently.
   * Note: the initial implementation actually does exactly this.
   **/
  Collection query(UnaryPredicate isMember);

  /**
   * Cancels the given Subscription which must have been returned by a
   * previous invocation of subscribe().  Alias for
   * <code> getSubscriber().unsubscribe(Subscription)</code>.
   * @param subscription the subscription to cancel
   * @see org.cougaar.core.blackboard.Subscriber#unsubscribe
   **/
  void unsubscribe(Subscription subscription);
  
  int getSubscriptionCount();
  
  int getSubscriptionSize();

  int getPublishAddedCount();

  int getPublishChangedCount();

  int getPublishRemovedCount();
  
  /** @return true iff collection contents have changed since the last 
   * transaction.
   **/
  boolean haveCollectionsChanged();

  //
  // LogPlan changes publishing
  //

  boolean publishAdd(Object o);
  
  boolean publishRemove(Object o);

  boolean publishChange(Object o);
  
  /** mark an element of the Plan as changed.
   * Behavior is not defined if the object is not a member of the plan.
   * There is no need to call this if the object was added or removed,
   * only if the contents of the object itself has been changed.
   * The changes parameter describes a set of changes made to the
   * object beyond those tracked automatically by the object class
   * (see the object class documentation for a description of which
   * types of changes are tracked).  Any additional changes are
   * merged in <em>after</em> automatically collected reports.
   * @param changes a set of ChangeReport instances or null.
   **/
  boolean publishChange(Object o, Collection changes); 

  // 
  // aliases for Transaction handling 
  //

  void openTransaction();

  boolean tryOpenTransaction();

  void closeTransaction() throws SubscriberException;
    
  void closeTransaction(boolean resetp) throws SubscriberException;


  //
  // ScheduleablePlugin API 
  //

  /** called when the client (Plugin) requests that it be waked again.
   * by default, just calls wakeSubscriptionWatchers, but subclasses
   * may be more circumspect.
   **/
  void signalClientActivity();

  /** register a watcher of subscription activity **/
  SubscriptionWatcher registerInterest(SubscriptionWatcher w);

  /** register a watcher of subscription activity **/
  SubscriptionWatcher registerInterest();

  /** stop watching subscription activity **/
  void unregisterInterest(SubscriptionWatcher w) throws SubscriberException;

  //
  // persistence hooks
  //

  /** indicate that this blackboard service information should (or should not)
   * be persisted.
   **/
  void setShouldBePersisted(boolean value);
  /** @return the current value of the persistence setting **/
  boolean shouldBePersisted();

//    /** indicate that the blackboard view is ready to persist **/
//    void setReadyToPersist();

  /** is this BlackboardService the result of a rehydration of a persistence 
   * snapshot? 
   **/
  boolean didRehydrate();

  /**
   * Take a persistence snapshot now. If called from inside a
   * transaction (the usual case for a plugin), the transaction will
   * be closed and reopened. This means that a plugin must first
   * process all of its existing envelopes before calling
   * <code>persistNow()</code> and then process a potential new set of
   * envelopes after re-opening the transaction. Otherwise, the
   * changes will be lost.
   * @exception PersistenceNotEnabledException
   **/
  void persistNow() throws PersistenceNotEnabledException;

  /** Hook to allow access to Blackboard persistence mechanism **/
  Persistence getPersistence();
}

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

package org.cougaar.core.blackboard;

import org.cougaar.core.agent.*;

import org.cougaar.util.UnaryPredicate;
import java.util.*;

/** 
 * Represent a view of a Plan with in three parts, (1) a description
 * of the view (predicate, etc), (2) a collection that
 * represents the slice of the plan and (3) an api to be used to
 * mutate the plan.
 *
 * The Subscription is described by the getPredicate() and getSubscriber()
 * methods.
 *
 * The mutation API allows consumers to add, remove and mark objects as changed
 * in the Plan.  The referenced objects might or might not be members of
 * this subscription.
 **/

public abstract class Subscription {

  /** our Subscriber and interface to the plan **/
  protected Subscriber subscriber = null;

  /**
   * set the Subscriber instance for the subscription.  Should only be
   * done by Subscriber.subscribe(), and will throw a RuntimeException
   * if called more than once.
   **/
  final void setSubscriber(Subscriber s) {
    if (subscriber != null) {
      throw new RuntimeException("Attempt to reset the Subscriber of " +
                                 this +
                                 " to " +
                                 s +
                                 " from " +
                                 subscriber);
    }
    subscriber = s;
  }

  /** @return the Subscriber instance which is the interface to the plugin.
   **/
  public final Subscriber getSubscriber() { return subscriber; }


  /** The predicate that represents this subscription **/
  protected final UnaryPredicate predicate;

  public Subscription(UnaryPredicate p) {
    predicate = p;
  }

  /**
   * Decide if the object is applicable to the subscription
   * and make the appropriate changes. 
   * Called by Envelope.wrapperAdd to add an object to the
   * subscription view.
   * @param isVisible If FALSE will make the change quietly, e.g. after
   * rehydration from persistence plugin.
   * @return true IFF the subscription was changed as a result of the call.
   **/
  boolean conditionalAdd(Object o, boolean isVisible) { 
    if (predicate.execute(o)) {
      privateAdd(o, isVisible);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Decide if the object is applicable to the subscription
   * and make the appropriate changes. 
   * Called by Envelope.wrapperAdd to remove an object in the
   * subscription view.
   * @param isVisible If FALSE will make the change quietly, e.g. after
   * rehydration from persistence plugin.
   * @return true IFF the subscription was changed as a result of the call.
   **/
  boolean conditionalRemove(Object o, boolean isVisible) {
    if (predicate.execute(o)) {
      privateRemove(o, isVisible);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Decide if the object is applicable to the subscription
   * and make the appropriate changes. 
   * Called by Envelope.wrapperAdd to mark as changed an object in the
   * subscription view.
   * @param changes a List of ChangeReport instances describing the changes
   * made to the object.  May be null.
   * @param isVisible If FALSE will make the change quietly, e.g. after
   * rehydration from persistence plugin.
   * @return true IFF the subscription was changed as a result of the call.
   **/
  boolean conditionalChange(Object o, List changes, boolean isVisible) {
    if (predicate.execute(o)) {
      privateChange(o, changes, isVisible);
      return true;
    } else {
      return false;
    }
  }


  abstract protected void privateAdd(Object o, boolean isVisible);
  abstract protected void privateRemove(Object o, boolean isVisible);
  abstract protected void privateChange(Object o, List changes, boolean isVisible);

  //
  // Want to move them down to a SubscriptionWithDeltas interface
  //

  // Change sets
  protected boolean myHasChanged = false;

  /**
   * Has the Subscription changed?  To be precise, this indicates
   * if there were any visible changes to the subscription contents
   * in the interval between the current and the previous calls to
   * Subscriber.openTransaction()
   **/
  public final boolean hasChanged() { 
    subscriber.checkTransactionOK("hasChanged()");
    return myHasChanged; 
  }

  // changed to package-protected
  protected final void setChanged( boolean changed ) { 
    myHasChanged = changed; 
  }

  /** Called by Subscriber's transaction system to update the
   * changes (and delta lists, if applicable) tracking.
   * @see hasChanged()
   **/
  protected void resetChanges() { setChanged(false); }

  public void fill(Envelope envelope) {
    if (envelope.applyToSubscription(this)) {
      setChanged(true);
      getSubscriber().signalExternalActivity();
    }
  }
}

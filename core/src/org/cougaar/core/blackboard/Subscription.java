/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.blackboard;

import java.util.List;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

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

  private static final Logger _logger = Logging.getLogger(Subscription.class);

  /** Have we recieved our InitializeSubscriptionEnvelope yet?
   * @see #apply(Envelope)
   **/
  private boolean isInitialized = false;

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


    // blackboard needs no delayed fill
    if (subscriber instanceof Blackboard) {
	//_logger.error("Preset InitializeSubscriptionEnvelope for "+this.predicate+" "+this.hashCode(), new Throwable());
      setIsInitialized();
    }
  }

  /** @return the Subscriber instance which is the interface to the plugin.
   **/
  public final Subscriber getSubscriber() { return subscriber; }

  /** Check to see if we're in a transaction for the named purpose if we
   * have a subscription which supports transactions.
   **/
  protected final void checkTransactionOK(String s) {
    if (subscriber != null) {
      subscriber.checkTransactionOK("hasChanged()");
    }
  }

  protected final void subscriberSignalExternalActivity() {
    if (subscriber != null) {
      subscriber.signalExternalActivity();
    }
  }

  /** The predicate that represents this subscription **/
  protected final UnaryPredicate predicate;

  /**
   * @note Although allowed, use of DynamicUnaryPredicate can be extremely expensive
   * and tends to create as many problems as it solves.  When in pedantic mode,
   * warning are emitted when DynamicUnaryPredicate is used. Disable Blackboard.PEDANTIC to quiet
   * such warnings if you are sure you want to do this.
   * @see Blackboard#PEDANTIC
   */
  public Subscription(UnaryPredicate p) {
    if (Blackboard.PEDANTIC && p instanceof org.cougaar.util.DynamicUnaryPredicate) {
      _logger.warn("Performance hit: use of DynamicUnaryPredicate "+p, new Throwable()); 
    }
    if (p == null) throw new IllegalArgumentException("Predicate must be non-null");
    predicate = p;
  }

  public String getName() { return predicate.getClass().getName(); }

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
    checkTransactionOK("hasChanged()");
    return myHasChanged; 
  }

  // changed to package-protected
  protected final void setChanged( boolean changed ) { 
    myHasChanged = changed; 
  }

  /** Called by Subscriber's transaction system to update the
   * changes (and delta lists, if applicable) tracking.
   * @see #hasChanged()
   **/
  protected void resetChanges() { setChanged(false); }

  /** Apply a set of transactional changes to our subscription.
   * Envelopes are ignored until a matching InitializeSubscriptionEnvelope 
   * has been received.
   * @note The real work of applying the envelope to the subscription is accomplished
   * by calling {@link #privateApply(Envelope)}.
   **/
  public boolean apply(Envelope envelope) {
    // if this is an ISE, check to see if it is ours!
    if (envelope instanceof InitializeSubscriptionEnvelope) {
      InitializeSubscriptionEnvelope ise = (InitializeSubscriptionEnvelope) envelope;
      if (ise.getSubscription() == this) {
        if (isInitialized) {
          _logger.error("Received redundant InitializeSubscriptionEnvelope for "+this.predicate);
        } else {
          if (_logger.isDebugEnabled()) {
            _logger.debug("Received InitializeSubscriptionEnvelope for "+this.predicate);
          }
          setIsInitialized();
	}
      }
      return false;             // doesn't actually change the subscription in any case
    } else {
      if (isInitialized) {
        return privateApply(envelope);
      } else {
        if (_logger.isInfoEnabled()) {
          _logger.info("Dropped an envelope for "+this.predicate);
        }
        return false;
      }
    }
  }
  
  final void setIsInitialized() {
    isInitialized = true;
  }

  /** Fill the subscription with the initial contents. **/
  public void fill(Envelope envelope) {
    // logically, just call apply(envelope), but we need to avoid the isInitialized checks.
    if (privateApply(envelope)) {
      setChanged(true);
      subscriberSignalExternalActivity();
    }
  }

  protected final boolean privateApply(Envelope envelope) {
    return envelope.applyToSubscription(this);
  }

}

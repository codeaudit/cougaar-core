/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster.persist;

import org.cougaar.core.cluster.Subscriber;
import org.cougaar.core.cluster.Envelope;
import java.util.List;
import java.util.ArrayList;

public class PersistenceSubscriberState implements java.io.Serializable {
  public String subscriptionClientName;	// The name of the client of the subscriber
  public String subscriberName;		// The name of the subscriber
  public List pendingEnvelopes;
  public List transactionEnvelopes;

  public PersistenceSubscriberState(Subscriber subscriber) {
    subscriptionClientName = subscriber.getClient().getSubscriptionClientName();
    subscriberName = subscriber.getName();
    if (subscriber.shouldBePersisted()) {
      this.pendingEnvelopes = subscriber.getPendingEnvelopes();
      this.transactionEnvelopes = subscriber.getTransactionEnvelopes();
    }
  }

  public boolean isSameSubscriberAs(Subscriber subscriber) {
    if (subscriber.getClient().getSubscriptionClientName().equals(subscriptionClientName) &&
	subscriber.getName().equals(subscriberName)) {
      return true;
    }
    return false;
  }

  public String getKey() {
    return subscriptionClientName + "." + subscriberName;
  }

  public String toString() {
    return super.toString() + " " + getKey();
  }
}

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
import java.util.List;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.blackboard.Subscriber;

public class PersistenceSubscriberState implements java.io.Serializable {
  public String clientName;	// The name of the client of the subscriber
  public String subscriberName;		// The name of the subscriber
  public List pendingEnvelopes;
  public List transactionEnvelopes;

  public PersistenceSubscriberState(Subscriber subscriber) {
    clientName = subscriber.getClient().getBlackboardClientName();
    subscriberName = subscriber.getName();
    if (subscriber.shouldBePersisted()) {
      this.pendingEnvelopes = subscriber.getPendingEnvelopes();
      this.transactionEnvelopes = subscriber.getTransactionEnvelopes();
    }
  }

  public boolean isSameSubscriberAs(Subscriber subscriber) {
    if (subscriber.getClient().getBlackboardClientName().equals(clientName) &&
	subscriber.getName().equals(subscriberName)) {
      return true;
    }
    return false;
  }

  public String getKey() {
    return clientName + "." + subscriberName;
  }

  public String toString() {
    return super.toString() + " " + getKey();
  }
}

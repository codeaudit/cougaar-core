/*
 * <copyright>
 *  Copyright 2002 BBNT Solutions, LLC
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


/** InitializeSubscriptionEnvelope is a special envelope which is
 * sent <em>in band</em> during subscription initialization.
 * Any transaction envelopes received by the client for a newly
 * created subscription prior to this envelope are ignored.  This
 * allows the new subscription contents to be kept transactionally
 * in-sync with the rest of the world.
 **/
public final class InitializeSubscriptionEnvelope extends Envelope {
  private transient Subscription subscription;
  InitializeSubscriptionEnvelope(Subscription subscription) {
    this.subscription = subscription;
  }

  Subscription getSubscription() {
    return subscription;
  }

  public String toString() {
    return "InitializeSubscriptionEnvelope for "+subscription;
  }
}


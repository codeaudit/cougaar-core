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

import java.util.Enumeration;
import java.util.Collection;
import java.util.Iterator;

import org.cougaar.core.blackboard.*;

public class PersistenceSubscriber extends Subscriber
{
  public PersistenceSubscriber(BlackboardClient client, Distributor Distributor) {
    setClientDistributor(client, Distributor);
  }

  /** outgoing transactions are bundled in PersistenceEnvelopes instead 
   * of standard Envelopes. 
   **/
  protected Envelope createEnvelope() {
    return new PersistenceEnvelope();
  }

  /** Expose the Subscriber.bulkAddObject api for PersistenceSubscribers **/
  public EnvelopeTuple bulkAdd(Collection c) {
    return bulkAddObject(c);
  }

  /** Expose the Subscriber.bulkAddObject api for PersistenceSubscribers **/
  public EnvelopeTuple bulkAdd(Enumeration e) {
    return bulkAddObject(e);
  }
}

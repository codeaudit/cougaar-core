/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.core.cluster;

import java.util.Enumeration;
import java.util.Collection;
import java.util.Iterator;

public class PersistenceSubscriber extends Subscriber
{
  public PersistenceSubscriber(SubscriptionClient client, Distributor Distributor) {
    setClientDistributor(client,Distributor);
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

/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;


import java.util.*;
import org.cougaar.util.*;

/** 
 * Adds a real delegate Collection to the Subscription, accessible 
 * via getCollection().
 **/


public class QuerySubscription 
  extends CollectionSubscription
{

  public QuerySubscription(UnaryPredicate p, Collection c) {
    super(p,c);
  }

  public QuerySubscription(UnaryPredicate p) {
    super(p, new ArrayList(5));
  }

  // override Subscription.fill to avoid kicking the subscriber due to
  // query activity.
  public void fill(Envelope envelope) {
    envelope.applyToSubscription(this);
  }
}

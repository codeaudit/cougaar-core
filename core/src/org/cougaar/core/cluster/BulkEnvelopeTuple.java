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
import org.cougaar.util.UnaryPredicate;

/** An EnvelopeTuple indicating that a set of objects have been added to the Plan.
 **/

public final class BulkEnvelopeTuple extends EnvelopeTuple {
  private final Collection bulk;
  public Object getObject() { return bulk; }

  public BulkEnvelopeTuple(Collection o) {
    if (o == null) throw new IllegalArgumentException("Collection is null");
    bulk = o;
  }

  public final int getAction() { return Envelope.BULK; }
  public final boolean isBulk() { return true; }
  public final Collection getCollection() { return bulk; }

  boolean applyToSubscription(Subscription s, boolean isVisible) {
    boolean changedP = false;

    if (bulk instanceof ArrayList) {
      ArrayList a = (ArrayList) bulk;
      int l = a.size();
      for (int i = 0; i < l; i++) {
        Object o = a.get(i);
        if (o == null) continue;
        changedP |=  s.conditionalAdd(o, isVisible);
      }
    } else {
      for (Iterator it = bulk.iterator(); it.hasNext(); ) {
        Object o = it.next();
        if (o == null) continue;
        changedP |=  s.conditionalAdd(o, isVisible);
      }
    }
    return changedP;
  }
}

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

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

/** An EnvelopeTuple indicating that an object has been added to the Plan
 **/

public final class AddEnvelopeTuple extends EnvelopeTuple {
  private final Object object;
  public Object getObject() { return object; }
  public AddEnvelopeTuple(Object o) {
    if (o == null) throw new IllegalArgumentException("Object is null");
    object=o;
  }

  public final int getAction() { return Envelope.ADD; }
  public final boolean isAdd() { return true; }

  boolean applyToSubscription(Subscription s, boolean isVisible) {
    return s.conditionalAdd(object,isVisible);
  }
}

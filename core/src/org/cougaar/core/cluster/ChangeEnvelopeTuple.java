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

/** An EnvelopeTuple indicating that an object in the Plan has been modified.
 **/

public final class ChangeEnvelopeTuple extends EnvelopeTuple {
  private final Object object;
  public Object getObject() { return object; }
  private final List changes;

  // perhaps at some point we should complain if we aren't told what the
  // changes are...
  public ChangeEnvelopeTuple(Object o, List changes) {
    if (o == null) throw new IllegalArgumentException("Object is null");
    object = o;
    this.changes = changes;
  }

  public final int getAction() { return Envelope.CHANGE; }
  public final boolean isChange() { return true; }

  // useful for Logic Providers.
  public Collection getChangeReports() { return changes; }

  boolean applyToSubscription(Subscription s, boolean isVisible) {
    return s.conditionalChange(object, changes, isVisible);
  }

}

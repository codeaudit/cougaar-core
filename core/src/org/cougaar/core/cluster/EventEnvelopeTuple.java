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

import java.util.*;

/** An EnvelopeTuple indicating an in-transaction event.
 * The events themselves are transient by definition.
 **/

public final class EventEnvelopeTuple extends EnvelopeTuple {
  private transient Object object;
  public Object getObject() { return object; }

  public EventEnvelopeTuple(Object o) {
    object=o;
  }

  public final int getAction() { return Envelope.EVENT; }
  public final boolean isEvent() { return true; }

  boolean applyToSubscription(Subscription s, boolean isVisible) {
    return ((s instanceof EventSubscription) &&
            (object != null) &&
            ((EventSubscription)s).conditionalTrigger(object,isVisible));
  }
}

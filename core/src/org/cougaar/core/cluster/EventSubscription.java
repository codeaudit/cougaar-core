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

import org.cougaar.util.UnaryPredicate;
import java.util.*;

/** 
 * A pseudo-subscription which catches Events distributed by the 
 * plan.  EventSubscriptions need never be pre-filled as they catch
 * object which do not stay in the plan.
 **/

public class EventSubscription extends Subscription {

  public EventSubscription(UnaryPredicate p) {
    super(p);
  }

  // dummy methods - will not be usefully called
  protected void privateAdd(Object o, boolean isVisible) {}
  protected void privateRemove(Object o, boolean isVisible) {}
  protected void privateChange(Object o, List changes, boolean isVisible) {}
  public void fill(Envelope envelope) {}

  boolean conditionalTrigger(Object o, boolean isVisible) {
    if (predicate.execute(o)) {
      return subscriber.triggerEvent(o);
    } else {
      return false;
    }
  }

}

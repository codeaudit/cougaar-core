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

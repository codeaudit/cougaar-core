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

import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;

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

/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import org.cougaar.util.Empty;
import org.cougaar.util.Enumerator;
import org.cougaar.util.UnaryPredicate;

/**
 * A subscription which collects EnvelopeMetrics.
 *
 * @see Subscriber required system property that must be enabled
 * @see EnvelopeMetrics
 */
public class EnvelopeMetricsSubscription extends Subscription {

  private final boolean includeBlackboard;
  private final List myList = new ArrayList(5);

  public EnvelopeMetricsSubscription() {
    this(true);
  }

  public EnvelopeMetricsSubscription(boolean includeBlackboard) {
    super(null);
    this.includeBlackboard = includeBlackboard;
  }

  protected void resetChanges() {
    super.resetChanges();
    myList.clear();
  }

  /**
   * @return an enumeration of EnvelopeMetrics that have been added
   * since the last transaction.
   */
  public Enumeration getAddedList() {
    checkTransactionOK("getAddedList()");
    if (myList.isEmpty()) return Empty.enumeration;
    return new Enumerator(myList);
  }

  /** 
   * @return a possibly empty collection of EnvelopeMetrics that have
   * been added since the last transaction. Will not return null.
   **/
  public Collection getAddedCollection() {
    return myList;
  }

  public boolean apply(Envelope e) {
    if (!(e instanceof TimestampedEnvelope)) {
      return false;
    }
    TimestampedEnvelope te = (TimestampedEnvelope) e;
    if ((!includeBlackboard) && te.isBlackboard()) {
      return false;
    }
    EnvelopeMetrics em = new EnvelopeMetrics(te);
    myList.add(em);
    setChanged(true);
    return true;
  }

  // never called, due to "apply(..)" override:
  protected void privateAdd(Object o, boolean isVisible) { }
  protected void privateRemove(Object o, boolean isVisible) { }
  protected void privateChange(Object o, List changes, boolean isVisible) { }

}

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

package org.cougaar.core.domain;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.UnaryPredicate;

/**
 * The "root" plan allows LPs to add/change/remove objects
 * in the blackboard, plus maintains a collection of all
 * UniqueObjects in the blackboard.
 * <p>
 * This is the class that LPs see.
 */
public class RootPlanImpl
implements RootPlan, XPlan, SupportsDelayedLPActions
{
  private Blackboard blackboard;

  /** is this a UniqueObject? **/
  private static final UnaryPredicate uniqueObjectP = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof UniqueObject) {
        UniqueObject uo = (UniqueObject) o;
        return (uo.getUID() != null);
      }
      return false;
    }
  };

  /** Private container for UniqueObject lookup.  */
  private UniqueObjectSet uniqueObjectSet = new UniqueObjectSet();
  private CollectionSubscription uniqueObjectCollection;

  public void setupSubscriptions(Blackboard blackboard) {
    this.blackboard = blackboard;
    uniqueObjectCollection = new CollectionSubscription(uniqueObjectP, uniqueObjectSet);
    blackboard.subscribe(uniqueObjectCollection);
  }

  public UniqueObject findUniqueObject(UID uid) {
    return uniqueObjectSet.findUniqueObject(uid);
  }

  // Implementation of BlackboardServesLogProvider

  /**
   * Apply predicate against the entire "Blackboard".
   * User provided predicate
   **/
  public Enumeration searchBlackboard(UnaryPredicate predicate) {
    return blackboard.searchBlackboard(predicate);
  }

  public int countBlackboard(UnaryPredicate predicate) {
    return blackboard.countBlackboard(predicate);
  }

  /** Add Object to the RootPlan Collection
   * (All subscribers will be notified)
   **/
  public void add(Object o) {
    blackboard.add(o);
  }

  /** Removed Object to the RootPlan Collection
   * (All subscribers will be notified)
   **/
  public void remove(Object o) {
    blackboard.remove(o);
  }

  /** Change Object to the RootPlan Collection
   * (All subscribers will be notified)
   **/
  public void change(Object o) {
    blackboard.change(o, null);
  }

  /** Change Object to the RootPlan Collection
   * (All subscribers will be notified)
   **/
  public void change(Object o, Collection changes) {
    blackboard.change(o, changes);
  }

  /**
   * Alias for sendDirective(Directive, null);
   **/
  public void sendDirective(Directive dir) {
    blackboard.sendDirective(dir, null);
  }

  /**
   * Reliably send a directive. Take pains to retransmit this message
   * until it is acknowledged even if clusters crash.
   **/
  public void sendDirective(Directive dir, Collection changes) {
    blackboard.sendDirective(dir, changes);
  }

  public PublishHistory getHistory() {
    return blackboard.getHistory();
  }

  //
  // DelayedLPAction support
  //
  
  private Object dlpLock = new Object();
  private HashMap dlpas = new HashMap(11);
  private HashMap dlpas1 = new HashMap(11);

  public void executeDelayedLPActions() {
    synchronized (dlpLock) {
      // loop in case we get cascades somehow (we don't seem to)
      while (dlpas.size() > 0) {
        // flip the map
        HashMap pending = dlpas;
        dlpas = dlpas1;
        dlpas1 = pending;

        // scan the pending map
        for (Iterator i = pending.values().iterator(); i.hasNext(); ) {
          DelayedLPAction dla = (DelayedLPAction) i.next();
          try {
            dla.execute(this);
          } catch (RuntimeException re) {
            System.err.println("DelayedLPAction "+dla+" threw: "+re);
            re.printStackTrace();
          }
        }

        // clear the pending queue before iterating.
        pending.clear();
      }
    }
  }
  
  public void delayLPAction(DelayedLPAction dla) {
    synchronized (dlpLock) {
      DelayedLPAction old = (DelayedLPAction) dlpas.get(dla);
      if (old != null) {
        old.merge(dla);
      } else {
        dlpas.put(dla,dla);
      }
    }
  }

  public ABATranslation getABATranslation(AttributeBasedAddress aba) {
    return blackboard.getABATranslation(aba);
  }
}

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


import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

/** 
 * Adds a real delegate Collection to the Subscription, accessible 
 * via getCollection().
 **/


public class CollectionSubscription 
  extends Subscription 
  implements Collection
{

  /** The actual (delegate) Container **/
  protected Collection real;
  private boolean hasDynamicPredicate;

  public Collection getCollection() { return real; }

  public CollectionSubscription(UnaryPredicate p, Collection c) {
    super(p);
    real = c;
    hasDynamicPredicate = (p instanceof DynamicUnaryPredicate);
  }

  public CollectionSubscription(UnaryPredicate p) {
    super(p);
    real = new HashSet(13);
    hasDynamicPredicate = (p instanceof DynamicUnaryPredicate);
  }

  //
  // implement Collection
  //

  public String toString() { return "Subscription of "+real; }
  public boolean equals(Object o) { return (this == o); }
  public int size() { return real.size(); }
  public boolean isEmpty() { return real.isEmpty(); }
  public Enumeration elements() { 
    return new Enumerator(real);
  }
  public Object first() {
    Iterator i = real.iterator();
    return (i.hasNext())?(i.next()):null;
  }

  /** overrides Subscription.conditionalChange **/
  boolean conditionalChange(Object o, List changes, boolean isVisible) {
    if (hasDynamicPredicate) {
      // this logic could be written more tersely, but I think this
      // is more clear.
      boolean wasIn = real.contains(o);
      boolean isIn = predicate.execute(o);
      if (wasIn) {
        if (isIn) {            
          // it was and still is in the collection
          privateChange(o, changes, isVisible);
          return true;
        } else {
          // it was in the collection but isn't any more
          privateRemove(o, isVisible);
          privateChange(o, changes, isVisible);
          return true;
        }
      } else {
        if (isIn) {
          // it wasn't in the collection but it is now
          privateAdd(o, isVisible);
          privateChange(o, changes, isVisible);
          return true;
        } else {
          // is wasn't in and isn't now
          return false;
        }
      }
    } else {
      return super.conditionalChange(o,changes,isVisible);
    }
  }

  protected void privateAdd(Object o, boolean isVisible) { 
    real.add(o); 
  }
  protected void privateRemove(Object o, boolean isVisible) {
    real.remove(o);
  }

  protected void privateChange(Object o, List changes, boolean isVisible) {
    if (isVisible) {
      Set set = (Set) changeMap.get(o);
      // here we avoid creating a new set instance if it will only
      //   contain the anonymous change report
      if (changes == AnonymousChangeReport.LIST) {
        if (set == null) {
          changeMap.put(o, AnonymousChangeReport.SET);
        } else if (set != AnonymousChangeReport.SET) {
          set.add(AnonymousChangeReport.INSTANCE);
        }
      } else {
        if (set == null) {
          int l = changes.size();
          changeMap.put(o, set = new HashSet(l));
        } else if (set == AnonymousChangeReport.SET) {
          int l = 1 + changes.size();
          changeMap.put(o, set = new HashSet(l));
          set.add(AnonymousChangeReport.INSTANCE);
        }
        // this should be sufficient because "Set.add" is defined
        // as only adding an element if it isn't already there.
        // In any case, it is critical that only the *FIRST* of a 
        // match be included in the set.
        set.addAll(changes);
      }
    }
  }

  protected Enumeration privateGetChangedList() {
    if (changeMap.isEmpty()) return Empty.enumeration;
    return new Enumerator(changeMap.keySet());
  }

  protected Collection privateGetChangedCollection() {
    if (changeMap.isEmpty()) return Collections.EMPTY_SET;
    return changeMap.keySet();
  }

  private HashMap changeMap = new HashMap(13);

  // override to reset the changereport hash
  protected void resetChanges() {
    super.resetChanges();       // propagate reset
    changeMap.clear();
  }

  /** Return a Set which contains the set of ChangeReports which
   * apply to the specified object in the current transaction.
   * <p>
   * If an object is changed without specifying a ChangeReport
   * then the "AnonymousChangeReport" is used.  Thus the set
   * is always non-null and contains one or more entries.
   * <p>
   * If an object is not changed during the transaction then
   * this method returns null.
   * <p>
   * Illegal to call outside of transaction boundaries.
   *
   * @return if the object was changed: a non-null Set of one or 
   *    more ChangeReport instances, possibly containing the 
   *    AnonymousChangeReport; otherwise null.
   **/
  public Set getChangeReports(Object o) {
    checkTransactionOK("hasChanged()");
    return (Set) changeMap.get(o);
  }

  // finish implementing Collection
  /** implements Collection, but only effects the internal Collection object **/
  public boolean add(Object o) {
    return real.add(o);
  }
  /** implements Collection, but only effects the internal Collection object **/
  public boolean addAll(Collection c) {
    return real.addAll(c);
  }
  /** implements Collection, but only effects the internal Collection object **/
  public void clear() {
    real.clear();
  }
  /** implements Collection, but only effects the internal Collection object **/
  public boolean contains(Object o) {
    return real.contains(o);
  }
  /** implements Collection, but only effects the internal Collection object **/
  public boolean containsAll(Collection c) {
    return real.containsAll(c);
  }
  /** implements Collection, but only effects the internal Collection object **/
  public Iterator iterator() {
    return real.iterator();
  }
  /** implements Collection, but only effects the internal Collection object **/
  public boolean remove(Object o) {
    return real.remove(o);
  }
  /** implements Collection, but only effects the internal Collection object **/
  public boolean removeAll(Collection c) {
    return real.removeAll(c);
  }
  /** implements Collection, but only effects the internal Collection object **/
  public boolean retainAll(Collection c) {
    return real.retainAll(c);
  }
  /** implements Collection, but only effects the internal Collection object **/
  public Object[] toArray() {
    return real.toArray();
  }
  /** implements Collection, but only effects the internal Collection object **/
  public Object[] toArray(Object[] a) {
    return real.toArray(a);
  }
} 

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
    if (isVisible && changes != null) {
      int l = changes.size();
      HashSet set = (HashSet)changeMap.get(o);
      if (set == null) {
        changeMap.put(o, set = new HashSet(l));
      }
      // this should be sufficient because HashSet.add is defined
      // as only adding an element if it isn't already there.
      // In any case, it is critical that only the *FIRST* of a 
      // match be included in the set.
      set.addAll(changes);
    }
  }

  private HashMap changeMap = new HashMap(13);

  // override to reset the changereport hash
  protected void resetChanges() {
    super.resetChanges();       // propagate reset
    changeMap.clear();
  }

  /** Return a Set which contains the set of ChangeReports which
   * apply to the specified object in the current transaction.
   * As there is no requirement that there be a ChangeReport (or even an
   * actual change) associated with a publishChange, existance of an
   * object in a change list does not necessarily imply any ChangeReports
   * for that object.<p>
   *
   * Illegal to call outside of transaction boundaries.
   * @return a Set of zero or more ChangeReport instances or null.
   **/
  public Set getChangeReports(Object o) {
    subscriber.checkTransactionOK("hasChanged()");
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

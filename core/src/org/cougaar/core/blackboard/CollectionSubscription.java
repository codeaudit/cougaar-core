/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.blackboard;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cougaar.util.DynamicUnaryPredicate;
import org.cougaar.util.Empty;
import org.cougaar.util.Enumerator;
import org.cougaar.util.UnaryPredicate;

/** 
 * A subclass of {@link Subscription} that maintains a {@link
 * Collection} of blackboard objects matching the filter
 * predicate, plus {@link ChangeReport}s for the objects changed
 * since the last transaction. 
 */
public class CollectionSubscription 
  extends Subscription 
  implements Collection
{

  /** The actual (delegate) Container */
  protected Collection real;
  private boolean hasDynamicPredicate;
  private HashMap changeMap = new HashMap(13);

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

  /**
   * @return the subscription's collection -- use <code>this</code>
   * instead.
   */
  public Collection getCollection() { return real; }

  /**
   * Return the set of {@link ChangeReport}s for publishChanges made
   * to the specified object since the last transaction.
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
   */
  public Set getChangeReports(Object o) {
    checkTransactionOK("hasChanged()");
    return (Set) changeMap.get(o);
  }

  //
  // implement Collection
  //

  /** @return the subscription size */
  public int size() { return real.size(); }
  /** @return {@link #size} == 0 */
  public boolean isEmpty() { return real.isEmpty(); }
  /** @return an Iterator of the subscription contents */
  public Iterator iterator() { return real.iterator(); }
  /** @return true of the subscription contains the object */
  public boolean contains(Object o) { return real.contains(o); }
  /** @return true of the subscription contains all the objects */
  public boolean containsAll(Collection c) {
    return real.containsAll(c);
  }
  /** @return an array of the subscription contents */
  public Object[] toArray() { return real.toArray(); }
  /** @return an array of the subscription contents */
  public Object[] toArray(Object[] a) { return real.toArray(a); }

  // semi-bogus collection methods:

  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishAdd} instead.
   * Add an object to the backing collection, <i>not</i> the blackboard. 
   */
  public boolean add(Object o) { return real.add(o); }
  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishAdd} instead.
   * Add objects to the backing collection, <i>not</i> the blackboard. 
   */
  public boolean addAll(Collection c) { return real.addAll(c); }
  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishRemove} instead.
   * Clear the backing collection, <i>not</i> the blackboard. 
   */
  public void clear() { real.clear(); }
  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishRemove} instead.
   * Remove an object from the backing collection, <i>not</i> the blackboard. 
   */
  public boolean remove(Object o) { return real.remove(o); }
  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishRemove} instead.
   * Remove objects from the backing collection, <i>not</i> the blackboard. 
   */ 
  public boolean removeAll(Collection c) { return real.removeAll(c); }
  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishRemove} instead.
   * Retain objects in the backing collection, <i>not</i> the blackboard. 
   */
  public boolean retainAll(Collection c) { return real.retainAll(c); }

  public boolean equals(Object o) { return (this == o); }

  public String toString() { return "Subscription of "+real; }

  /** @return an enumeration of the subscription objects */
  public Enumeration elements() { 
    return new Enumerator(real);
  }
  
  /** @return the first object in the collection. */
  public Object first() {
    Iterator i = real.iterator();
    return (i.hasNext())?(i.next()):null;
  }

  //
  // subscriber methods:
  //

  /** overrides Subscription.conditionalChange */
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

  /** {@link Subscriber} method to add an object */
  protected void privateAdd(Object o, boolean isVisible) { 
    real.add(o); 
  }

  /** {@link Subscriber} method to remove an object */
  protected void privateRemove(Object o, boolean isVisible) {
    real.remove(o);
  }

  /** {@link Subscriber} method to change an object */
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

  /** {@link IncrementalSubscription} method to get the changed enumeration */
  protected Enumeration privateGetChangedList() {
    if (changeMap.isEmpty()) return Empty.enumeration;
    return new Enumerator(changeMap.keySet());
  }

  /** {@link IncrementalSubscription} method to get the changed collection */
  protected Collection privateGetChangedCollection() {
    if (changeMap.isEmpty()) return Collections.EMPTY_SET;
    return changeMap.keySet();
  }

  /** {@link Subscriber} method to reset the ChangeReports map */
  protected void resetChanges() {
    super.resetChanges();       // propagate reset
    changeMap.clear();
  }
}

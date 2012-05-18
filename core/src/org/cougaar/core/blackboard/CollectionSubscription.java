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

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.util.DynamicUnaryPredicate;
import org.cougaar.util.Empty;
import org.cougaar.util.Enumerator;
import org.cougaar.util.UnaryPredicate;

/** 
 * A subclass of {@link Subscription} that maintains a {@link
 * Collection} of blackboard objects matching the filter
 * predicate, plus {@link ChangeReport}s for the objects changed
 * since the last transaction. 
 *
 * @property org.cougaar.core.blackboard.trackUnusedCollections
 * Periodically log all CollectionSubscriptions that never use their
 * "real" backing collection and could likely be replaced with
 * DeltaSubscriptions, which would reduce the memory footprint.
 * (defaults to false)
 */
public class CollectionSubscription 
  extends Subscription 
  implements Collection
{

  private static final boolean TRACK_UNUSED_COLLECTIONS =
    SystemProperties.getBoolean(
        "org.cougaar.core.blackboard.trackUnusedCollections");

  /** The actual (delegate) Container */
  protected Collection real;
  private final boolean hasDynamicPredicate;
  private final HashMap changeMap = new HashMap(13);
  private boolean usedReal;

  private static final ObjectTracker USE_TRACKER;
  static {
    ObjectTracker u = null;
    if (TRACK_UNUSED_COLLECTIONS) {
      u = new ObjectTracker();
      int period = 30*1000;
      String[] ignored_classes = new String[] {
        "org.cougaar.core.blackboard.",
        "org.cougaar.planning.plugin.legacy.PluginAdapter",
      };
      u.startThread(period, ignored_classes);
    }
    USE_TRACKER = u;
  }

  public CollectionSubscription(UnaryPredicate p, Collection c) {
    super(p);
    real = c;
    hasDynamicPredicate = (p instanceof DynamicUnaryPredicate);
    recordCreation();
  }

  public CollectionSubscription(UnaryPredicate p) {
    super(p);
    real = new HashSet(13);
    hasDynamicPredicate = (p instanceof DynamicUnaryPredicate);
    recordCreation();
  }

  /**
   * Retrieve the underlying Collection backing this Subscription. 
   * When an object is publishAdded and matches the predicate, it 
   * will be added to the Collection. If it is later publishRemoved 
   * (and the predicate still matches), then the object will be 
   * removed from the Collection.
   * @return the subscription's collection -- use <code>this</code>
   * instead.
   */
  public Collection getCollection() { recordUse(); return real; }

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
  public int size() { recordUse(); return real.size(); }
  /** @return {@link #size} == 0 */
  public boolean isEmpty() { recordUse(); return real.isEmpty(); }
  /** @return an Iterator of the subscription contents */
  public Iterator iterator() { recordUse(); return real.iterator(); }
  /** @return true of the subscription contains the object */
  public boolean contains(Object o) { recordUse(); return real.contains(o); }
  /** @return true of the subscription contains all the objects */
  public boolean containsAll(Collection c) {
    recordUse(); return real.containsAll(c);
  }
  /** @return an array of the subscription contents */
  public Object[] toArray() { recordUse(); return real.toArray(); }
  /** @return an array of the subscription contents */
  public Object[] toArray(Object[] a) { recordUse(); return real.toArray(a); }

  // semi-bogus collection methods:

  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishAdd} instead.
   * Add an object to the backing collection, <i>not</i> the blackboard. 
   */
  public boolean add(Object o) { recordUse(); return real.add(o); }
  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishAdd} instead.
   * Add objects to the backing collection, <i>not</i> the blackboard. 
   */
  public boolean addAll(Collection c) { recordUse(); return real.addAll(c); }
  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishRemove} instead.
   * Clear the backing collection, <i>not</i> the blackboard. 
   */
  public void clear() { recordUse(); real.clear(); }
  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishRemove} instead.
   * Remove an object from the backing collection, <i>not</i> the blackboard. 
   */
  public boolean remove(Object o) { recordUse(); return real.remove(o); }
  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishRemove} instead.
   * Remove objects from the backing collection, <i>not</i> the blackboard. 
   */ 
  public boolean removeAll(Collection c) { recordUse(); return real.removeAll(c); }
  /**
   * Use {@link org.cougaar.core.service.BlackboardService#publishRemove} instead.
   * Retain objects in the backing collection, <i>not</i> the blackboard. 
   */
  public boolean retainAll(Collection c) { recordUse(); return real.retainAll(c); }

  public boolean equals(Object o) { return (this == o); }

  public String toString() { return "Subscription of "+real; }

  /** @return an enumeration of the subscription objects */
  public Enumeration elements() { 
    recordUse(); return new Enumerator(real);
  }
  
  /** @return the first object in the collection. */
  public Object first() {
    recordUse();
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
	int size = changes.size();
	for (int i=0; i<size; i++) {
	    Object change = changes.get(i);
	    if (change instanceof OverrideChangeReport) set.remove(change);
	    set.add(change);
	}
        // set.addAll(changes);
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

  private void recordCreation() {
    if (TRACK_UNUSED_COLLECTIONS) {
      USE_TRACKER.add(this);
      if (hasDynamicPredicate || (this instanceof DeltaSubscription)) {
        recordUse();
      }
    }
  }
  private void recordUse() {
    if (TRACK_UNUSED_COLLECTIONS && !usedReal) {
      usedReal = true;
      USE_TRACKER.remove(this);
    }
  }
}

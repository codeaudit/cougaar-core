/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster;

import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.util.UnaryPredicate;
import java.util.*;

/** Envelope is an encapsulation of an atomic update
 * of a set data structure.  E.g. A representation of Container-level transaction.
 * Envelope does no synchronization -
 * it must never be accessed simultaneously by multiple readers/writers.
 * There are several types of transaction elements (EnvelopeTuple) supported:
 *   ADD	add an object to the set.
 *   REMOVE     remove an object from the set.
 *   CHANGE     mark the object as changed in the set.
 *   BULK       like ADD of a set of Objects, with the slightly different
 *                functional semantics.  In particular, since these are
 *                only emitted Blackboard on
 *                initialization of subscriptions, and by PersistenceSubscribers
 *                on LogPlan rehydration, LogicProviders function differently
 *                on BULKs than ADDs, for instance, business rules which
 *                fire on new LogPlan elements and produce other LogPlan
 *                elements will not fire on BULKs because the BULK delta
 *                should already include *those* products.
 *   EVENT	An indication of an Event distributed in-band with respect
 *		  to transactions.
 **/
public class Envelope implements java.io.Serializable {
  public Envelope() {
  }

  /** a set of changes to make to a subscriber's data structures */
  private final List deltas = new ArrayList(5);

  /** direct access to the internal structure to allow for more efficient
   * traversal of deltas.
   **/
  final List getRawDeltas() { return deltas; }

  public static final int ADD = 0;
  public static final int REMOVE = 1;
  public static final int CHANGE = 2;
  /**
   * BULK is a bulk add - then Object is a Container of objects
   * Additionally, BULK tuples are generally handled differently than 
   * ADDs by LogicProviders (See documentation on individual LPs to see
   * how they process BULK transactions.
   **/
  public static final int BULK = 3;

  public static final int EVENT = 4;

  public final EnvelopeTuple addObject(Object o) {
    if (o == null) throw new IllegalArgumentException("Null Object");
    EnvelopeTuple t = new AddEnvelopeTuple(o);
    deltas.add(t);
    return t;
  }
  public final EnvelopeTuple removeObject(Object o) {
    if (o == null) throw new IllegalArgumentException("Null Object");
    EnvelopeTuple t = new RemoveEnvelopeTuple(o);
    deltas.add(t);
    return t;
  }
  public final EnvelopeTuple changeObject(Object o, List changes) {
    if (o == null) throw new IllegalArgumentException("Null Object");
    EnvelopeTuple t = new ChangeEnvelopeTuple(o, changes);
    deltas.add(t);
    return t;
  }
  public final void addTuple(EnvelopeTuple t) {
    deltas.add(t);
  }

  /** how many elements are in the Envelope?*/
  public final int size() {
    return deltas.size();
  }

  public final Iterator getAllTuples()
  {
    return deltas.iterator();
  }

  /** equivalent to adding a homogeneous Collection of objects
   * as separate adds.  Distributor-level predicate tests will
   * assume that all objects in the Collection apply to the same
   * degree to a given predicate instance.
   * The container must be be immutable, as there is no guarantee
   * that it will be unpacked at any specific time.  If this is
   * a problem, the Enumeration form should be used.
   **/
  public final EnvelopeTuple bulkAddObject(Collection c) {
    if (c == null) throw new IllegalArgumentException("Null Collection");
    EnvelopeTuple t = new BulkEnvelopeTuple(c);
    deltas.add(t);
    return t;
  }

  /** safer form of bulkAddObject does the equivalent
   * of calling bulkAddObject on a container
   * constructed by iterating over the elements of 
   * the Enumeration argument.
   **/
  public final EnvelopeTuple bulkAddObject(Enumeration en) {
    List v = new ArrayList();
    while (en.hasMoreElements()) {
      v.add(en.nextElement());
    }

    EnvelopeTuple t = new BulkEnvelopeTuple(v);
    deltas.add(t);
    return t;
  }

  /** safer form of bulkAddObject does the equivalent
   * of calling bulkAddObject on a container
   * constructed by iterating over the elements of 
   * the argument.
   **/
  public final EnvelopeTuple bulkAddObject(Iterator i) {
    List v = new ArrayList();
    while (i.hasNext()) {
      v.add(i.next());
    }

    EnvelopeTuple t = new BulkEnvelopeTuple(v);
    deltas.add(t);
    return t;
  }

  public final EnvelopeTuple addEvent(Object o) {
    if (o == null) throw new IllegalArgumentException("Null Object");
    EnvelopeTuple t = new EventEnvelopeTuple(o);
    deltas.add(t);
    return t;
  }

  /** boolean used to decide on visibility of subscription modifications
   * in applyToSubscription. Overridden by PersistenceEnvelope.
   **/
  protected boolean isVisible() { return true; }

  /**
   * Apply all object deltas in this envelope to the subscription.
   **/
  public final boolean applyToSubscription(Subscription subscription) {
    boolean vp = isVisible();     // in case we've got *lots* of tuples.
    boolean somethingFired = false;
    // we use the List directly instead of getAllTuples to avoid iterator overhead.
    int l = deltas.size();
    for (int i = 0; i<l; i++) {
      EnvelopeTuple tuple = (EnvelopeTuple) deltas.get(i);
      somethingFired |= tuple.applyToSubscription(subscription, vp);
    }
    return vp && somethingFired;
  }

}


/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
 
package org.cougaar.core.cluster.persist;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

/**
 * This class is used to wrap arbitrary objects such that they can be
 * used to uniquely key a hashtable. Equality and hashcode are based
 * entirely on object identity. This is also a WeakReference so that
 * the existence of this key does not force objects to be retained.
 **/
class PersistenceKey extends WeakReference{

  private int hash;
  private int referenceId;

  PersistenceKey(Object o) {
    super(o);
    hash = System.identityHashCode(o); // Get this now before the object disappears
  }

  PersistenceKey(Object o, ReferenceQueue refQ, PersistenceReference id) {
    super(o, refQ);
    hash = System.identityHashCode(o); // Get this now before the object disappears
    referenceId = id.intValue();
  }

  public int hashCode() {
    return hash;
  }

  public boolean equals(Object o2) {
    if (o2 == this) return true; // This is the usual case -- make it first
    if (o2 instanceof PersistenceKey) {
      Object t = this.get();
      Object u = ((PersistenceKey) o2).get();
      if (t == null || u == null) return false;	// Garbage collected -- can't be equal
      return t == u;
    }
    return false;
  }
  public int getReferenceId() {
    return referenceId;
  }
}

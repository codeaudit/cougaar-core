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

package org.cougaar.core.persist;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

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

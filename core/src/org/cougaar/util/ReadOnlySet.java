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
package org.cougaar.util;

import java.util.*;

/**
 * Wraps a Set disabling all mutator methods.
 **/
public class ReadOnlySet implements Set {
  private Set inner;

  public ReadOnlySet(Set s) {
    inner = s;
  }

  // Accessors

  public int size() { return inner.size(); }
  public boolean isEmpty() { return inner.isEmpty(); }
  public boolean contains(Object o) { return inner.contains(o); }
  public boolean containsAll(Collection c) { return inner.containsAll(c); }
  public Iterator iterator() {
    return new Iterator() {
      Iterator it = inner.iterator();
      public boolean hasNext() {
        return it.hasNext();
      }
      public Object next() {
        return it.next();
      }
      public void remove() {
        throw new UnsupportedOperationException("ReadOnlySet: remove disallowed");
      }
    };
  }
  public boolean equals(Object o) { return inner.equals(o); }
  public int hashCode() { return inner.hashCode(); }
  public Object[] toArray() { return inner.toArray(); }
  public Object[] toArray(Object[] a) { return inner.toArray(a); }

  // Mutators

  public boolean add(Object o) {
    throw new UnsupportedOperationException("ReadOnlySet: add disallowed");
  }

  public boolean remove(Object o) {
    throw new UnsupportedOperationException("ReadOnlySet: remove disallowed");
  }

  public boolean addAll(Collection c) {
    throw new UnsupportedOperationException("ReadOnlySet: addAll disallowed");
  }

  public boolean retainAll(Collection c) {
    throw new UnsupportedOperationException("ReadOnlySet: retainAll disallowed");
  }

  public boolean removeAll(Collection c) {
    throw new UnsupportedOperationException("ReadOnlySet: removeAll disallowed");
  }

  public void clear() {
    throw new UnsupportedOperationException("ReadOnlySet: clear disallowed");
  }
}

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
 * KeyedSet is a custom collection which looks like a Set,
 * but allows redefinition of the key to use.  The default key
 * is the identity operation.
 **/

public class KeyedSet
  implements Set
{
  protected Map inner;

  public KeyedSet() { 
    inner = new HashMap(89);
  }
  public KeyedSet(int s) { 
    inner = new HashMap(s);
  }
  public KeyedSet(Collection c) {
    inner = new HashMap(c.size()*2+1);
    addAll(c);
  }

  public KeyedSet makeSynchronized() {
    inner = Collections.synchronizedMap(inner);
    return this;
  }

  public void clear() { inner.clear(); }
  public boolean contains(Object o) { return inner.containsValue(o); }
  public boolean containsAll(Collection c) {
    return inner.values().containsAll(c);
  }
  public boolean isEmpty() { return inner.isEmpty(); }
  public Iterator iterator() { return inner.values().iterator(); }
  public int size() { return inner.size(); }
  public Object[] toArray() { return inner.values().toArray(); }
  public Object[] toArray(Object[] a) { return inner.values().toArray(a); }

  public Set keySet() {
    return inner.keySet();
  }

  public int hashCode() { return 7+inner.hashCode(); }
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof CollectionDelegate) {
      return inner.equals(((CollectionDelegate)o).inner);
    } else {
      return false;
    }
  }

  /** override this method to get a more useful key **/
  protected Object getKey(Object o) {
    return o;
  }

  public boolean add(Object o) { 
    Object key = getKey(o);
    if (key != null) {
      return (inner.put(key,o) == o);
    } else {
      return false;
    }
  }

  public boolean addAll(Collection c) {
    boolean hasChanged = false;
    for (Iterator i = c.iterator(); i.hasNext();) {
      if (add(i.next()))
        hasChanged = true;
    }
    return hasChanged;
  }

  public boolean remove(Object o) {
    Object key = getKey(o);
    if (key != null) {
      return (inner.remove(key) != null);
    } else {
      return false;
    }
  }
  public boolean removeAll(Collection c) {
    boolean hasChanged = false;
    for (Iterator i = c.iterator(); i.hasNext();) {
      if (remove(i.next()))
        hasChanged = true;
    }
    return hasChanged;
  }

  // implement some time
  public boolean retainAll(Collection c) {
    throw new RuntimeException("KeyedSet.retainAll not implemented");
  }
}

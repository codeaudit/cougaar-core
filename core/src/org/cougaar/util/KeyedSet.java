/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
  protected HashMap inner;

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
  
  public void clear() { inner.clear(); }
  public boolean contains(Object o) { return inner.containsValue(o); }
  public boolean containsAll(Collection c) { 
    for (Iterator i = c.iterator(); i.hasNext();) 
      if (! inner.containsValue(i.next())) return false;
    return true;
  }
  public boolean isEmpty() { return inner.isEmpty(); }
  public Iterator iterator() { return inner.values().iterator(); }
  public int size() { return inner.size(); }
  public Object[] toArray() { return inner.values().toArray(); }
  public Object[] toArray(Object[] a) { return inner.values().toArray(a); }

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

/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import java.util.*;

public class CollectionDelegate
  implements Collection
{
  protected Collection inner;

  public CollectionDelegate(Collection realCollection) {
    inner = realCollection;
  }
  
  public boolean add(Object o) { return inner.add(o); }
  public boolean addAll(Collection c) { return inner.addAll(c); }
  public void clear() { inner.clear(); }
  public boolean contains(Object o) { return inner.contains(o); }
  public boolean containsAll(Collection c) { return inner.containsAll(c); }
  public boolean isEmpty() { return inner.isEmpty(); }
  public Iterator iterator() { return inner.iterator(); }
  public boolean remove(Object o) { return inner.remove(o); }
  public boolean removeAll(Collection c) { return inner.removeAll(c); }
  public boolean retainAll(Collection c) { return inner.retainAll(c); }
  public int size() { return inner.size(); }
  public Object[] toArray() { return inner.toArray(); }
  public Object[] toArray(Object[] a) { return inner.toArray(a); }

  public String toString() {
    return "Delate to "+inner;
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
}

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

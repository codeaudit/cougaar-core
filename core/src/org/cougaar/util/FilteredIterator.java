package org.cougaar.util;

import java.util.Iterator;

/**
 * Wraps an Enumeration and allows elements satisfying a UnaryPredicate to get through
 **/
public class FilteredIterator implements Iterator {
  private UnaryPredicate predicate;
  private Iterator base;
  private Object next = null;

  public FilteredIterator(Iterator iter, UnaryPredicate pred) {
    base = iter;
    predicate = pred;
  }

  public boolean hasNext() {
    while (next == null && base.hasNext()) {
      next = base.next();
      if (predicate.execute(next)) return true;
      next = null;
    }
    return next != null;
  }

  public Object next() {
    if (next != null || hasNext()) {
      Object result = next;
      next = null;
      return result;
    }
    throw new java.util.NoSuchElementException("Filtered Iterator");
  }

  public void remove() {
    base.remove();
  }
}

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

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

import java.util.Enumeration;

/**
 * Wraps an Enumeration and allows elements satisfying a UnaryPredicate to get through
 **/
public class FilteredEnumeration implements Enumeration {
  private UnaryPredicate predicate;
  private Enumeration base;
  private Object next = null;

  public FilteredEnumeration(Enumeration enum, UnaryPredicate pred) {
    base = enum;
    predicate = pred;
  }

  public boolean hasMoreElements() {
    while (next == null && base.hasMoreElements()) {
      next = base.nextElement();
      if (predicate.execute(next)) return true;
      next = null;
    }
    return next != null;
  }

  public Object nextElement() {
    if (next != null || hasMoreElements()) {
      Object result = next;
      next = null;
      return result;
    }
    throw new java.util.NoSuchElementException("Vector Enumeration");
  }
}

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
import java.util.NoSuchElementException;

/** Useful for cheaply indicating empty sets */
public final class SingleElementEnumeration implements Enumeration {
  private boolean hasMore = true;
  private Object object;

  public SingleElementEnumeration(Object o) {
    object = o;
  }

  public final boolean hasMoreElements() { return hasMore; }
  public final Object nextElement() throws NoSuchElementException { 
    if (hasMore) {
      hasMore = false;
      Object o = object;
      object = null;
      return o;
    } else {
      throw new NoSuchElementException();
    }
  }
}

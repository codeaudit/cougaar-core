/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Useful for cheaply indicating empty sets */
public class EmptyIterator implements Iterator {
  public final boolean hasNext() { return false; }
  public final Object next() { throw new NoSuchElementException(); }
  public final void remove() { }

  public static final Iterator emptyIterator = new EmptyIterator();
  public static final Iterator iterator() {
    return emptyIterator;
  }
}
      

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

import java.util.Iterator;
import java.util.Enumeration;
import java.util.Collection;

/** An Enumeration which is backed by an Iterator.
 * Useful for exposing a pre-collections interface to a collections-based
 * implementation.
 **/

public final class Enumerator implements Enumeration {
  private Iterator i;
  public Enumerator(Iterator i) { this.i = i; }
  public Enumerator(Collection c) { this.i = c.iterator(); }
  public final boolean hasMoreElements() { return i.hasNext(); }
  public final Object nextElement() { return i.next(); }
}
      

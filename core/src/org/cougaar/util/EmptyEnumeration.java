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

import java.util.Enumeration;
import java.util.NoSuchElementException;

/** Useful for cheaply indicating empty sets */
public class EmptyEnumeration implements Enumeration {
  public final boolean hasMoreElements() { return false; }
  public final Object nextElement() throws NoSuchElementException { 
    throw new NoSuchElementException();
  }
  public static final Enumeration elements() {
    return defaultEnumeration;
  }

  public static final Enumeration defaultEnumeration = new EmptyEnumeration();
  public static final Enumeration getEnumeration() {
    return defaultEnumeration;
  }

}
      

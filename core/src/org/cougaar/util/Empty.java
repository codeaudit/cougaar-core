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

/** Holding class for providing singleton collection-like
 * objects.
 **/
public class Empty {
  public static final Enumeration enumeration = new EmptyEnumeration();
  public static final Enumeration elements() { return enumeration; }

  public static final Iterator iterator = new EmptyIterator();
  public static final Iterator iterator() { return iterator; }
}
      

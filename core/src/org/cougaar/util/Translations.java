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

/** 
 * Translations holds a set of static methods for translating between
 * various representations of objects, primarily collection-like
 * things.  For the most part, this functionality should either not be
 * needed (e.g. enumeration versus iteration) or belongs in core java.
 **/

public final class Translations {
  private Translations() {}

  /** copy the contents of an enumeration into a collection for later
   * searching.  The Enumeration will be empty when the method
   * returns.
   **/
  public static Collection toCollection(Enumeration e) {
    Collection tmp = new ArrayList();
    while (e.hasMoreElements()) {
      tmp.add(e.nextElement());
    }
    return tmp;
  }

  public static Collection toCollection(Iterator i) {
    Collection tmp = new ArrayList();
    while (i.hasNext()) {
      tmp.add(i.next());
    }
    return tmp;
  }

  public static Enumeration toEnumeration(Collection c) {
    return new Enumerator(c);
  }

  public static Enumeration toEnumeration(Iterator i) {
    return new Enumerator(i);
  }
}

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

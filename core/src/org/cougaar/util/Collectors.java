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
 * Collectors is a class of static methods for applying a Thunk
 * to various sorts of collection-like data structures.
 */

public final class Collectors {

  /** apply the Thunk to each element of the collection. **/
  public static void apply(Thunk t, Collection c) {
    // BOZO - this should not be necessary. Under some set of circumstances however the VM
    // calls this version of apply with a List
    if (c instanceof List) {
      /* System.err.println("org.cougaar.util.Collectors.apply(Thunk t, Collection c): c is actually a List. " +
                         "Calling apply(Thunk t, List l)");
      */
      apply(t, (List) c);
    } else {
      for (Iterator i = c.iterator(); i.hasNext();) {
        t.apply(i.next());
      }
    }
  }

  /** apply the Thunk to each element of the list. **/
  public static void apply(Thunk t, List l) {
    // optimize for List
    int listSize = l.size();
    for (int index = 0; index < listSize; index++) {
      t.apply(l.get(index));
    }
  }

  /** apply the Thunk to each element of the collection. **/
  public static void apply(Thunk t, Iterator i) {
    while (i.hasNext()) {
      t.apply(i.next());
    }
  }

  /** apply the Thunk to each element of the Array **/
  public static void apply(Thunk t, Object[] a) {
    int l = a.length;
    for (int i = 0; i<l; i++) {
      t.apply(a[i]);
    }
  }

  /** apply the Thunk to each element of the Array.
   * Thunk will be applied to the first length elements.
   **/
  public static void apply(Thunk t, Object[] a, int length) {
    for (int i = 0; i<length; i++) {
      t.apply(a[i]);
    }
  }

  /** apply the Thunk to each element of the Array 
   * t will be applied to the length elements starting at position start.
   **/
  public static void apply(Thunk t, Object[] a, int start, int length) {
    int i = 0;
    int j = start;
    while (i<length) {
      t.apply(a[j]);
      i++;
      j++;
    }
  }
}






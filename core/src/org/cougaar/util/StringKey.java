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

/**
 * Wrapper around a String for use as a key in a hash table (hashMap, etc).
 * Has the benefit that it prevents recomputation of the hashCode each time.
 * Wherever possible, it is still much better to have the objects themselves 
 * provide a cheap hashcode.
 **/

public final class StringKey {
  private final String string;
  private final int hc;
  public StringKey(String s) {
    string = s;
    hc = s.hashCode();
  }
  public StringKey(Object o) {
    string = o.toString();
    hc = string.hashCode();
  }

  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof StringKey) {
      return string.equals(((StringKey) o).string);
    }
    return false;
  }
  public final int hashCode() { return hc; }

  public final String toString() { return string; }
}
    

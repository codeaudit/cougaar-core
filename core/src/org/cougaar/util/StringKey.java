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
    

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

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Collection;

/** An Enumeration which is backed by an Iterator over a copy of an
 * original collection.  Optimized for zero-element cases, and
 * non-zero length ArrayList arguments.
 *
 * Useful for exposing a pre-collections interface to a
 * collections-based implementation.
 **/

public final class BackedEnumerator implements Enumeration {
  private Object[] a;
  private int size;
  private int index=0;
  public BackedEnumerator(Collection c) { 
    size = c.size();
    if (size != 0) {
      Iterator ci = c.iterator();
      a = new Object[size];
      for (int i=0;i<size;i++) {
        a[i]=ci.next();
      }
    } else {
      a=null;
    }
  }
  // more efficient version for ArrayList
  public BackedEnumerator(ArrayList c) { 
    if (c.size() > 0) {
      a = c.toArray();
      size = a.length;
    } else {
      a=null;
      size=0;
    }
  }

  public BackedEnumerator(Enumeration e) {
    if (e.hasMoreElements()) {
      ArrayList tmp = new ArrayList();
      while (e.hasMoreElements()) {
        tmp.add(e.nextElement());
      }
      a = tmp.toArray();
      size=a.length;
    } else {
      a=null;
      size=0;
    }
  }
  public BackedEnumerator(Iterator i) {
    if (i.hasNext()) {
      ArrayList tmp = new ArrayList();
      while (i.hasNext()) {
        tmp.add(i.next());
      }
      a = tmp.toArray();
      size=a.length;
    } else {
      a=null;
      size=0;
    }
  }

  public final boolean hasMoreElements() { return (index<size); }
  public final Object nextElement() { return a[index++]; }
}
      

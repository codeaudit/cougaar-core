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

package org.cougaar.core.blackboard;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;

/**
 * A change report for an anonymous (unspecified) change.
 * <p>
 * This is used whenever a subscriber does:<pre>
 *     - publishChange(o)
 * or
 *     - publishChange(o, null)
 * or
 *     - publishChange(o, an-empty-collection)</pre>
 * <p>
 * Subscribers should watch for the "AnonymousChangeReport.SET".
 */
public final class AnonymousChangeReport implements ChangeReport {

  // singleton instance
  public static final AnonymousChangeReport INSTANCE =
    new AnonymousChangeReport();

  // a list containing the singleton
  public static final List LIST = new AnonList();

  // a set containing the singleton
  public static final Set SET = new AnonSet();


  private AnonymousChangeReport() { }

  private Object readResolve() { return INSTANCE; }

  public String toString() {
    return "anonymous";
  }

  static final long serialVersionUID = 1209837181010093282L;

  // singleton LIST with singleton-friendly "readResolve()":
  private static class AnonList extends AbstractList
    implements RandomAccess, Serializable {
      private AnonList() { }
      public int size() {return 1;}
      public boolean contains(Object obj) {return (obj == INSTANCE);}
      public Object get(int index) {
        if (index != 0)
          throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
        return INSTANCE;
      }
      private Object readResolve() { return LIST; }
      static final long serialVersionUID = 3190948102986892191L;
    }

  // singleton SET with singleton-friendly "readResolve()":
  private static class AnonSet extends AbstractSet
    implements Serializable
    {
      private AnonSet() {}
      public Iterator iterator() {
        return new Iterator() {
          private boolean hasNext = true;
          public boolean hasNext() {
            return hasNext;
          }
          public Object next() {
            if (hasNext) {
              hasNext = false;
              return INSTANCE;
            }
            throw new NoSuchElementException();
          }
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
      public int size() {return 1;}
      public boolean contains(Object obj) {return (obj == INSTANCE);}
      private Object readResolve() { return SET; }
      private static final long serialVersionUID = 409580998934879938L;
    }

}

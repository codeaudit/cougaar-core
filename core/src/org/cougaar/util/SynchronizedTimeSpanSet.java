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
import java.io.Serializable;

/**
 * A synchronized version of TimeSpanSet
 **/
public class SynchronizedTimeSpanSet extends TimeSpanSet {

  // constructors
  public SynchronizedTimeSpanSet() {
    super();
  }

  public SynchronizedTimeSpanSet(int i) {
    super(i);
  }

  public SynchronizedTimeSpanSet(Collection c) {
    super(c);
  }

  public SynchronizedTimeSpanSet(TimeSpanSet t) {
    super(t.size);

    synchronized(t) {
      unsafeUpdate(t);
    }
  }

  // Synchronization of ArrayList methods -  

  /**
   * BOZO - write a comment
   * Should simply return false
   */
  public synchronized boolean add(Object o) {
    return super.add(o);
  }

  /**
   * BOZO - write a comment
   * Should simply return false
   */
  public synchronized boolean addAll(Collection objects) {
    synchronized (objects) {
      return super.addAll(objects);
    }
  }

  public synchronized void clear() {
    super.clear();
  }
  public synchronized boolean contains(Object o) {
    return super.contains(o);
  }

  public synchronized boolean containsAll(Collection c) {
    synchronized (c) {
      return super.containsAll(c);
    }
  }

  public synchronized Object get(int index) {
    return super.get(index);
  }
  
  public synchronized int indexOf(Object elem) {
    return super.indexOf(elem);
  }

  public synchronized int lastIndexOf(Object elem) {
    return super.lastIndexOf(elem);
  }

  /**
   * BOZO - write a comment
   * Should simply return false
   */
  public synchronized boolean remove(Object o) {
    return super.remove(o);
  }

  public synchronized Object remove(int index) {
    return super.remove(index);
  }

  /**
   * BOZO - write a comment
   * Should simply return false
   */
  public synchronized boolean removeAll(Collection objects) {
    synchronized (objects) {
      return super.removeAll(objects);
    }
  }


  /**
   * BOZO - write a comment
   * Should simply return false
   */
  public synchronized boolean retainAll(Collection objects) {
    synchronized (objects) {
      return super.removeAll(objects);
    }
  }


  public synchronized int size() {
    return super.size();
  }

  public synchronized Object[] toArray() {
    return super.toArray();
  }

  public synchronized Object[] toArray(Object a[]) {
    return super.toArray(a);
  }

  public synchronized String toString() {
    return super.toString();
  }


  public synchronized Object last() {
    return super.last();
  }

  public synchronized Collection filter(UnaryPredicate predicate) {
    return super.filter(predicate);
  }

  /** @return the intersecting Element with the smallest timespan.
   * The result is undefined if there is a tie for smallest and null 
   * if there are no elements.
   **/
  public synchronized Object getMinimalIntersectingElement(final long time) {
    return super.getMinimalIntersectingElement(time);
  }

  public synchronized boolean equals(Object o) {
    synchronized(o) {
      return super.equals(o);
    }
  }

  public synchronized int hashCode() {
    return super.hashCode();
  }

   /** 
   * unsafeUpdate - not synchronized. As a protected method it assumes that
   * caller already has the synch lock.
   * Should only be used if c has already been validated.
   * @returns boolean - true if any elements added else false.
   */
  protected boolean unsafeUpdate(Collection c) {
    synchronized (c) {
      return super.unsafeUpdate(c);
    }
  }
 
}
  
  






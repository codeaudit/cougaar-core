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
 * Implements a traditional circular queue, except that it will enlarge 
 * backing array if it runs out of space.
 * Also implements the Collection API, except for remove methods.
 * This implementation leaves all synchronization to the caller.
 * The implementation is optimized to make add(Object) and Object next() 
 * operations as cheap as possible. size, isEmpty and peek are also very
 * cheap.  Other operations may be considerably more expensive.
 * The iterator supplied does NOT support fast-fail on modification.
 *
 * Inspired by _Data Structure and Algorithms_ by Aho, Hopcroft and Ullman.
 **/

public class CircularQueue extends AbstractCollection
{
  protected Object[] elements;
  protected int front;
  protected int rear;
  protected int size;
  protected int max;

  public CircularQueue() {
    this(5);
  }
  public CircularQueue(int max) {
    elements = new Object[max];
    front=0;
    rear=max-1;
    size = 0;
    this.max=max;
  }
    
  private final int nextIndex(int i) {
    return (i+1)%max;
  }
  public void clear() {
    front=0;
    rear=max-1;
    size=0;
  }
  public final boolean isEmpty() {
    return (nextIndex(rear)==front);
  }
  private final boolean isFull() {
    return (nextIndex(nextIndex(rear))==front);
  }

  public Object peek() {
    if (isEmpty())
      return null;
    else
      return elements[front];
  }
  public boolean add(Object el) {
    if (isFull())
      extend();
    int adv = nextIndex(rear);
    rear = adv;
    elements[adv]=el;
    size++;
    return true;
  }
  public boolean addAll(Collection c) {
    for (Iterator i = c.iterator(); i.hasNext(); ) {
      add(i.next());
    }
    return true;
  }
  public boolean remove(Object el) {
    throw new UnsupportedOperationException();
  }
  public boolean removeAll(Collection c) {
    throw new UnsupportedOperationException();
  }
  public boolean retainAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  public boolean contains(Object el) {
    int i = front;
    int x = nextIndex(rear);
    while (i!=x) {
      if (el.equals(elements[i])) 
        return true;
      i=nextIndex(i);
    }
    return false;
  }

  public Object[] toArray() {
    Object[] result = new Object[size];
    int i = front;
    int x = nextIndex(rear);
    int j = 0;
    while (i!=x) {
      result[j] = elements[i];
      i=nextIndex(i);
    }
    return result;
  }
    
  public Object[] toArray(Object[] result) {
    int i = front;
    int x = nextIndex(rear);
    int j = 0;
    while (i!=x) {
      result[j] = elements[i];
      i=nextIndex(i);
    }
    return result;
  }

  public boolean containsAll(Collection c) {
    // UGH! n^2
    for (Iterator i = c.iterator(); i.hasNext(); ) {
      if (!contains(i.next()))
        return false;
    }
    return true;
  }

  public Object next() {
    if (isEmpty()) {
      return null;
    } else {
      Object o = elements[front];
      elements[front]=null;     // allow gc
      front = nextIndex(front);
      size--;
      return o;
    }
  }
  private void extend() {
    //System.err.println("Extending "+this);
    int nmax = max*2;
    Object[] nelements = new Object[nmax];
    // we could do this with two array copies
    for (int i = 0; i<size; i++) {
      nelements[i]=peek();
      front = nextIndex(front);
    }
    elements=nelements;
    front=0;
    rear=size-1;
    max=nmax;
  }
      
  public Iterator iterator() {
    final int capturedFront = front;
    return new Iterator() {
        private int i = capturedFront;
        public final boolean hasNext() {
          return (nextIndex(rear)!=i);
        }
        public final Object next() {
          if (!hasNext()) throw new NoSuchElementException();
          Object o = elements[i];
          i = nextIndex(i);
          return o;
        }
        public final void remove() {}
      };
  }

  public int size() {
    return size;
  }

  public String toString() {
    return "{CircularQueue "+size+"/"+max+"}";
  }

  public String toPrettyString() {
    String s="{";
    int i = front;
    int x = nextIndex(rear);
    while (i!=x) {
      s=s+(elements[i].toString());
      i=nextIndex(i);
      if (i!=x) s=s+" ";
    }
    return s+"}";
  }

  public static void main(String args[]) {
    CircularQueue q = new CircularQueue(2);
    for (int i = 0; i < 25; i++) {
      Integer o = new Integer(i);
      q.add(o);
      System.out.println("add i="+i+" q="+q);
    }

    for (int i = 0; i< 12;i++) {
      Object o = q.next();
      System.out.println("next i="+i+" o="+o+" q="+q);
    }
    
    for (int i = 0; i< 12;i++) {
      q.add(new Integer(i+25));
      System.out.println("re-add i="+i+" q="+q);
    }
  }
}


/*
 * Modifications of Sun Microsystems code:
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

/*
 * @(#)ArrayList.java	1.19 99/04/22
 *
 * Copyright 1997-1999 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package org.cougaar.util;

import java.util.*;

/**
 * A copy of java.util.ArrayList, modified so that previously
 * private data members are here declared protected and which does not
 * use iterators to walk itself p>
 * 
 * This allows extending classes to have efficient access to the 
 * actual data storage elements so that the supported API extensions
 * have first-class access.
 *
 * @see java.util.ArrayList
 */

public class ArrayListFoundation extends AbstractList 
  implements List, Cloneable, java.io.Serializable 
{
  /**
   * The array buffer into which the elements of the ArrayListFoundation are stored.
   * The capacity of the ArrayListFoundation is the length of this array buffer.
   */
  protected transient Object elementData[];
  
  /**
   * The size of the ArrayListFoundation (the number of elements it contains).
   *
   * @serial
   */
  protected transient int size;
  
  /**
   * Constructs an empty list with the specified initial capacity.
   *
   * @param   initialCapacity   the initial capacity of the list.
   */
  public ArrayListFoundation(int initialCapacity) {
    super();
    this.elementData = new Object[initialCapacity];
  }
  
  /**
   * Constructs an empty list.
   */
  public ArrayListFoundation() {
    this(10);
  }
  
  /**
   * Constructs a list containing the elements of the specified
   * collection, in the order they are returned by the collection's
   * iterator.  The <tt>ArrayListFoundation</tt> instance has an initial capacity of
   * 110% the size of the specified collection.
   */
  public ArrayListFoundation(Collection c) {
    size = c.size();
    elementData = new Object[(size*110)/100]; // Allow 10% room for growth
    c.toArray(elementData);
  }
  
  /**
   * Trims the capacity of this <tt>ArrayListFoundation</tt> instance to be the
   * list's current size.  An application can use this operation to minimize
   * the storage of an <tt>ArrayListFoundation</tt> instance.
   */
  public void trimToSize() {
    modCount++;
    int oldCapacity = elementData.length;
    if (size < oldCapacity) {
      Object oldData[] = elementData;
      elementData = new Object[size];
      System.arraycopy(oldData, 0, elementData, 0, size);
    }
  }
  
  /**
   * Increases the capacity of this <tt>ArrayListFoundation</tt> instance, if
   * necessary, to ensure  that it can hold at least the number of elements
   * specified by the minimum capacity argument. 
   *
   * @param   minCapacity   the desired minimum capacity.
   */
  public synchronized void ensureCapacity(int minCapacity) {
    modCount++;
    int oldCapacity = elementData.length;
    if (minCapacity > oldCapacity) {
      Object oldData[] = elementData;
      int newCapacity = (oldCapacity * 3)/2 + 1;
      if (newCapacity < minCapacity)
        newCapacity = minCapacity;
      elementData = new Object[newCapacity];
      System.arraycopy(oldData, 0, elementData, 0, size);
    }
  }
  
  /**
   * Returns the number of elements in this list.
   *
   * @return  the number of elements in this list.
   */
  public int size() {
    return size;
  }
  
  /**
   * Tests if this list has no elements.
   *
   * @return  <tt>true</tt> if this list has no elements;
   *          <tt>false</tt> otherwise.
   */
  public boolean isEmpty() {
    return size == 0;
  }
  
  /**
   * Returns <tt>true</tt> if this list contains the specified element.
   *
   * @param o element whose presence in this List is to be tested.
   */
  public boolean contains(Object elem) {
    return indexOf(elem) >= 0;
  }
  
  /**
   * Searches for the first occurence of the given argument, testing 
   * for equality using the <tt>equals</tt> method. 
   *
   * @param   elem   an object.
   * @return  the index of the first occurrence of the argument in this
   *          list; returns <tt>-1</tt> if the object is not found.
   * @see     Object#equals(Object)
   */
  public int indexOf(Object elem) {
    if (elem == null) {
      for (int i = 0; i < size; i++)
        if (elementData[i]==null)
          return i;
    } else {
      for (int i = 0; i < size; i++)
        if (elem.equals(elementData[i]))
          return i;
    }
    return -1;
  }
  
  /**
   * Returns the index of the last occurrence of the specified object in
   * this list.
   *
   * @param   elem   the desired element.
   * @return  the index of the last occurrence of the specified object in
   *          this list; returns -1 if the object is not found.
   */
  public int lastIndexOf(Object elem) {
    if (elem == null) {
      for (int i = size-1; i >= 0; i--)
        if (elementData[i]==null)
          return i;
    } else {
      for (int i = size-1; i >= 0; i--)
        if (elem.equals(elementData[i]))
          return i;
    }
    return -1;
  }
  
  /**
   * Returns a shallow copy of this <tt>ArrayListFoundation</tt> instance.  (The
   * elements themselves are not copied.)
   *
   * @return  a clone of this <tt>ArrayListFoundation</tt> instance.
   */
  public Object clone() {
    try { 
      ArrayListFoundation v = (ArrayListFoundation)super.clone();
      v.elementData = new Object[size];
      System.arraycopy(elementData, 0, v.elementData, 0, size);
      v.modCount = 0;
      return v;
    } catch (CloneNotSupportedException e) { 
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }
  
  /**
   * Returns an array containing all of the elements in this list
   * in the correct order.
   *
   * @return an array containing all of the elements in this list
   * 	       in the correct order.
   */
  public Object[] toArray() {
    Object[] result = new Object[size];
    System.arraycopy(elementData, 0, result, 0, size);
    return result;
  }
  
  /**
   * Returns an array containing all of the elements in this list in the
   * correct order.  The runtime type of the returned array is that of the
   * specified array.  If the list fits in the specified array, it is
   * returned therein.  Otherwise, a new array is allocated with the runtime
   * type of the specified array and the size of this list.<p>
   *
   * If the list fits in the specified array with room to spare (i.e., the
   * array has more elements than the list), the element in the array
   * immediately following the end of the collection is set to
   * <tt>null</tt>.  This is useful in determining the length of the list
   * <i>only</i> if the caller knows that the list does not contain any
   * <tt>null</tt> elements.
   *
   * @param a the array into which the elements of the list are to
   *		be stored, if it is big enough; otherwise, a new array of the
   * 		same runtime type is allocated for this purpose.
   * @return an array containing the elements of the list.
   * @throws ArrayStoreException if the runtime type of a is not a supertype
   *         of the runtime type of every element in this list.
   */
  public Object[] toArray(Object a[]) {
    if (a.length < size)
      a = (Object[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), 
                                                        size);
    
    System.arraycopy(elementData, 0, a, 0, size);
    
    if (a.length > size)
      a[size] = null;
    
    return a;
  }
  
  // Positional Access Operations
  
  /**
   * Returns the element at the specified position in this list.
   *
   * @param  index index of element to return.
   * @return the element at the specified position in this list.
   * @throws    IndexOutOfBoundsException if index is out of range <tt>(index
   * 		  &lt; 0 || index &gt;= size())</tt>.
   */
  public Object get(int index) {
    RangeCheck(index);
    
    return elementData[index];
  }
  
  /**
   * Replaces the element at the specified position in this list with
   * the specified element.
   *
   * @param index index of element to replace.
   * @param element element to be stored at the specified position.
   * @return the element previously at the specified position.
   * @throws    IndexOutOfBoundsException if index out of range
   *		  <tt>(index &lt; 0 || index &gt;= size())</tt>.
   */
  public Object set(int index, Object element) {
    RangeCheck(index);
    
    Object oldValue = elementData[index];
    elementData[index] = element;
    return oldValue;
  }
  
  /**
   * Appends the specified element to the end of this list.
   *
   * @param o element to be appended to this list.
   * @return <tt>true</tt> (as per the general contract of Collection.add).
   */
  public boolean add(Object o) {
    ensureCapacity(size + 1);  // Increments modCount!!
    elementData[size++] = o;
    return true;
  }
  
  /**
   * Inserts the specified element at the specified position in this
   * list. Shifts the element currently at that position (if any) and
   * any subsequent elements to the right (adds one to their indices).
   *
   * @param index index at which the specified element is to be inserted.
   * @param element element to be inserted.
   * @throws    IndexOutOfBoundsException if index is out of range
   *		  <tt>(index &lt; 0 || index &gt; size())</tt>.
   */
  public void add(int index, Object element) {
    if (index > size || index < 0)
      throw new IndexOutOfBoundsException(
                                          "Index: "+index+", Size: "+size);
    
    ensureCapacity(size+1);  // Increments modCount!!
    System.arraycopy(elementData, index, elementData, index + 1,
                     size - index);
    elementData[index] = element;
    size++;
  }
  
  /**
   * Removes a single instance of the specified element from this
   * collection, if it is present (optional operation).  More formally,
   * removes an element <tt>e</tt> such that <tt>(o==null ? e==null :
   * o.equals(e))</tt>, if the collection contains one or more such
   * elements.  Returns <tt>true</tt> if the collection contained the
   * specified element (or equivalently, if the collection changed as a
   * result of the call).<p>
   *
   * This implementation walks the elements of the collection looking for the
   * specified element.  If it finds the element, it removes the element
   * from the collection.<p>
   *
   * @param o element to be removed from this collection, if present.
   * @return <tt>true</tt> if the collection contained the specified
   *         element.
   * 
   */
  public boolean remove(Object o) {
    int removeIndex = -1;

    if (o == null) {
      for (int i = 0; i < size; i++) {
        if (elementData[i] == null) {
          removeIndex = i;
          break;
        }
      }
    } else {
      for (int i = 0; i < size; i++) {
        if (elementData[i].equals(o)) {
          removeIndex = i;
          break;
        }
      }
    }

    if (removeIndex != -1) {
      remove(removeIndex);
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Removes the element at the specified position in this list.
   * Shifts any subsequent elements to the left (subtracts one from their
   * indices).
   *
   * @param index the index of the element to removed.
   * @return the element that was removed from the list.
   * @throws    IndexOutOfBoundsException if index out of range <tt>(index
   * 		  &lt; 0 || index &gt;= size())</tt>.
   */
  public Object remove(int index) {
    RangeCheck(index);
    
    modCount++;
    Object oldValue = elementData[index];
    
    int numMoved = size - index - 1;
    if (numMoved > 0)
      System.arraycopy(elementData, index+1, elementData, index,
                       numMoved);
    elementData[--size] = null; // Let gc do its work
    
    return oldValue;
  }
  
  /**
   * Removes all of the elements from this list.  The list will
   * be empty after this call returns.
   */
  public void clear() {
    modCount++;
    
    // Let gc do its work
    for (int i = 0; i < size; i++)
      elementData[i] = null;
    
    size = 0;
  }

  /**
   * Appends all of the elements in the specified Collection to the end of
   * this list, in the order that they are returned by the
   * specified Collection's Iterator.  The behavior of this operation is
   * undefined if the specified Collection is modified while the operation
   * is in progress.  (This implies that the behavior of this call is
   * undefined if the specified Collection is this list, and this
   * list is nonempty.)
   *
   * @param index index at which to insert first element
   *			  from the specified collection.
   * @param c elements to be inserted into this list.
   * @throws    IndexOutOfBoundsException if index out of range <tt>(index
   *		  &lt; 0 || index &gt; size())</tt>.
   */
  public boolean addAll(Collection c) {
    modCount++;
    int numNew = c.size();
    ensureCapacity(size + numNew);
    
    if (c instanceof List) {
      // Use List access if we can
      List list = (List)c;
      for (int i=0; i<numNew; i++)
        elementData[size++] = list.get(i);
    } else {
      Iterator e = c.iterator();
      for (int i=0; i<numNew; i++)
        elementData[size++] = e.next();
    }


    return numNew != 0;
  }
  
  /**
   * Inserts all of the elements in the specified Collection into this
   * list, starting at the specified position.  Shifts the element
   * currently at that position (if any) and any subsequent elements to
   * the right (increases their indices).  The new elements will appear
   * in the list in the order that they are returned by the
   * specified Collection's iterator.
   *
   * @param index index at which to insert first element
   *		    from the specified collection.
   * @param c elements to be inserted into this list.
   * @throws    IndexOutOfBoundsException if index out of range <tt>(index
   *		  &lt; 0 || index &gt; size())</tt>.
   */
  public boolean addAll(int index, Collection c) {
    if (index > size || index < 0)
      throw new IndexOutOfBoundsException(
                                          "Index: "+index+", Size: "+size);
    
    int numNew = c.size();
    ensureCapacity(size + numNew);  // Increments modCount!!
    
    int numMoved = size - index;
    if (numMoved > 0)
      System.arraycopy(elementData, index, elementData, index + numNew,
                       numMoved);
    
    if (c instanceof List) {
      // Use List access if we can
      List list = (List)c;
      for (int i=0; i<numNew; i++)
        elementData[index++] = list.get(i);
    } else {
      Iterator e = c.iterator();
      for (int i=0; i<numNew; i++)
        elementData[index++] = e.next();
    }

    size += numNew;
    return numNew != 0;
  }
  
  /**
   * Removes from this collection all of its elements that are contained in
   * the specified collection (optional operation).<p>
   *
   * This implementation walks the elements of this collection, checking each
   * element in turn to see if it's contained in the specified collection.  If 
   * it's so contained, it's removed from this collection.<p>
   *
   *
   * @param c elements to be removed from this collection.
   * @return <tt>true</tt> if this collection changed as a result of the
   * call.
   * 
   * @see #remove(Object)
   * @see #contains(Object)
   */
  public boolean removeAll(Collection c) {
    boolean modified = false;

    for (int i = 0; i < size; i++) {
      if (c.contains(elementData[i])) {
        remove(i);
        modified = true;
      }
    }
    return modified;
  }

  /**
   * Retains only the elements in this collection that are contained in the
   * specified collection (optional operation).  In other words, removes
   * from this collection all of its elements that are not contained in the
   * specified collection. <p>
   *
   * This implementation walks the elements of this collection, checking each
   * element returned in turn to see if it's contained in the specified 
   * collection.  If it's not so contained, it's removed from this collection.
   *
   * @return <tt>true</tt> if this collection changed as a result of the
   *         call.
   * 
   * @see #remove(Object)
   * @see #contains(Object)
   */
  public boolean retainAll(Collection c) {
    boolean modified = false;

    for (int i = 0; i < size; i++) {
      if (!c.contains(elementData[i])) {
        remove(i);
        modified = true;
      }
    }

    return modified;
  }

  /**
   * Returns a string representation of this collection.  The string
   * representation consists of a list of the collection's elements, 
   * enclosed in square brackets (<tt>"[]"</tt>).  Adjacent elements are 
   * separated by the characters <tt>", "</tt> (comma and space).  Elements 
   * are converted to strings as by <tt>String.valueOf(Object)</tt>.<p>
   *
   * This implementation creates an empty string buffer, appends a left
   * square bracket, and walks the elements of the collection appending the string
   * representation of each element in turn.  After appending each element
   * except the last, the string <tt>", "</tt> is appended.  Finally a right
   * bracket is appended.  A string is obtained from the string buffer, and
   * returned.
   * 
   * @return a string representation of this collection.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    for (int i = 0; i < size; i++) {
      buf.append(String.valueOf(elementData[i]));
      if (i < (size - 1)) 
        buf.append(", ");
    }
    buf.append("]");
    return buf.toString();
  }

  // Comparison and hashing
  
  /**
   * Compares the specified object with this list for equality.  Returns
   * <tt>true</tt> if and only if the specified object is also a list, both
   * lists have the same size, and all corresponding pairs of elements in
   * the two lists are <i>equal</i>.  (Two elements <tt>e1</tt> and
   * <tt>e2</tt> are <i>equal</i> if <tt>(e1==null ? e2==null :
   * e1.equals(e2))</tt>.)  In other words, two lists are defined to be
   * equal if they contain the same elements in the same order.<p>
   *
   * This implementation first checks if the specified object is this
   * list. If so, it returns <tt>true</tt>; if not, it checks if the
   * specified object is a list. If not, it returns <tt>false</tt>; if so,
   * it iterates over both lists, comparing corresponding pairs of elements.
   * If any comparison returns <tt>false</tt>, this method returns
   * <tt>false</tt>.  If either iterator runs out of elements before the
   * other it returns <tt>false</tt> (as the lists are of unequal length);
   * otherwise it returns <tt>true</tt> when the iterations complete.
   *
   * @param o the object to be compared for equality with this list.
   * 
   * @return <tt>true</tt> if the specified object is equal to this list.
   */
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof List))
      return false;
    
    List otherList = (List) o;

    if (size != otherList.size()) {
      return false;
    }

    for (int i = 0; i < size; i++) {
      Object otherElement = otherList.get(i);
      if (!(elementData[i] == null ? 
            otherElement == null : elementData[i].equals(otherElement)))
        return false;
    }
    return true;
  }
  
  /**
   * Returns the hash code value for this list. <p>
   *
   * This implementation uses exactly the code that is used to define the
   * list hash function in the documentation for the <tt>List.hashCode</tt>
   * method.
   *
   * @return the hash code value for this list.
   */
  public int hashCode() {
    int hashCode = 1;
    for (int i = 0; i < size; i++) {
      hashCode = 31*hashCode + (elementData[i] == null ? 
                                0 : elementData[i].hashCode());
    }
    return hashCode;
  }
  
  /**
   * Removes from this List all of the elements whose index is between
   * fromIndex, inclusive and toIndex, exclusive.  Shifts any succeeding
   * elements to the left (reduces their index).
   * This call shortens the list by <tt>(toIndex - fromIndex)</tt> elements.
   * (If <tt>toIndex==fromIndex</tt>, this operation has no effect.)
   *
   * @param fromIndex index of first element to be removed.
   * @param fromIndex index after last element to be removed.
   */
  protected void removeRange(int fromIndex, int toIndex) {
    modCount++;
    int numMoved = size - toIndex;
    System.arraycopy(elementData, toIndex, elementData, fromIndex,
                     numMoved);
    
    // Let gc do its work
    int newSize = size - (toIndex-fromIndex);
    while (size != newSize)
      elementData[--size] = null;
  }
  

  /**
   * Check if the given index is in range.  If not, throw an appropriate
   * runtime exception.
   */
    private void RangeCheck(int index) {
      if (index >= size || index < 0)
        throw new IndexOutOfBoundsException(
                                            "Index: "+index+", Size: "+size);
    }
  
  /**
   * Save the state of the <tt>ArrayListFoundation</tt> instance to a stream (that
   * is, serialize it). We need to be careful to not hold our monitor
   * while serializing subobjects. Doing so is prone to deadlock. So
   * make a copy of the elementData so it can't change while
   * serialization is happening.
   *
   * @serialData The length of the array backing the <tt>ArrayListFoundation</tt>
   *             instance is emitted (int), followed by all of its elements
   *             (each an <tt>Object</tt>) in the proper order.
   */
  private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException{
    // Write out any hidden stuff
    s.defaultWriteObject();
    int sizeToWrite;
    int lengthToWrite;
    Object[] objectsToWrite;
    synchronized (this) {
      sizeToWrite = size;
      lengthToWrite = elementData.length;
      objectsToWrite = new Object[sizeToWrite];
      System.arraycopy(elementData, 0, objectsToWrite, 0, sizeToWrite);
    }
    // Write out the current size;
    s.writeInt(sizeToWrite);
    // Write out array length
    s.writeInt(lengthToWrite);
    // Write out all elements in the proper order.
    for (int i = 0; i < sizeToWrite; i++)
      s.writeObject(objectsToWrite[i]);
  }
  
  /**
   * Reconstitute the <tt>ArrayListFoundation</tt> instance from a stream (that is,
   * deserialize it).
   */
  private synchronized void readObject(java.io.ObjectInputStream s)
    throws java.io.IOException, ClassNotFoundException {
    // Read in size, and any hidden stuff
    s.defaultReadObject();
    size = s.readInt();
    
    // Read in array length and allocate array
    int arrayLength = s.readInt();
    elementData = new Object[arrayLength];
    
    // Read in all elements in the proper order.
    for (int i=0; i<size; i++)
      elementData[i] = s.readObject();
  }
}














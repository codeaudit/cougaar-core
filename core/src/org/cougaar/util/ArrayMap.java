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
 * An implementation of <code>Map</code> that maintains the elements in 
 * the order they were added, as opposed to the random order of a 
 * <code>HashMap</code>.
 *
 * An ArrayMap is a Map, and can be used exactly like a typical Map.  
 * The most significant differences are that<ol>
 *   <li>The elements are kept in the order that they are added.</li>
 *   <li>One can restrict the (key, value) types by overriding
 *       <tt>createEntry(Object key, Object value)</tt>.</li>
 *   <li>There are index-based "getters", such as <tt>getKey(int)</tt>.
 * </ol>
 *
 * @see java.util.Map
 */
public class ArrayMap 
  implements Map, Cloneable, java.io.Serializable {

  /**
   * Use an <code>ArrayList</code> to hold the <code>Map.Entry</code>
   * elements.
   *
   * This could be optimized for lookup speed, but a simple ArrayList 
   * should be fine for our needs.
   */
  private final ArrayList l;

  public ArrayMap(int initialCapacity) {
    l = new ArrayList(initialCapacity);
  }

  public ArrayMap() {
    this(10);
  }

  public ArrayMap(Map t) {
    this((110*t.size())/100); // Allow 10% room for growth
    putAll(t);
  }

  /**
   * Create a new <code>Map.Entry</code>.
   *
   * @see ArrayMap.ArrayEntry
   */
  protected Map.Entry createEntry(
      final Object key, 
      final Object value) {
    // create the standard implementation of Map.Entry
    return new ArrayEntry(key, value);
  }

  /**
   * Helper utility for <tt>createEntry</tt> to see if an 
   * <code>Object</code> represents a wrapped Java primitive:</ul>
   *   <li><code>Boolean</code></li>
   *   <li><code>Character</code></li>
   *   <li><code>Byte</code></li>
   *   <li><code>Short</code></li>
   *   <li><code>Integer</code></li>
   *   <li><code>Long</code></li>
   *   <li><code>Float</code></li>
   *   <li><code>Double</code></li>
   *   <li><code>Void</code></li>
   * </ul>.
   *
   * @return true if the given Object is a wrapped Java primitive
   */
  public static final boolean isWrappedPrimitive(final Object o) {
    // must be better ways to do this!  maybe a HashMap of Classes?
    return
      ((o instanceof Boolean) ||
       (o instanceof Integer) ||
       (o instanceof Long) ||
       (o instanceof Double) ||
       (o instanceof Float) ||
       (o instanceof Character) ||
       (o instanceof Byte) ||
       (o instanceof Short) ||
       (o instanceof Void));
  }

  public int size() {
    return l.size();
  }

  public boolean isEmpty() {
    return l.isEmpty();
  }

  public boolean containsKey(Object key) {
    // search for key
    int n = l.size();
    if (key == null) {
      for (int i = 0; i < n; i++) {
        Map.Entry meI = (Map.Entry)l.get(i);
        if (meI.getKey() == null) {
          // found matching entry
          return true;
        }
      }
    } else {
      for (int i = 0; i < n; i++) {
        Map.Entry meI = (Map.Entry)l.get(i);
        if (key.equals(meI.getKey())) {
          // found matching entry
          return true;
        }
      }
    }
    // no such entry
    return false;
  }

  public boolean containsValue(Object value) {
    // search for key
    int n = l.size();
    if (value == null) {
      for (int i = 0; i < n; i++) {
        Map.Entry meI = (Map.Entry)l.get(i);
        if (meI.getValue() == null) { 
          // found matching entry
          return true;
        }
      }
    } else {
      for (int i = 0; i < n; i++) {
        Map.Entry meI = (Map.Entry)l.get(i);
        if (value.equals(meI.getValue())) { 
          // found matching entry
          return true;
        }
      }
    }
    // no such entry
    return false;
  }

  /**
   * Get the value for the specified key.
   *
   * @see #containsKey(Object) especially if <tt>isValidValue</tt> allows
   *  <tt>null</tt>.
   *
   * @see #getValue(int) for a indexed lookup
   */
  public Object get(Object key) {
    // search for key
    int n = l.size();
    if (key == null) {
      for (int i = 0; i < n; i++) {
        Map.Entry meI = (Map.Entry)l.get(i);
        if (meI.getKey() == null) {
          // found matching entry
          //
          // note that value can be null -- use "containsKey(key)"
          //   to distinguish these cases
          return meI.getValue();
        }
      }
    } else {
      for (int i = 0; i < n; i++) {
        Map.Entry meI = (Map.Entry)l.get(i);
        if (key.equals(meI.getKey())) {
          // found matching entry
          //
          return meI.getValue();
        }
      }
    }
    // no such entry
    return null;
  }

  /**
   * Simple utility from <code>java.util.Properties</code>.
   *
   * Note that
   *
   * @see #getValue(int) for a indexed lookup
   */
  public Object get(Object key, Object defaultValue) {
    Object val = get(key);
    return ((val != null) ? val : defaultValue);
  }

  public Object put(Object key, Object value) {
    // see if key is already listed
    int n = l.size();
    for (int i = 0; i < n; i++) {
      Map.Entry meI = (Map.Entry)l.get(i);
      if (key.equals(meI.getKey())) {
        // found matching entry, replace value
        return meI.setValue(value);
      }
    }
    // create a new entry
    Map.Entry newME = createEntry(key, value);
    l.add(newME);
    // no old value
    return null;
  }

  public Object remove(Object key) {
    // search for key
    int n = l.size();
    if (key == null) {
      for (int i = 0; i < n; i++) {
        Map.Entry meI = (Map.Entry)l.get(i);
        if (meI.getKey() == null) {
          // found matching entry
          //
          // note that value can be null -- use "containsKey(key)"
          //   to distinguish these cases
          l.remove(i);
          return meI.getValue();
        }
      }
    } else {
      for (int i = 0; i < n; i++) {
        Map.Entry meI = (Map.Entry)l.get(i);
        if (key.equals(meI.getKey())) {
          // found matching entry
          //
          // note that value can be null -- use "containsKey(key)"
          //   to distinguish these cases
          l.remove(i);
          return meI.getValue();
        }
      }
    }
    // no such entry
    return null;
  }

  public Object clone() {
    try { 
      return super.clone();
    } catch (CloneNotSupportedException e) { 
      // this shouldn't happen, since we are Cloneable
      throw new InternalError();
    }
  }

  //
  // Lots of code taken from HashMap
  //

  public void putAll(Map t) {
    Iterator i = t.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry e = (Map.Entry) i.next();
      put(e.getKey(), e.getValue());
    }
  }

  public void clear() {
    l.clear();
  }

  private transient Set keySet = null;
  private transient Set entrySet = null;
  private transient Collection values = null;

  public Set keySet() {
    if (keySet == null) {
      keySet = new AbstractSet() {
        public Iterator iterator() {
          final Iterator lIter = l.iterator();
          return new Iterator() {
            public boolean hasNext() {
              return lIter.hasNext();
            }
            public Object next() {
              return ((Map.Entry)lIter.next()).getKey();
            }
            public void remove() {
              lIter.remove();
            }
          };
        }
        public int size() {
          return l.size();
        }
        public boolean contains(Object o) {
          return containsKey(o);
        }
        public boolean remove(Object o) {
          return ArrayMap.this.remove(o) != null;
        }
        public void clear() {
          ArrayMap.this.clear();
        }
      };
    }
    return keySet;
  }

  public Collection values() {
    if (values==null) {
      values = new AbstractCollection() {
        public Iterator iterator() {
          final Iterator lIter = l.iterator();
          return new Iterator() {
            public boolean hasNext() {
              return lIter.hasNext();
            }
            public Object next() {
              return ((ArrayEntry)lIter.next()).getValue();
            }
            public void remove() {
              lIter.remove();
            }
          };
        }
        public int size() {
          return l.size();
        }
        public boolean contains(Object o) {
          return containsValue(o);
        }
        public void clear() {
          ArrayMap.this.clear();
        }
      };
    }
    return values;
  }

  public Set entrySet() {
    if (entrySet==null) {
      entrySet = new AbstractSet() {
        public Iterator iterator() {
          return l.iterator();
        }

        public boolean contains(Object o) {
          if (o instanceof Map.Entry) {
            int n = l.size();
            for (int i = 0; i < n; i++) {
              if (o.equals(l.get(i))) {
                return true;
              }
            }
          }
          return false;
        }

        public boolean remove(Object o) {
          if (o instanceof Map.Entry) {
            int n = l.size();
            for (int i = 0; i < n; i++) {
              if (o.equals(l.get(i))) {
                l.remove(i);
                return true;
              }
            }
          }
          return false;
        }

        public int size() {
          return l.size();
        }

        public void clear() {
          ArrayMap.this.clear();
        }
      };
    }

    return entrySet;
  }

  //
  // Some List methods that might be useful
  //

  public void trimToSize() {
    l.trimToSize();
  }

  /**
   * Get the <code>Map.Entry</code> at the specifed offset of the
   * <tt>entrySet().iterator()</tt>.
   *
   * <code>Map</code>s typically don't define an indexable ordering.
   * However, an <code>ArrayMap</code> is defined to be sorted in the
   * order that the elements were added, so let's make this function 
   * available.
   */
  public Map.Entry get(int index) {
    return (Map.Entry)l.get(index);
  }

  /**
   * @see #get(int)
   */
  public Object getKey(int index) {
    return get(index).getKey();
  }

  /**
   * @see #get(int)
   */
  public Object getValue(int index) {
    return get(index).getValue();
  }

  //
  // Other List methods could be added, but let's limit it to "get*(int)"
  // for now.
  //

  /**
   * Simple <code>Map.Entry</code> implementation.
   *
   * Subclasses of <code>ArrayMap</code> can subclass this implementation,
   * restrict the (key, value)s, and override <tt>ArrayMap.createEntry</tt>.
   * For example, one could force String keys and values by overriding
   * the constructor and <tt>setValue(Object)</tt>.
   */
  protected static class ArrayEntry 
    implements Map.Entry, Cloneable, java.io.Serializable {

      public final Object key;
      public Object value;

      public ArrayEntry(final Object key, final Object value) {
        this.key = key;
        this.value = value;
      }

      public Object getKey() {
        return key;
      }

      public Object getValue() {
        return value;
      }

      public Object setValue(final Object newValue) {
        Object oldValue = this.value;
        this.value = newValue;
        return oldValue;
      }

      public Object clone() {
        try { 
          return super.clone();
        } catch (CloneNotSupportedException e) { 
          // this shouldn't happen, since we are Cloneable
          throw new InternalError();
        }
      }

      public boolean equals(final Object o) {
        if (o == this) {
          return true;
        } else if (o instanceof Map.Entry) {
          Map.Entry me = (Map.Entry)o;
          return
            (((key != null) ?
              (key.equals(me.getKey())) :
              (me.getKey() == null)) &&
             ((value != null) ?
              (value.equals(me.getValue())) :
              (me.getValue() == null)));
        } else {
          return false;
        }
      }

      public int hashCode() {
        return 
          (((key != null) ? key.hashCode() : 0) ^
           ((value != null) ? value.hashCode() : 0));
      }

      public String toString() {
        return key+"="+value;
      }
    };

  public String toString() {
    int n = l.size();
    if (n > 0) {
      StringBuffer buf = new StringBuffer();
      buf.append("{");
      buf.append(l.get(0));
      for (int i = 1; i < n; i++) {
        buf.append(", ");
        buf.append(l.get(i));
      }
      buf.append("}");
      return buf.toString();
    } else {
      return "{}";
    }
  }

  private static final long serialVersionUID = 782934727772928381L;
}


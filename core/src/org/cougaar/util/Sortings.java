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

/** 
 * Sortings is a set of utility methods which provide simple 
 * and/or fixed sorting functionality to JDK Collections Framework.
 *
 * The names of the methods are intended to echo CommonLisp functions
 * with similar functionality.
 *
 * This is functionality which should be part of Java Collections framework
 * but is sadly missing.
 */

public final class Sortings {

  /** Similar functionality to java.util.Collections.sort, except with 
   * several important extensions:  the collection is sorted
   * into a new Collection (actually an ArrayList), all Collections are
   * sortable (not just Lists), and a key function may be specified, so 
   * that the elements are compared based on the results of the key rather
   * than on the elements themselves.
   * If Collection is a List, and the key is null, this is equivalent to
   * something like Collections.sort(new ArrayList(collection), comparator);
   *
   * Currently, the key mapping may be evaluated multiple times for each element. 
   * A better implementation would evaluate the keys exactly once into an array
   * and then sort the keys and objects at the same time.
   */
  public static Collection sort(Collection collection, final Comparator comparator, final Mapping key) {
    List l = new ArrayList(collection);
    Comparator rcomp = comparator;
    if (key != null) {
      rcomp = new Comparator() {
          public final int compare(Object o1, Object o2) {
            return comparator.compare(key.map(o1), key.map(o2));
          }
        };
    }
    Collections.sort(l, rcomp);
    return l;
  }

  /** alias for sort(collection,comparator,null); **/
  public static Collection sort(Collection collection, Comparator comparator) {
    return sort(collection, comparator, null);
  }
  /** Like sort(collection, comparator, key) where comparator
   * is defined as using the Comparable interface.  Key may or may not
   * be null.  If key is non-null, the resulting objects of applying the
   * key mapping must be Comparable.
   **/
  public static Collection sort(Collection collection, final Mapping key) {
    Comparator comp;
    if (key == null) {
      comp = new Comparator() {
          public final int compare(Object o1, Object o2) {
            return ((Comparable)o1).compareTo(o2);
          }
        };
    } else {
      comp = new Comparator() {
          public final int compare(Object o1, Object o2) {
            return ((Comparable)(key.map(o1))).compareTo(key.map(o2));
          }
        };
    }      
    return sort(collection, comp, null);
  }

  /** Like Collections.sort except non-destructive **/
  public static Collection sort(Collection collection) {
    List l = new ArrayList(collection);
    Collections.sort(l);
    return l;
  }

  //////
  // destructive versions
  //////

  /** destructive version of sort(Collection,Comparator,key).  collection must
   * implement the optional retainAll Collections framework method.
   * @return the (modified) collection
   **/
  public static Collection sortInPlace(Collection collection, Comparator comparator, Mapping key) {
    Collection sorted = sort(collection, comparator, key);
    collection.retainAll(sorted);
    return collection;
  }

  /** destructive version of sort(Collection, Comparator).  collection must
   * implement the optional retainAll Collections framework method.
   * @return the (modified) collection
   **/
  public static Collection sortInPlace(Collection collection, Comparator comparator) {
    Collection sorted = sort(collection, comparator);
    collection.retainAll(sorted);
    return collection;
  }
  /** destructive version of sort(Collection, Mapping).  collection must
   * implement the optional retainAll Collections framework method.
   * @return the (modified) collection
   **/
  public static Collection sortInPlace(Collection collection, final Mapping key) {
    Collection sorted = sort(collection, key);
    collection.retainAll(sorted);
    return collection;
  }

  /** destructive version of sort(Collection).  collection must
   * implement the optional retainAll Collections framework method.
   * @return the (modified) collection
   **/
  public static Collection sortInPlace(Collection collection) {
    Collection sorted = sort(collection);
    collection.retainAll(sorted);
    return collection;
  }

  

}
    
    

/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

/** 
 * Mappings is a class of static methods for transforming Collections via
 * mapping functions.
 *
 * The names of the methods are intended to echo CommonLisp functions
 * with similar functionality.
 *
 * This is functionality which should be part of Java Collections framework
 * but is sadly missing.
 */

package org.cougaar.util;

import java.util.*;

public final class Mappings {

  /** Compute an element-by-element map of the input collection.
   * Essentially, this means constructing a new Collection which is
   * contains the results of applying the Mapping to each element
   * of the input collection.
   *
   * The Mapping may not return null or an IllegalArgumentException will
   * be thrown (use mapcan instead).
   *
   * @return a Collection representing the mapped collection.
   * @param result a Collection to add elements to.  If null, defaults to 
   * an new ArrayList.
   **/
  public static Collection mapcar(Mapping m, Collection input, Collection result) {
    if (result==null) 
      result = new ArrayList(input.size());

    for (Iterator i = input.iterator(); i.hasNext(); ) {
      Object e = i.next();
      Object me = m.map(e);
      if (me == null)
        throw new IllegalArgumentException("mapcar Mapping "+m+
                                           " returned null when applied to"+
                                           e);
      result.add(me);
    }
    return result;
  }

  /** equivalent to mapcar(Mapping m, Collection c, null) which 
   * implies that the return value will be a new ArrayList.
   */
  public static Collection mapcar(Mapping m, Collection c) {
    return mapcar(m,c,null);
  }

  /** Compute an element-to-n-element map of the input collection.
   * Essentially, this means constructing a new Collection which is
   * a concatenation of the resulting collections resulting from the
   * application of the Mapping to each element of the input collection.
   *
   * In general, the Mapping should return a Collection of zero or more
   * elements to be added to the result map.
   *
   * As a convenience, the Mapping may also return null, which is equivalent
   * to returning an empty collection, or a Non-Collection, which is equivalent
   * to returning a Collection with one element.
   *
   * @return an ArrayList representing the mapped collection.
   * @param result a Collection to add elements to.
   **/
  public static Collection mapcan(Mapping m, Collection input, Collection result) {
    for (Iterator i = input.iterator(); i.hasNext(); ) {
      Object e = i.next();
      Object me = m.map(e);
      if (me != null) {
        if (me instanceof Collection) {
          result.addAll((Collection)me);
        } else {
          result.add(me);
        }
      }
    }
    return result;
  }

  /** equivalent to mapcan(Mapping m, Collection c, null) which 
   * implies that the return value will be a new ArrayList.
   */
  public static Collection mapcan(Mapping m, Collection c) {
    return mapcan(m,c,null);
  }

}

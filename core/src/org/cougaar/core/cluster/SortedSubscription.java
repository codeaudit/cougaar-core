/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster;

import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Add sorted add list to IncrementalSubscription.
 **/

public class SortedSubscription extends IncrementalSubscription {

  /**
   * Check that the underlying Collection is either a List (so it can
   * be sorted on demand) or a SortedSet so it is intrinsically
   * sorted.
   * @param c the Collection to check
   * @exception an IllegalArgumentException if the collection cannot
   * be sorted.
   * @return the Collection
   **/
  private static Collection verifyList(Collection c) {
    if (! (c instanceof List) &&
        ! (c instanceof SortedSet)) {
      throw new IllegalArgumentException("Collection is not a List");
    }
    return c;
  }

  public SortedSubscription(UnaryPredicate p, Collection c) {
    super(p, verifyList(c));
    isAlwaysSorted = (c instanceof SortedSet);
  }

  private boolean isSorted = true;
  private boolean isAlwaysSorted;

  /**
   * Override to make the added set be a SortedSet.
   **/
  protected Set createAddedSet() {
    return new TreeSet();
  }

  /**
   * Override to mark the underlying collection as unsorted.
   **/
  protected void privateAdd(Object o, boolean isVisible) {
    isSorted = isAlwaysSorted;
    super.privateAdd(o, isVisible);
  }

  /**
   * Override to mark the underlying collection as unsorted.
   **/
  protected void privateChange(Object o, List changes, boolean isVisible) {
    isSorted = isAlwaysSorted;
    super.privateChange(o, changes, isVisible);
  }

  /**
   * Override to insure that the collection is sorted before returning
   **/
  public Enumeration elements() {
    if (!isSorted) {
      Collections.sort((List) real);
      isSorted = true;
    }
    return super.elements();
  }
}

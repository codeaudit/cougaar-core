/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.blackboard;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cougaar.util.UnaryPredicate;

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

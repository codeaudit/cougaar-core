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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cougaar.util.Empty;
import org.cougaar.util.Enumerator;
import org.cougaar.util.UnaryPredicate;

/** Add tracking of incremental changes to the container.
 *
 **/

public class IncrementalSubscription extends CollectionSubscription {

  public IncrementalSubscription(UnaryPredicate p, Collection c) {
    super(p, c);
  }

  private Set myAddedSet = null; 
  private List myRemovedList = null;

  protected void resetChanges() {
    super.resetChanges();
    if (myAddedSet != null) 
      myAddedSet.clear();
    if (myRemovedList != null)
      myRemovedList.clear();
  }


  /** @return an list of the objects of the collection that have been added.
   **/
  public Enumeration getAddedList() {
    checkTransactionOK("getAddedList()");
    if (myAddedSet == null || myAddedSet.isEmpty()) 
      return Empty.enumeration;
    return new Enumerator(myAddedSet);
  }

  /** @return a possibly empty collection of objects added since 
   * the last transaction. Will not return null.
   **/
  public Collection getAddedCollection() {
    return (myAddedSet!=null)?myAddedSet:Collections.EMPTY_SET;
  }

  /** @return an list of the objects of the collection that have been removed.
   **/

  public Enumeration getRemovedList() {
    checkTransactionOK("getRemovedList()");
    if (myRemovedList == null || myRemovedList.isEmpty())
      return Empty.enumeration;
    return new Enumerator(myRemovedList);
  }

  /** @return a possibly empty collection of objects removed 
   * since the last transaction.  Will not return null.
   **/
  public Collection getRemovedCollection() {
    return (myRemovedList!=null)?myRemovedList:Collections.EMPTY_LIST;
  }

  /** @return an list of the objects of the collection that have 
   * been marked as changed.
   **/
  public Enumeration getChangedList() {
    checkTransactionOK("getChangedList()");
    return super.privateGetChangedList();
  }

  /** @return a possibly empty collection of objects marked as changed
   * since the last transaction. Will not return null.
   **/
  public Collection getChangedCollection() {
    return super.privateGetChangedCollection();
  }

  /**
   * Override this for sorted sets
   **/
  protected Set createAddedSet() {
    return new HashSet(5);
  }

  /** called by privateAdd **/
  private void addToAddedList( Object o ) {
    if (myAddedSet == null) myAddedSet = createAddedSet();
    myAddedSet.add(o);
  }
  /** called by privateRemove **/
  private void addToRemovedList( Object o ) {
    if (myRemovedList == null) myRemovedList = new ArrayList(3);
    myRemovedList.add( o );
  }

  protected void privateAdd(Object o, boolean isVisible) { 
    super.privateAdd(o, isVisible);
    if (isVisible) {
      setChanged(true);
      addToAddedList(o);
    }
  }
  protected void privateRemove(Object o, boolean isVisible) {
    super.privateRemove(o, isVisible);
    if (isVisible) {
      setChanged(true);
      addToRemovedList(o);
    }
  }

  protected void privateChange(Object o, List changes, boolean isVisible) {
    if (isVisible) {
      setChanged(true);
      super.privateChange(o, changes, true);
    }
  }
}

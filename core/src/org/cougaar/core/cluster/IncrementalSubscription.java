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

import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;
import org.cougaar.domain.planning.ldm.plan.*;

/** Add tracking of incremental changes to the container.
 *
 **/

public class IncrementalSubscription extends CollectionSubscription {

  public IncrementalSubscription(UnaryPredicate p, Collection c) {
    super(p, c);
  }

  private Set myAddedSet = null; 
  private List myRemovedList = null;
  private List myChangedList = null;

  protected void resetChanges() {
    super.resetChanges();
    if (myAddedSet != null) 
      myAddedSet.clear();
    if (myRemovedList != null)
      myRemovedList.clear();
    if (myChangedList != null)
      myChangedList.clear();
  }


  /** @return an list of the objects of the collection that have been added.
   **/
  public Enumeration getAddedList() {
    subscriber.checkTransactionOK("getAddedList()");
    if (myAddedSet == null || myAddedSet.isEmpty()) 
      return Empty.enumeration;
    return new Enumerator(myAddedSet);
  }

  public Collection getAddedCollection() {
    return myAddedSet;
  }

  /** @return an list of the objects of the collection that have been removed.
   **/

  public Enumeration getRemovedList() {
    subscriber.checkTransactionOK("getRemovedList()");
    if (myRemovedList == null || myRemovedList.isEmpty())
      return Empty.enumeration;
    return new Enumerator(myRemovedList);
  }

  public Collection getRemovedCollection() {
    return myRemovedList;
  }

  /** @return an list of the objects of the collection that have 
   * been marked as changed.
   **/
  public Enumeration getChangedList() {
    subscriber.checkTransactionOK("getChangedList()");
    if (myChangedList == null || myChangedList.isEmpty())
      return Empty.enumeration;
    return new Enumerator(myChangedList);
  }

  public Collection getChangedCollection() {
    return myChangedList;
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
  /** called by privateChange **/
  private void addToChangedList( Object o ) {
    if (myChangedList == null) myChangedList = new ArrayList(3);
    myChangedList.add( o );
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
    super.privateChange(o, changes, isVisible);
    if (isVisible) {
      setChanged(true);
      addToChangedList(o);
    }
  }
}

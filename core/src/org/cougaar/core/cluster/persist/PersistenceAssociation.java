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
package org.cougaar.core.cluster.persist;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import org.cougaar.core.cluster.Envelope;

/**
 * This class establishes an association between an object that has
 * been persisted and a reference number. It also captures the state
 * of the object.
 **/

public class PersistenceAssociation extends WeakReference {

  /**
   * The id assigned to the object. This is id is used to replace the
   * object when its actual value is not significant.
   **/
  private PersistenceReference referenceId;

  /**
   * Records if the object has not yet been removed from the
   * plan. In a persistence delta, this boolean does not include the
   * effect of envelopes that have not yet been distributed.
   **/
  private static final int NEW      = 0;
  private static final int ACTIVE   = 1;
  private static final int INACTIVE = 2;
  private int active = NEW;
  /**
   * marked is used to identify associations having objects that need
   * to be written to the current delta to avoid double writing those
   * objects.
   **/
  private boolean marked = false;

  /**
   * The hashcode of the object. See IdentityTable.
   **/
  int hash;

  /**
   * Chain of associations in IdentityTable.
   **/
  PersistenceAssociation next;  // Chain of PersistenceAssociations in hash table

  PersistenceAssociation(Object object, int id, ReferenceQueue refQ) {
    this(object, new PersistenceReference(id), refQ);
  }

  PersistenceAssociation(Object object, PersistenceReference id, ReferenceQueue refQ) {
    super(object, refQ);
    if (id == null) throw new IllegalArgumentException("Null PersistenceReference");
    if (object == null) throw new IllegalArgumentException("Null Object");
    referenceId = id;
    hash = System.identityHashCode(object); // Get this now before the object disappears
  }

  public Object getObject() {
    return get();
  }

  public PersistenceReference getReferenceId() {
    return referenceId;
  }

  /**
   * A mark used for various purposes. During a persist operation, the
   * mark serves to identify associations that are being written to
   * this persistence delta. During rehydration, the mark is used to
   * identify associations that were restored from a particular delta.
   **/
  public void setMarked(boolean newMarked) {
    marked = newMarked;
  }

  public boolean isMarked() {
    return marked;
  }

  public void setActive() {
    if (active == NEW) active = ACTIVE;
  }

  public void setActive(int newActive) {
    if (newActive > active) active = newActive;
  }

  public void setInactive() {
    active = INACTIVE;
  }

  public boolean isActive() {
    return active == ACTIVE;
  }

  public int getActive() {
    return active;
  }

  public boolean isNew() {
    return active == NEW;
  }

  public boolean isInactive() {
    return active == INACTIVE;
  }

  public String toString() {
    String activity;
    switch (active) {
    default:       activity = " 0"; break;
    case ACTIVE:   activity = " +"; break;
    case INACTIVE: activity = " -"; break;
    }
    return BasePersistence.hc(getObject()) + activity + " @ " + referenceId;
  }
}

/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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

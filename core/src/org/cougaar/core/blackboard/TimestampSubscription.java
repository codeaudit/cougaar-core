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
package org.cougaar.core.blackboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.UnaryPredicate;

/**
 * A TimestampSubscription is an Subscription that tracks
 * UniqueObject creation (ADD) and most recent modification
 * (CHANGE) timestamps.
 * <p>
 * These timestamps are not persisted, and upon rehydration the
 * creation time of the objects will be the agent restart time.
 * 
 * @see Subscriber required system property that must be enabled
 * @see #getTimestampEntry access to the (UID, TimestampEntry) timestamp data
 */
public class TimestampSubscription 
extends Subscription 
{

  // a map if (UID, TimestampEntry) pairs
  //
  // the map is locked to allow multiple reader threads to
  // access the "get*(UID)" methods.
  private final Map map;

  // the "apply(..)" timestamp from the transaction close time
  // of the TimestampedEnvelope that is being processed.
  //
  // only one "apply(..)" can occur at a time, so this is thread 
  // safe.
  private long time;

  /**
   * @param p the predicate should only accept UniqueObjects; 
   *    all non-UniqueObjects and UniqueObjects with null UIDs 
   *    are ignored.
   */
  public TimestampSubscription(UnaryPredicate p) {
    super(p);
    map = new HashMap(13);
  }

  //
  // all the methods from Subscription are provided.
  //

  /**
   * @see #getTimestampEntry get the creation time
   */
  public long getCreationTime(UID uid) {
    TimestampEntry entry = getTimestampEntry(uid);
    return 
      ((entry != null) ? 
       entry.getCreationTime() : 
       TimestampEntry.UNKNOWN_TIME);
  }

  /**
   * @see #getTimestampEntry get the modification time
   */
  public long getModificationTime(UID uid) {
    TimestampEntry entry = getTimestampEntry(uid);
    return 
      ((entry != null) ? 
       entry.getModificationTime() : 
       TimestampEntry.UNKNOWN_TIME);
  }

  /**
   * Get the TimestampEntry for the local blackboard object with the 
   * given UID.
   * <p>
   * The object must match this subscription's predicate.
   * <p>
   * The timestamps are measured in milliseconds, and matches the
   * transaction close times of the subscriber that performed
   * the "publishAdd()" or "publishChange()".
   * <p>
   * This method is thread-safe to allow multiple clients to
   * access the underlying (UID, TimestampEntry) map.  The map is
   * also updated <i>during</i> the subscriber's transaction.  
   * Multiple calls to "getTimestampEntry()", even within the same
   * subscriber transaction, may return different results.
   *
   * @return the TimestampEntry, or null if not known.
   */
  public TimestampEntry getTimestampEntry(UID uid) {
    boolean todd = 
      ((uid != null) && 
       (uid.getOwner().equals("widget")) ||
       (uid.getOwner().equals("junk")));
    if (todd) System.out.println("GET "+uid);
    synchronized (map) {
      if (todd) System.out.println("  ret) "+map.get(uid));
      return (TimestampEntry) map.get(uid);
    }
  }

  protected void privateAdd(Object o, boolean isVisible) {
    boolean todd = 
      (o instanceof org.cougaar.core.TestTimestampsPlugin.Test);
    if (todd) System.out.println("ADD "+o);
    if (todd) System.out.println("  a) "+o+" ("+time+")");
    // always fill in the map, even if (!isVisible)
    if (o instanceof UniqueObject) {
      if (todd) System.out.println("  b) "+o);
      UID uid = ((UniqueObject) o).getUID();
      if (uid != null) {
        if (todd) System.out.println("  c) "+o);
        TimestampEntry entry = new TimestampEntry(time, time);
        synchronized (map) {
          if (todd) System.out.println("  put) "+o+", "+entry);
          map.put(uid, entry);
        }
      }
    }
    if (todd) System.out.println("  e) "+o);
  }

  protected void privateChange(Object o, List changes, boolean isVisible) {
    if (o instanceof UniqueObject) {
      UID uid = ((UniqueObject) o).getUID();
      if (uid != null) {
        TimestampEntry newEntry = new TimestampEntry(time, time);
        synchronized (map) {
          TimestampEntry prevEntry = (TimestampEntry) map.put(uid, newEntry);
          if (prevEntry != null) {
            // typical case.  replace an existing entry.
            long creationTime = prevEntry.getCreationTime();
            // assert (creationTime <= time);
            //
            // this "private_*" call saves us an extra "map.get(..)".
            // it is safe only within this "map.put(..)" situation
            newEntry.private_setCreationTime(creationTime);
          }
        }
      }
    }
  }

  protected void privateRemove(Object o, boolean isVisible) {
    if (o instanceof UniqueObject) {
      UID uid = ((UniqueObject) o).getUID();
      // FIXME remove without a trace?
      synchronized (map) {
        map.remove(uid);
      }
    }
  }

  public boolean apply(Envelope envelope) {
    if (envelope instanceof TimestampedEnvelope) {
      TimestampedEnvelope te = (TimestampedEnvelope) envelope;
      long closeTime = te.getTransactionCloseTime();
      if (closeTime != TimestampEntry.UNKNOWN_TIME) {
        this.time = closeTime;
        return super.apply(envelope);
      }
    }
    // FIXME should we still "apply(..)" with the current time?
    return false;
  }

}

/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp.resolver;

import java.io.Serializable;

import org.cougaar.core.util.UID;
import org.cougaar.core.wp.Timestamp;

/**
 * A data response from a successful LookupService lookup,
 * which validates the client's cached Record and extends
 * the TTL for the cached data.
 * <p>
 * This is only used if the client passed a non-null UID in
 * the lookup.
 * <p>
 * The client sent the UID of the Record, and this "RecordIsValid"
 * confirms that the Record with that UID hasn't changed and
 * permits the client to cache the Record for a little longer.
 * <p>
 * It's possible for a client to evict the entry before the
 * "record is valid" is received, e.g. due to a cache size limit.
 * In this situation the client should send a new lookup (null-UID),
 * which will incur a second lookup delay.
 */
public final class RecordIsValid implements Serializable {

  private final UID uid;
  private final long ttd;

  public RecordIsValid(UID uid, long ttd) {
    this.uid = uid;
    this.ttd = ttd;
    // validate
    String s =
      ((uid == null) ? "null uid" :
       (ttd < 0) ? "negative ttd" :
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  /**
   * The UID of the validated Record.
   */
  public UID getUID() {
    return uid;
  }

  /**
   * The expiration "time-to-death" relative to the base timestamp.
   */
  public long getTTD() {
    return ttd;
  }

  public String toString() {
    return "(record-is-valid uid="+uid+" ttd="+ttd+")";
  }

  public String toString(long baseTime, long now) {
    long ttl = baseTime + ttd;
    return 
      "(record-is-valid uid="+uid+
      " ttd="+ttd+
      " ttl="+Timestamp.toString(ttl, now)+
      ")";
  }
}

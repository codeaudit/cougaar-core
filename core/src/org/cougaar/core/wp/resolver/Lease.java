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
 * An "successful lease" response from the ModifyService,
 * indicating that either a new Record was successfully bound
 * or an existing Lease was extended.
 * <p>
 * The client must renew this lease before it expires, otherwise
 * the server(s) will automatically remove it.
 * <p>
 * Renewals can pass the UID of the Record, as documented in
 * the ModifyService.
 * <p>
 * A relative "TTD" for the expiration data is also specified,
 * which is relative to the base time passed by the
 * "modifyAnswer" method.
 */
public final class Lease implements Serializable {

  private final UID uid;
  private final long ttd;

  public Lease(UID uid, long ttd) {
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
   * The UID of the lease, as selected by the View.
   * <p>
   * This is the "in response to" field.
   */
  public UID getUID() {
    return uid;
  }

  /**
   * The lease time-to-death relative to the base timestamp.
   * <p>
   * This is the lease expiration date.  A negative number
   * indicates a failed bind or renewal, in which case the
   * result field will be non-null.
   */
  public long getTTD() {
    return ttd;
  }

  public String toString() {
    return "(lease uid="+uid+" ttd="+ttd+")";
  }

  public String toString(long baseTime, long now) {
    long ttl = baseTime + ttd;
    return 
      "(lease uid="+uid+
      " ttd="+ttd+
      " ttl="+(0 < ttl ? Timestamp.toString(ttl, now) : "N/A")+
      ")";
  }
}

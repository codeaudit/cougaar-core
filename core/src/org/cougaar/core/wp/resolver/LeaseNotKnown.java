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

/**
 * An "unknown lease uid" response from the ModifyService,
 * indicating that an attempt to renew a lease failed because
 * the server doesn't know a lease with the specified UID.
 * <p>
 * If a client attempts to renew a Lease by passing the UID, and
 * the server doesn't know the UID, then this response is
 * returned to request the full Record of data.
 * <p>
 * This is used to cover two cases:<ol>
 *   <li>The server expired the lease due to lack of renewal
 *       (e.g. network partition) and the client must remind
 *       the server of the data</li>
 *   <li>The server has crashed and must be reconciled.</li>
 * </ol>
 * <p>
 * The Record should use the same UID as the Lease.  This will
 * ensure that duplicate or out-of-order messages will not
 * cause problems.
 */
public final class LeaseNotKnown implements Serializable {

  private final UID uid;

  public LeaseNotKnown(UID uid) {
    this.uid = uid;
    // validate
    String s = 
      ((uid == null) ? "null uid" :
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  public UID getUID() {
    return uid;
  }

  public String toString() {
    return "(lease-not-known uid="+uid+")";
  }
}

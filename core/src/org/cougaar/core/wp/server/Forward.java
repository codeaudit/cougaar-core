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

package org.cougaar.core.wp.server;

import java.io.Serializable;
import org.cougaar.core.util.UID;
import org.cougaar.core.wp.resolver.Lease;
import org.cougaar.core.wp.resolver.Record;

/**
 */
public final class Forward implements Serializable {

  private final Lease lease;
  private final Record record;

  public Forward(
      Lease lease,
      Record record) {
    this.lease = lease;
    this.record = record;
    // validate
    String s =
      (lease == null ? "null lease" :
       (record != null &&
        !record.getUID().equals(lease.getUID())) ?
       "record "+record+" doesn't match lease "+lease :
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  /**
   * The non-null lease.
   */
  public Lease getLease() {
    return lease;
  }

  /**
   * The record, which may be null.
   *
   * If the record is null then the lease was renewed based upon
   * the UID.  If the recipient doesn't know the matching record
   * then it should send back a LeaseNotKnown response.
   */
  public Record getRecord() {
    return record;
  }

  public String toString() {
    return "(forward lease="+lease+" record="+record+")";
  }
}

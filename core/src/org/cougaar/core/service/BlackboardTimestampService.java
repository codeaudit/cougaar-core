/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

package org.cougaar.core.service;

import org.cougaar.core.blackboard.TimestampEntry;
import org.cougaar.core.component.Service;
import org.cougaar.core.util.UID;

/** 
 * The blackboard timestamp service tracks UniqueObject creation 
 * (ADD) and most recent modification (CHANGE) timestamps.
 * <p>
 * These timestamps are not persisted, and upon rehydration the
 * creation time of the objects will be the agent restart time.
 *
 * @see org.cougaar.core.blackboard.TimestampSubscription
 */
public interface BlackboardTimestampService extends Service {

  /**
   * @see #getTimestampEntry get the creation time
   */
  long getCreationTime(UID uid);

  /**
   * @see #getTimestampEntry get the modification time
   */
  long getModificationTime(UID uid);

  /**
   * Get the TimestampEntry for the local blackboard UniqueObject 
   * with the given UID.
   * <p>
   * The timestamps are measured in milliseconds, and matches the
   * transaction close times of the blackboard subscriber that 
   * performed the "publishAdd()" or "publishChange()".
   * <p>
   * The underlying (UID, TimestampEntry) map is dynamically 
   * maintained by a separate subscriber.  These methods are 
   * thread safe.  Clients should be aware that multiple calls
   * to "getTimestampEntry(..)" may return different responses.
   * <p>
   * The service provider of this service may restrict the set
   * of UniqueObjects covered by this service.  UniqueObjects
   * that are not covered have null TimestampEntry values.
   *
   * @return the TimestampEntry for the UniqueObject with the 
   *    specified UID, or null if not known.
   */
  TimestampEntry getTimestampEntry(UID uid);

}

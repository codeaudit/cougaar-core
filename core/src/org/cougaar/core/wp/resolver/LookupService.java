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

import java.util.Map;

import org.cougaar.core.component.Service;

/**
 * This is the lookup transport layer of the white pages client,
 * which is used by the white pages cache.
 * <p>
 * This API hides the MTS and messaging details.  In particular,
 * the transport selects which WP server(s) the lookups should
 * be sent to, aggregates results if necessary, and retries any
 * failed deliveries.
 * <p>
 * The service requestor must implement the Client API.
 */
public interface LookupService extends Service {

  /**
   * Lookup entries in the server(s).
   * <p>
   * The map is from String keys to UIDs.  The key is the "getAll"
   * name or "list" suffix.  If the client has a cached Record
   * from a prior lookup then it can pass the UID of that Record
   * to request a RecordIsValid response, otherwise it should pass
   * a null UID.  If the current record's UID doesn't match
   * the UID then a full Record will be sent back.
   * <p>
   * It's fine to submit a singleton map, but for efficiency a
   * client can use this API to batch requests.
   */
  void lookup(Map m);

  /**
   * The service API that must be implemented by the requestor
   * of this service.
   */
  interface Client {
    /**
     * Respond to a lookup request.
     * <p>
     * The map is from String keys to either Record or RecordIsValid
     * objects.
     * <p>
     * RecordIsValid objects are used for lookups that specified a
     * UID to validate a cached Record, otherwise Record objects
     * are used to provide the full data (e.g. the AddressEntries
     * for a "getAll" lookup).
     * <p>
     * The values contain relative timestamp offsets for the
     * cache expiration time (e.g. "+5000 millis"), so the baseTime
     * is also specified.
     */
    void lookupAnswer(long baseTime, Map m);
  }
}

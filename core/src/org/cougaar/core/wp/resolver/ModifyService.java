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
 * This is the modify transport layer of the white pages client,
 * which is used by the white pages lease manager.
 * <p>
 * This API hides the MTS and messaging details.  In particular,
 * the transport selects which WP server(s) the modifications should
 * be sent to, aggregates results if necessary, and retries any
 * failed deliveries.
 * <p>
 * The service requestor must implement the Client API.
 */
public interface ModifyService extends Service {

  /**
   * Bind or renew leased entries in the server(s).
   * <p>
   * The map keys are Strings for the name of the entry (e.g.
   * "AgentX").  The value can be either a Record, UID, or a
   * NameTag that wraps either a Record or UID.  Records are
   * used for new leases and UIDs are used for lease renewals.
   * <p>
   * The NameTag specifies the agent that's requesting the action.
   * If a NameTag is not used then the agent is the local
   * node-agent. 
   * <p>
   * Lease renewal just sends the UID of the most recent
   * version.  If the server's UID for the name matches the
   * sent UID then the server extends the Lease and tells the
   * client the new expiration time.  If the server doesn't know
   * the UID then the client will be told that the lease is not
   * known and it should send the full Record.  If a conflicting
   * UID is in place then the server will either tell the client
   * that the Lease renewal is denied or act as if the lease is
   * not known and request the full Record object for further
   * deconfliction analysis.
   * <p>
   * It's fine to submit a singleton map, but for efficiency a
   * client can use this API to batch requests.
   */
  void modify(Map m);

  /**
   * The service API that must be implemented by the requestor
   * of this service.
   */
  interface Client {
    /**
     * Receive the answer to a modify request.
     * <p>
     * The map is from String keys to objects, which are of type
     * Lease, LeaseNotKnown, or LeaseDenied.
     * <p>
     * The values in the map may use relative timestamp offsets
     * (e.g. "+5000 millis"), so the baseTime is also specified.
     */
    void modifyAnswer(long baseTime, Map m);
  }
}

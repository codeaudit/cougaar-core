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

import java.util.Map;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * This is the forwarding (replicate) transport layer of the white
 * pages server, which is used to broadcast client modifications.
 * <p>
 * This API hides the MTS and messaging details.  In particular,
 * the transport selects which WP server(s) the lookups should
 * be sent to, aggregates results if necessary, and retries any
 * failed deliveries.
 * <p>
 * The service requestor must implement the Client API.
 */
public interface ForwardService extends Service {

  /**
   * Forward entries to all our peer servers.
   * <p>
   * The map keys are Strings and the values are Forward objects.
   * <p>
   * Like the ModifyService, the "forwardAnswer" can send back a
   * LeaseNotKnown response if the UID is not known (e.g. due to a
   * server restart), in which case the server should send the full
   * Record.  A LeaseDenied is never sent because this is typically
   * a race condition -- the remote server's "forward" is likely
   * on the wire and will correct the sender's tables.  Lastly, a
   * success Lease is not necessary since a lack of acknowledgement
   * is assumed to be an acceptance of the forwarded data.
   * <p>
   * Clients may see temporary inconsistencies due to propagation 
   * delays, but these should be minimal.  Races between clients
   * are resolved by deconflicting the entries based upon the
   * optional "version" entries in each record.
   * <p>
   * Larger inconsistencies may occur due to network partitions or
   * server crashes.  These conflicts are eventually remedied with
   * the server-side deconfliction code and periodic lease
   * renewals.  Additionally, as an optional optimization, a server
   * can detect another server's crash and forward a full copy of
   * its data to that server, by using the other "forward" method.
   * <p>
   * It's fine to submit a singleton map, but for efficiency a
   * client can use this API to batch requests.
   */
  void forward(Map m, long ttd);

  /**
   * Reply to a "forwardAnswer" LeaseNotKnown by sending a Forward
   * to our peer.
   */
  void forward(MessageAddress target, Map m, long ttd);

  /**
   * The service API that must be implemented by the requestor
   * of this service.
   */
  interface Client {
    /**
     * Receive the answer to a forward request.
     */
    void forwardAnswer(
        MessageAddress addr,
        long baseTime,
        Map m);
  }
}

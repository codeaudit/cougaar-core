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

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * This is the "server selection service" that the client uses to
 * select a white pages server.
 * <p>
 * The primary job of this service is to manage the list of servers
 * and rank them based upon the {@link #update} feedback from
 * the clients and policy.
 * <p>
 * Design goals:<br>
 *   We want to prefer servers that are working, but we also want
 * to occasionally select a server that we haven't tried recently
 * (to explore our options).  Our solution must balance across
 * the servers, since we're sharing the servers with other clients.
 * Also, we don't want to change servers too rapidly, since this
 * may expose minor discrepancies due to server replication delays.
 * <p> 
 * This may be extended to segregate servers, e.g.<ul>
 *   <li>read-only servers</li> 
 *   <li>segregation based upon name (e.g. chord hashing)</li> 
 *   <li>hierarchical servers</li> 
 * </ul>
 */
public interface SelectService extends Service {

  /**
   * Select a server for the specified name.
   * <p>
   * For example, this could be a lookup for the record of "AgentX".
   * A server from the pool is selected and returned, or null is
   * returned if no applicable server is listed.
   */
  MessageAddress select(
      boolean lookup,
      String name);

  /**
   * Update the server list with measured performance statistics.
   * <p>
   * If the timeout flag is false then the duration is the measured
   * round-trip-time for the message response, otherwise the
   * duration is the timeout duration.
   */
  void update(
     MessageAddress addr,
     long duration,
     boolean timeout);

  /**
   * @return true if the address is a listed white pages server
   */
  boolean contains(MessageAddress addr);

  interface Client {
    /**
     * The set of servers has changed, either with added or removed
     * entries, so the client should scan their pending messages
     * to see if the target is contained in the server's list.
     */
    void onChange();
  }
}

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
 * This is the "modify" transport layer of the white pages server.
 * <p>
 * This API mirrors the client's ModifyService.
 * <p>
 * The clientAddr is the address of the client that sent
 * the modify message, which is where the response message
 * will be sent.
 * <p>
 * The clientTime is the time that the client sent it's request.
 * Answer messages sent back to the client are tagged with both
 * the client's time and the server's time, which allows
 * the client to measure the round-trip-time and correct for
 * any clock drift.
 * <p>
 * This API hides the MTS and messaging details.
 * <p>
 * The service requestor must implement the Client API.
 *
 * @see org.cougaar.core.wp.resolver.ModifyService
 */
public interface ModifyAckService extends Service {

  /**
   * Acknowledge a client's "modify" request.
   */
  void modifyAnswer(
      MessageAddress clientAddr,
      long clientTime,
      Map m);

  /**
   * The service API that must be implemented by the requestor
   * of this service.
   */
  interface Client {
    /**
     * Handle a client's modify request.
     * <p>
     * The server should call the "modifyAnswer" method.
     */
    void modify(
        MessageAddress clientAddr,
        long clientTime,
        Map m);
  }
}

/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.service.wp;

import java.io.Serializable;

import org.cougaar.core.component.Service;

/**
 * This service is used by the white pages client and server to
 * protect and verify requests.
 * <p>
 * The component that advertises this service is optional.  If
 * it is not loaded in the node then wrapping is disabled. 
 * <p>
 * The node's white pages lease manager will wrap each agent's
 * request.  Multiple requests may be batched into a message
 * sent by the node to the white pages server.  The server
 * receives the message and unwraps the batched requests.
 */
public interface WhitePagesProtectionService extends Service {

  /**
   * Client method to wrap a request.
   * <p> 
   * For example, this may sign the request and wrap it with the
   * certificate chain used for signing.
   *
   * @param agent - The agent making the request
   * @param request - the request object
   * @return the wrapped request object
   * @throws Exception if the request can't be wrapped and the
   *   client must fail the request
   */
  Wrapper wrap(String agent, Object request) throws Exception;

  /**
   * Server method to unwrap a client's wrapper.
   * <p>
   * For example, this may install and verify the signing
   * certificate.
   *
   * @param agent - The agent making the request
   * @param request - the request object
   * @return the request object 
   * @throws Exception if the request can't be wrapped and the
   *   server must ignore the request
   */
  Object unwrap(String agent, Wrapper w) throws Exception;

  /**
   * Marker interface for a wrapper.
   */
  interface Wrapper extends Serializable {
  }
}

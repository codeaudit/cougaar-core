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
import org.cougaar.core.service.wp.Response;

/**
 * This class watches for bind/unbind requests and maintains
 * the leases in the server.
 */
public interface LeaseService extends Service {

  /**
   * Submit a request to the lease manager.
   * <p>
   * The requests include "bind" and "unbind".  The lease manager
   * fully handles these requests, either by answering them
   * immediately based upon pending/active leases or by sending
   * remote lease requests.
   * <p>
   * The lease manager is also told about "get" operations, to
   * work around version number lookup bug 2839.
   *
   * @param agent the agent making the request 
   */
  void submit(Response res, String agent);
 
}

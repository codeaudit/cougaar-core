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
 * This is the service API for the white pages client-side cache.
 * <p>
 * The Resolver makes calls into the CacheService.  The cache
 * should use the LookupService to send remote lookups and
 * receive Records.
 * <p>
 * Features:
 * <ul>
 *   <li>Holds "get", "getAll", and "list" results</li>
 *   <li>Allows the client to add bootstrapped cache "hints"</li>
 *   <li>Holds both positive and negative results</li>
 *   <li>Evicts expired and least-recently-used entries</li>
 *   <li>Renewals (in the background) recently-used entries that
 *       will soon expire</li>
 *   <li>Allows the client to flush and force-renewal entries
 *       that are known to be stale</li>
 *   <li>Upgrades "get" requests to "getAll" requests, to reduce
 *       server traffic</li>
 * </ul>
 * <p>
 * The cache doesn't manage "bind/unbind" leases; that's the job of
 * the LeaseService.
 */
public interface CacheService extends Service {

  /**
   * Submit a request to the cache.
   * <p>
   * The requests include "get", "getAll", "list", and "flush",
   * plus the cache-only bind operations of "hint" and "unhint".
   * The cache fully handles these requests, either by answering
   * them immediately based upon cached data or by sending
   * lookups.
   * <p>
   * The cache is also told about "bind" and "unbind" operations,
   * to flush the cache.
   */
  void submit(Response res);

}

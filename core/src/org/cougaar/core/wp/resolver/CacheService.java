/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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

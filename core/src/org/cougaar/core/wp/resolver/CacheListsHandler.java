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

import java.util.Collections;
import java.util.Set;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.util.LRUExpireMap;
import org.cougaar.util.log.Logger;

/**
 * This cache holds "list" results, including negative
 * caching, and supports internal ttl/lru gc.
 */
public class CacheListsHandler
extends HandlerBase
{

  private CacheListsConfig config;

  private final Object lock = new Object();

  // Map<String, Response.List.Result>
  private LRUExpireMap lists;

  public void setParameter(Object o) {
    this.config = new CacheListsConfig(o);
  }

  public void load() {
    lists = new LRUExpireMap(
        new MyCacheConfig(),              // my config
        new LoggingCacheWatcher(logger)); // my logger

    super.load();

    if (config.cleanCachePeriod > 0) {
      scheduleRestart(config.cleanCachePeriod);
    }
  }

  /** @return true if handled by the cached */
  protected Response mySubmit(Response res) {
    Request req = res.getRequest();
    if (!(req instanceof Request.List)) {
      // ignore -- we only handle lists
      return res;
    }
    boolean bypass = !req.useCache();
    String suffix = ((Request.List) req).getSuffix();

    Object ret = fetch(suffix, bypass);
    boolean isHit = (ret != null);
    if (isHit) {
      // found cached value
      res.setResult(ret, -1);
    }
    return res;
  }

  protected void myExecute(
      Request req,
      Object result,
      long ttl) {
    if (req instanceof Request.List) {
      String suffix = ((Request.List) req).getSuffix();
      update(suffix, result, ttl);
    }
  }

  //
  // look in the cache:
  //

  private Set fetch(String suffix, boolean bypass) {
    Set names;
    synchronized (lock) {
      names = (Set) lists.get(suffix, bypass);
    }
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Cache "+(names == null ? "MISS" : "HIT")+
          " for (suffix="+suffix+", bypass="+bypass+")");
    }
    return names;
  }

  //
  // update the cache:
  //

  private void update(
      String suffix,
      Object result,
      long ttl) {
    // examine ttl
    long now = System.currentTimeMillis();
    long maxTTL = now + config.maxTTD;
    if (maxTTL < ttl) {
      // reduce ttl if too long
      if (logger.isInfoEnabled()) {
        logger.info(
            "Reduce ttl from "+
            Timestamp.toString(ttl,now)+
            " to "+
            Timestamp.toString(maxTTL,now));
      }
      ttl = maxTTL;
    }
    if (ttl < now) {
      // ignore expired update
      if (logger.isInfoEnabled()) {
        logger.info(
            "Ignoring expired update (ttl="+
            Timestamp.toString(ttl,now)+
            " < now="+
            now+"), suffix="+
            suffix+", result="+
            result);
      }
      return;
    }

    if (result instanceof Set) {
      Set names = (Set) result; // immutable?
      listed(suffix, names, ttl);
    } else if (result == null) {
      Set names = Collections.EMPTY_SET;
      listed(suffix, names, ttl);
    } else {
      // failure?
    }
  }

  private void listed(String suffix, Set names, long ttl) {
    if (logger.isInfoEnabled()) {
      logger.info(
          "Caching list(suffix="+suffix+
          ", ttl="+Timestamp.toString(ttl)+
          ", names="+names+")");
    }
    synchronized (lock) {
      lists.put(suffix, names, ttl);
    }
  }

  protected void myRun() {
    cleanCache();
    // run me again later
    scheduleRestart(config.cleanCachePeriod);
  }

  // this is optional, since the LRU will clean
  private void cleanCache() {
    synchronized (lock) {
      lists.trim();
    }
  }

  /** config options, soon to be parameters/props */
  private static class CacheListsConfig {
    public final long cleanCachePeriod = 30*1000;
    public final long maxTTD;
    public final long minBypass;
    public final int initSize = 16;
    public final int maxSize;

    public CacheListsConfig(Object o) {
      // FIXME parse!
      maxTTD =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.resolver.cacheLists.maxTTD",
              "60000"));
      minBypass =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.resolver.cacheLists.minBypass",
              "10000"));
      maxSize =
        Integer.parseInt(
            System.getProperty(
              "org.cougaar.core.wp.resolver.cacheLists.maxSize",
              "100"));
    }
  }

  /** my config for the LRUExpireMap */
  private class MyCacheConfig
    implements LRUExpireMap.Config {
      public int initialSize() {
        return config.initSize;
      }
      public int maxSize() {
        return config.maxSize;
      }
      public long minBypassTime() {
        return config.minBypass;
      }
    }
}

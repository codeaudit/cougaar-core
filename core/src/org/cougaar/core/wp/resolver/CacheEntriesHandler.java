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
import java.util.HashMap;
import java.util.Map;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.util.LRUExpireMap;
import org.cougaar.util.log.Logger;

/**
 * This cache holds "get" and "getAll" results, including negative
 * caching, and supports internal ttl/lru gc.
 * <p>
 * This implementation accepts a client-side "fetch" for any type of
 * request, but (as a design simplification) does not support
 * responses of type "get".  It's expected that the BatchHandler
 * will upgrade all "get" requests to equivalent "getAll" requests.
 */
public class CacheEntriesHandler
extends HandlerBase
{
  private CacheEntriesConfig config;

  private final Object lock = new Object();

  // name -> (type -> entry)
  // Map<String, Map<String, AddressEntry>>
  private LRUExpireMap entries;

  public void setParameter(Object o) {
    this.config = new CacheEntriesConfig(o);
  }

  public void load() {
    entries = new LRUExpireMap(
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
    Object ret = fetch(req);
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
    update(req, result, ttl);
  }

  //
  // look in the cache:
  //

  private Object fetch(Request req) {
    boolean bypass = !req.useCache();
    Object ret;
    if (req instanceof Request.Get) {
      Request.Get rg = (Request.Get) req;
      String n = rg.getName();
      ret = getAll(n, bypass);
    } else if (req instanceof Request.GetAll) {
      String n = ((Request.GetAll) req).getName();
      ret = getAll(n, bypass);
    } else if (req instanceof Request.List) {
      // ignore
      ret = null;
    } else if (req instanceof Request.Bind) {
      Request.Bind rb = (Request.Bind) req;
      AddressEntry ae = rb.getAddressEntry();
      boolean overwrite = rb.isOverWrite();
      // assert(bypass);
      // ignore renewal flag
      ret = bind(ae, overwrite);
    } else if (req instanceof Request.Unbind) {
      Request.Unbind ru = (Request.Unbind) req;
      AddressEntry ae = ru.getAddressEntry();
      // assert(bypass);
      ret = unbind(ae);
    } else {
      throw new IllegalArgumentException("Unknown action");
    }
    return ret;
  }

  private Map getAll(String n, boolean bypass) {
    Map m;
    synchronized (lock) {
      m = (Map) entries.get(n, bypass);
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Cache "+(m == null ? "MISS" : "HIT")+
            " for (name="+n+", bypass="+bypass+")");
      }
    }
    return m;
  }

  private Object bind(AddressEntry ae, boolean overwrite) {
    String n = ae.getName();
    synchronized (lock) {
      // clear entry
      if (logger.isInfoEnabled()) {
        if (entries.containsKey(n)) {
          Map oldMap = (Map) entries.get(n);
          if (oldMap != null) {
            long ttl = entries.getExpirationTime(n);
            logger.info(
                "Removing cache entry on bind("+ae+
                "), ttl="+Timestamp.toString(ttl)+
                ", entries="+oldMap+")");
          }
        }
      } else {
        entries.remove(n);
      }
    }
    return null;
  }

  private Object unbind(AddressEntry ae) {
    String n = ae.getName();
    synchronized (lock) {
      // clear entry
      entries.remove(n);
    }
    return null;
  }

  //
  // update the cache:
  //

  private void update(
      Request req,
      Object result,
      long ttl) {
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
            now+"), req="+
            req+", result="+
            result);
      }
      return;
    }
    if (req instanceof Request.Get) {
      // Not supported for a design simplification.
      //
      // The ClientBatch should upgrade all "get" requests
      // to "getAll".
      logger.error(
          "This cache doesn't support \"get\" responses,"+
          " expecting a full \"getAll\": "+req);
    } else if (req instanceof Request.GetAll) {
      Request.GetAll rga = (Request.GetAll) req;
      String n = rga.getName();
      if (result instanceof Map) {
        Map m = (Map) result; // immutable?
        gotAll(n, m, ttl);
      } else if (result == null) {
        Map m = Collections.EMPTY_MAP;
        gotAll(n, m, ttl);
      } else {
        // failure?
      }
    } else if (req instanceof Request.List) {
      // ignore
    } else if (req instanceof Request.Bind) {
      Request.Bind rb = (Request.Bind) req;
      AddressEntry ae = rb.getAddressEntry();
      if (result instanceof Long) {
        bound(ae, ttl);
      } else {
        // failure?
      }
    } else if (req instanceof Request.Unbind) {
      Request.Unbind ru = (Request.Unbind) req;
      AddressEntry ae = ru.getAddressEntry();
      if (result instanceof Boolean) {
        unbound(ae, ttl);
      } else {
        // failure?
      }
    } else {
      // failure?
    }
  }

  private void gotAll(String n, Map m, long ttl) {
    synchronized (lock) {
      if (logger.isInfoEnabled()) {
        logger.info(
            "Caching gotAll(name="+n+
            ", ttl="+Timestamp.toString(ttl)+
            ", entries="+m+")");
      }
      entries.put(n, m, ttl);
    }
  }

  private void bound(
      AddressEntry ae, long ttl) {
    bound(ae, ae, ttl);
  }

  private void unbound(
      AddressEntry ae, long ttl) {
    bound(ae, null, ttl);
  }

  private void bound(
      AddressEntry oldAE,
      AddressEntry ae,
      long ttl) {
    // we've bound something, so modify our cache to match our bind
    //
    // we only fix our cache entry if it exists, and we'll be
    // conservative and use the minimum TTL
    //
    // this is a bit awkward, but it's better than keeping an entry
    // that we know to be stale or dropping the entry altogether
    String n = oldAE.getName();
    synchronized (lock) {
      Map oldMap = (Map) entries.get(n);
      if (oldMap != null) {
        long oldTTL = entries.getExpirationTime(n);
        // reduce the ttl to match
        long newTTL = Math.min(ttl, oldTTL);
        // replace the entry
        Map newMap = new HashMap(oldMap);
        String type = oldAE.getType();
        newMap.put(type, ae);
        newMap = Collections.unmodifiableMap(newMap);
        entries.put(n, newMap, newTTL);
      }
    }
  }

  protected void myRun() {
    cleanCache();
    // run me again later
    scheduleRestart(config.cleanCachePeriod);
  }

  // this is optional since the LRU will clean itself
  private void cleanCache() {
    synchronized (lock) {
      entries.trim();
    }
  }

  /** config options, soon to be parameters/props */
  private static class CacheEntriesConfig {
    public final long cleanCachePeriod = 30*1000;
    public final long maxTTD;
    public final long minBypass;
    public final int initSize = 16;
    public final int maxSize;

    public CacheEntriesConfig(Object o) {
      // FIXME parse!
      maxTTD =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.resolver.cacheEntries.maxTTD",
              "60000"));
      minBypass =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.resolver.cacheEntries.minBypass",
              "10000"));
      maxSize =
        Integer.parseInt(
            System.getProperty(
              "org.cougaar.core.wp.resolver.cacheEntries.maxSize",
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

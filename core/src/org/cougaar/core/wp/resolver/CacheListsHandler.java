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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.wp.Scheduled;
import org.cougaar.core.wp.SchedulableWrapper;
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
  private LRUExpireMap cache;

  // map request to queued responses
  // name -> MissEntry
  // Map<String, MissEntry>
  private final Map misses = new HashMap(13);

  //
  // clean the cache:
  //

  private SchedulableWrapper cleanCacheThread;

  public void setParameter(Object o) {
    this.config = new CacheListsConfig(o);
  }

  public void load() {
    cache = new LRUExpireMap(
        new MyCacheConfig(),              // my config
        new LoggingCacheWatcher(logger)); // my logger

    super.load();

    if (config.cleanCachePeriod > 0) {
      // create expiration timer
      Scheduled cleanCacheRunner =
        new Scheduled() {
          public void run(SchedulableWrapper thread) {
            cleanCache(thread);
          }
        };
      cleanCacheThread = SchedulableWrapper.getThread(
          threadService,
          cleanCacheRunner,
          "White pages server clean lists cache");
      cleanCacheThread.schedule(config.cleanCachePeriod);
    }
  }

  /** @return true if handled by the cached */
  protected Response mySubmit(Response res) {
    Request req = res.getRequest();
    if (req instanceof Request.List) {
      synchronized (lock) {
        return fetch(res, (Request.List) req);
      }
    } else {
      return res;
    }
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

  private Response fetch(Response res, Request.List req) {
    String suffix = req.getSuffix();
    boolean bypass = req.hasOption(Request.BYPASS_CACHE);

    // check the cache
    Set names = (Set) cache.get(suffix, bypass);
    if (names != null) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "Cache HIT for list(suffix="+
            suffix+", bypass="+bypass+")");
      }
      res.setResult(names, -1);
      return res;
    }

    // no "list" hints

    // check for cache-only
    if (req.hasOption(Request.CACHE_ONLY)) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "Cache MISS (CACHE_ONLY) for list(suffix="+
            suffix+", bypass="+bypass+")");
      }
      res.setResult(null, -1);
      return res;
    }

    MissEntry me = (MissEntry) misses.get(suffix);
    if (me == null) {
      me = new MissEntry();
      misses.put(suffix, me);
    }

    // check for an already pending "list" request
    //
    // we send the first "list" ourselves, so we only
    // need to see if the pending list is non-empty
    boolean alreadyPending = (me.numResponses() > 0);

    // must add this response (first or not)
    me.addResponse(res);

    if (logger.isDetailEnabled()) {
      logger.detail(
          "Cache MISS ("+
          (alreadyPending ? "PENDING" : "SENDING")+
          ") for list(suffix="+
          suffix+", bypass="+bypass+")");
    }

    if (alreadyPending) {
      return null;
    }

    return res;
  }

  //
  // update the cache:
  //

  private void update(
      String suffix,
      Object result,
      long ttl) {
    // fix ttl
    long now = System.currentTimeMillis();
    long maxTTL = now + config.maxTTD;
    if (maxTTL < ttl) {
      // reduce ttl if too long
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Reduce ttl from "+
            Timestamp.toString(ttl,now)+
            " to "+
            Timestamp.toString(maxTTL,now));
      }
      ttl = maxTTL;
    }

    listed(suffix, result, ttl);
  }

  private void listed(String suffix, Object result, long ttl) {
    Set names;
    if (result instanceof Set) {
      names = (Set) result; // immutable?
    } else if (result == null) {
      names = Collections.EMPTY_SET;
    } else {
      // failure?
      names = null;
    }

    synchronized (lock) {
      if (names != null) {
        if (logger.isDebugEnabled()) {
          logger.debug(
              "Caching listed(suffix="+suffix+
              ", ttl="+Timestamp.toString(ttl)+
              ", names="+names+")");
        }
        cache.put(suffix, names, ttl);
      }

      MissEntry me = (MissEntry) misses.remove(suffix);
      if (me != null) {
        if (me.numResponses() > 0) {
          if (logger.isDebugEnabled()) {
            logger.debug(
                "Setted pending listed(suffix="+suffix+
                ", result="+result+") for "+me);
          }
          me.setResults(result);
        }
      }
    }
  }

  private void cleanCache(SchedulableWrapper thread) {
    // assert (thread == cleanCacheThread);
    cleanCache();
    // run me again later
    thread.schedule(config.cleanCachePeriod);
  }

  // this is optional, since the LRU will clean
  private void cleanCache() {
    synchronized (lock) {
      cache.trim();

      // might as well debug our misses-table on the timer thread
      if (logger.isDebugEnabled()) {
        String s = "";
        s += "\n##### pending list requests ###############################";
        s += "\ntable["+misses.size()+"]: ";
        for (Iterator iter = misses.entrySet().iterator(); iter.hasNext(); ) {
          Map.Entry x = (Map.Entry) iter.next();
          String name = (String) x.getKey();
          MissEntry me = (MissEntry) x.getValue();
          s += "\n  "+name+":";
          s += "\n     "+me;
        }
        s += "\n###########################################################";
        logger.debug(s);
      }
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

  /** used for pending cache misses */
  private static class MissEntry {

    private static List NO_RESPONSES = Collections.EMPTY_LIST;

    // List<Response>
    private List responses = NO_RESPONSES;
    private long firstResponseTime = 0;

    public int numResponses() {
      return responses.size();
    }
    public Response getResponse(int index) {
      return (Response) responses.get(index);
    }
    public long getFirstResponseTime() {
      return firstResponseTime;
    }
    public void addResponse(Response res) {
      if (res == null) {
        throw new IllegalArgumentException("Null res");
      }
      if (responses == NO_RESPONSES) {
        responses = new ArrayList(3);
        firstResponseTime = System.currentTimeMillis();
      }
      responses.add(res);
    }
    public void removeResponse(int index) {
      if (responses.remove(index) != null) {
        if (responses.isEmpty()) {
          responses = NO_RESPONSES;
          firstResponseTime = 0;
        }
      }
    }
    /** set the result for all pending responses */
    public void setResults(Object obj) {
      int n = numResponses();
      if (n > 0) {
        for (int i = 0; i < n; i++) {
          Response res = getResponse(i);
          res.setResult(obj, -1);
        }
        responses = NO_RESPONSES;
        firstResponseTime = 0;
      }
    }

    public String toString() {
      return 
        "(miss-entry"+
        " responses["+responses.size()+"]="+responses+
        " firstResponseTime="+
        Timestamp.toString(firstResponseTime)+
        ")";
    }
  }
}

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.wp.Scheduled;
import org.cougaar.core.wp.SchedulableWrapper;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.util.LRUExpireMap;
import org.cougaar.util.log.Logger;

/**
 * This cache holds "get" and "getAll" results, including negative
 * caching, and supports internal ttl/lru gc.
 * <p>
 * This implementation accepts a client-side "fetch" for any type of
 * request, but (as a design simplification) does not support
 * responses of type "get".
 */
public class CacheEntriesHandler
extends HandlerBase
{
  // see LeaseHandler workaround + bug 2837 + bug 2839
  private static final boolean UPGRADE_GET_TO_GETALL = false;

  private CacheEntriesConfig config;

  private WhitePagesService wps;

  private final Object lock = new Object();

  // name -> (type -> entry)
  // Map<String, Map<String, AddressEntry>>
  private LRUExpireMap cache;
  
  // map request to hints and queued responses
  // name -> MissEntry
  // Map<String, MissEntry>
  private final Map misses = new HashMap(13);


  //
  // send upgraded "getAll" requests:
  //

  private SchedulableWrapper sendGetAllThread;

  // Set<String>
  private final Set getAllQueue = new HashSet(13);

  // temporary list to drain the getAllQueue
  // List<String>
  private final List tmpRunList = new ArrayList(13);

  //
  // clean the cache:
  //

  private SchedulableWrapper cleanCacheThread;

  public void setParameter(Object o) {
    this.config = new CacheEntriesConfig(o);
  }

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  public void load() {
    cache = new LRUExpireMap(
        new MyCacheConfig(),              // my config
        new LoggingCacheWatcher(logger)); // my logger

    Scheduled sendGetAllRunner =
      new Scheduled() {
        public void run(SchedulableWrapper thread) {
          // assert (thread == sendGetAllThread);
          sendGetAll();
        }
      };
    sendGetAllThread = SchedulableWrapper.getThread(
        threadService,
        sendGetAllRunner,
        "White pages client cache send getAll requests");

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
          "White pages server clean entries cache");
      cleanCacheThread.schedule(config.cleanCachePeriod);
    }
  }

  protected Response mySubmit(Response res) {
    return fetch(res);
  }

  protected void myExecute(
      Request req,
      Object result,
      long ttl) {
    update(req, result, ttl);
  }

  // send upgraded "get" -> "getAll" requests
  private void sendGetAll() {
    synchronized (lock) {
      if (getAllQueue.isEmpty()) {
        return;
      }
      tmpRunList.addAll(getAllQueue);
      getAllQueue.clear();
    }

    for (int i = 0, n = tmpRunList.size(); i < n; i++) {
      String name = (String) tmpRunList.get(i);
      Request req = new Request.GetAll(Request.NONE, name);
      wps.submit(req);
    }
    tmpRunList.clear();
  }

  //
  // look in the cache:
  //

  private Response fetch(Response res) {
    Response ret = res;
    Request req = res.getRequest();
    synchronized (lock) {
      if (req instanceof Request.Get) {
        ret = fetch(res, (Request.Get) req);
      } else if (req instanceof Request.GetAll) {
        ret = fetch(res, (Request.GetAll) req);
      } else if (req instanceof Request.List) {
        // ignore
      } else if (req instanceof Request.Bind) {
        fetch(res, (Request.Bind) req);
      } else if (req instanceof Request.Unbind) {
        fetch(res, (Request.Unbind) req);
      } else {
        throw new IllegalArgumentException("Unknown action");
      }
    }
    return ret;
  }

  private Response fetch(Response res, Request.Get req) {
    String name = req.getName();
    String type = req.getType();
    boolean bypass = req.hasOption(Request.BYPASS_CACHE);

    // check the cache
    Map entries = (Map) cache.get(name, bypass);
    if (entries != null) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "Cache HIT for get(name="+
            name+", type="+type+", bypass="+bypass+")");
      }
      res.setResult(entries, -1);
      return res;
    }

    MissEntry me = (MissEntry) misses.get(name);

    // check for a hint
    AddressEntry hintAE = (me == null ? null : me.getHint(type));
    if (hintAE != null) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "Cache HINT for get(name="+
            name+", type="+type+", bypass="+bypass+")");
      }
      res.setResult(hintAE, -1);
      return res;
    }

    // check for cache-only
    if (req.hasOption(Request.CACHE_ONLY)) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "Cache MISS (CACHE_ONLY) for get(name="+
            name+", type="+type+", bypass="+bypass+")");
      }
      res.setResult(null, -1);
      return res;
    }

    if (me == null) {
      me = new MissEntry();
      misses.put(name, me);
    }

    // check for an already pending request
    boolean alreadyPending;
    if (UPGRADE_GET_TO_GETALL) {
      // already sent our "getAll"
      alreadyPending = (me.numResponses() > 0);
    } else {
      // look for the specific type
      alreadyPending = (me.containsGet(type));
    }

    if (logger.isDetailEnabled()) {
      logger.detail(
          "Cache MISS ("+
          (alreadyPending ? "PENDING" : "SENDING")+
          ") for get(name="+
          name+", type="+type+", bypass="+bypass+")");
    }

    if (alreadyPending) {
      return null;
    }

    // add a new pending response
    me.addResponse(res);

    // queue for separate "getAll" submit
    getAllQueue.add(name);
    sendGetAllThread.start();

    if (UPGRADE_GET_TO_GETALL) {
      // the other thread will send our "getAll"
      return null;
    } else {
      // we must send the "get" as well
      //
      // the LeaseHandler swallows it anyways
      return res;
    }
  }

  private Response fetch(Response res, Request.GetAll req) {
    String name = req.getName();
    boolean bypass = req.hasOption(Request.BYPASS_CACHE);

    // check the cache
    Map entries = (Map) cache.get(name, bypass);
    if (entries != null) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "Cache HIT for getAll(name="+
            name+", bypass="+bypass+")");
      }
      res.setResult(entries, -1);
      return res;
    }

    // no "getAll" hints

    // check for cache-only
    if (req.hasOption(Request.CACHE_ONLY)) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "Cache MISS (CACHE_ONLY) for getAll(name="+
            name+", bypass="+bypass+")");
      }
      res.setResult(null, -1);
      return res;
    }

    MissEntry me = (MissEntry) misses.get(name);
    if (me == null) {
      me = new MissEntry();
      misses.put(name, me);
    }

    // check for an already pending "getAll" request
    //
    // be careful, since we may be the one sending a "getAll"
    // due to a "get" upgrade
    boolean alreadyPending = me.containsGetAll();

    if (logger.isDetailEnabled()) {
      logger.detail(
          "Cache MISS ("+
          (alreadyPending ? "PENDING" : "SENDING")+
          ") for getAll(name="+
          name+", bypass="+bypass+")");
    }

    if (alreadyPending) {
      return null;
    }

    // add a new pending response
    me.addResponse(res);

    // send out to the remote WP
    return res;
  }

  private void fetch(Response res, Request.Bind req) {
    AddressEntry ae = req.getAddressEntry();
    String name = ae.getName();
    boolean overwrite = req.isOverWrite();

    // handle new hints
    if (req.hasOption(Request.CACHE_ONLY)) {
      String type = ae.getType();

      MissEntry me = (MissEntry) misses.get(name);
      if (me == null) {
        me = new MissEntry();
        misses.put(name, me);
      }

      if (!overwrite) {
        AddressEntry oldAE = me.getHint(type);
        if (oldAE != null) {
          Object ret;
          if (oldAE.equals(ae)) {
            // same as the current hint
            ret = Boolean.TRUE;
          } else {
            // a conflicting hint is already in place
            ret = oldAE;
          }
          res.setResult(ret, -1);
          return;
        }
      }

      // add the new hint
      me.addHint(ae);

      if (logger.isInfoEnabled()) {
        logger.info("Added hint "+ae+" to "+me);
      }

      // fill in any pending "get" requests
      for (int i = 0, n = me.numResponses(); i < n; i++) {
        Response pRes = me.getResponse(i);
        Request  pReq = pRes.getRequest();
        if (pReq instanceof Request.Get) {
          Request.Get pGReq = (Request.Get) pReq;
          String pType = pGReq.getType();
          if (type.equals(pType)) {
            me.removeResponse(i);
            --i;
            --n;
            if (logger.isInfoEnabled()) {
              logger.info(
                  "Setting batched get("+name+", "+type+
                  ") to hint "+ae+" for request "+pRes);
            }
            pRes.setResult(ae, -1);
          }
        }
      }

      // let the hint pass on to the bootstrappers,
      // since it stops at the lease manager
      return;
    }

    // clear the cache entry, just in case
    if (logger.isDebugEnabled()) {
      if (cache.containsKey(name)) {
        Map oldMap = (Map) cache.get(name);
        if (oldMap != null) {
          long ttl = cache.getExpirationTime(name);
          logger.debug(
              "Removing cache entry on bind("+ae+
              "), ttl="+Timestamp.toString(ttl)+
              ", entries="+oldMap+")");
        }
      }
    }
    cache.remove(name);

    // let the lease manager handle the batching
  }

  private void fetch(Response res, Request.Unbind req) {
    AddressEntry ae = req.getAddressEntry();
    String name = ae.getName();

    // handle dead hints
    if (req.hasOption(Request.CACHE_ONLY)) {
      MissEntry me = (MissEntry) misses.get(name);
      if (me != null) {
        if (me.removeHint(ae)) {

          if (logger.isInfoEnabled()) {
            logger.info("Removed hint "+ae+" from "+me);
          }

          if (me.numHints() == 0 &&
              me.numResponses() == 0) {
            misses.remove(name);
          }

          res.setResult(Boolean.TRUE, -1);
          return;
        }
      }

      // let the hint pass on to the bootstrappers,
      // it stops at the lease manager
      return;
    }

    // clear the cache entry
    cache.remove(name);
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
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Reduce ttl from "+
            Timestamp.toString(ttl,now)+
            " to "+
            Timestamp.toString(maxTTL,now));
      }
      ttl = maxTTL;
    }
    if (req instanceof Request.Get) {
      // Not supported for a design simplification.
      //
      // The batcher should upgrade all "get" requests
      // to "getAll".
      if (logger.isErrorEnabled()) {
        logger.error(
            "This cache doesn't support \"get\" responses,"+
            " expecting a full \"getAll\": "+req);
      }
    } else if (req instanceof Request.GetAll) {
      Request.GetAll rga = (Request.GetAll) req;
      String name = rga.getName();
      gotAll(name, result, ttl);
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

  private void gotAll(String name, Object result, long ttl) {

    Map entries;
    if (result instanceof Map) {
      entries = (Map) result; // immutable?
    } else if (result == null) {
      entries = Collections.EMPTY_MAP;
    } else {
      // failure?
      entries = null;
    }

    synchronized (lock) {
      if (entries != null) {
        if (logger.isDebugEnabled()) {
          logger.debug(
              "Caching gotAll(name="+name+
              ", ttl="+Timestamp.toString(ttl)+
              ", entries="+entries+")");
        }
        cache.put(name, entries, ttl);
      }

      MissEntry me = (MissEntry) misses.get(name);
      if (me != null) {
        if (me.numResponses() > 0) {
          if (logger.isDebugEnabled()) {
            logger.debug(
                "Setted pending gotAll(name="+name+
                ", result="+result+") for "+me);
          }
          me.setResults(result);
        }
        if (me.numHints() == 0) {
          misses.remove(name);
        }
      }
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
    String name = oldAE.getName();
    synchronized (lock) {
      Map oldMap = (Map) cache.get(name);
      if (oldMap != null) {
        long oldTTL = cache.getExpirationTime(name);
        // reduce the ttl to match
        long newTTL = Math.min(ttl, oldTTL);
        // replace the entry
        Map newMap = new HashMap(oldMap);
        String type = oldAE.getType();
        newMap.put(type, ae);
        newMap = Collections.unmodifiableMap(newMap);
        cache.put(name, newMap, newTTL);
      }
    }
  }

  private void cleanCache(SchedulableWrapper thread) {
    // assert (thread == cleanCacheThread);
    cleanCache();
    // run me again later
    thread.schedule(config.cleanCachePeriod);
  }

  // this is optional since the LRU will clean itself
  private void cleanCache() {
    synchronized (lock) {
      cache.trim();

      // might as well debug our misses-table on the timer thread
      if (logger.isDebugEnabled()) {
        String s = "";
        s += "\n##### pending \"get/getAll\" requests & hints #############";
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
              "600000"));
      minBypass =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.resolver.cacheEntries.minBypass",
              "10000"));
      maxSize =
        Integer.parseInt(
            System.getProperty(
              "org.cougaar.core.wp.resolver.cacheEntries.maxSize",
              "200"));
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

  /** used for hints and pending cache misses */
  private static class MissEntry {

    private static Map NO_HINTS = Collections.EMPTY_MAP;
    private static List NO_RESPONSES = Collections.EMPTY_LIST;

    // Map<String, AddressEntry>
    private Map hints = NO_HINTS;

    // List<Response>
    private List responses = NO_RESPONSES;
    private long firstResponseTime = 0;

    public int numHints() {
      return hints.size();
    }
    public AddressEntry getHint(String type) {
      return (AddressEntry) hints.get(type);
    }
    public void addHint(AddressEntry ae) {
      if (ae == null) {
        throw new IllegalArgumentException("Null hint");
      }
      if (hints == NO_HINTS) {
        hints = new HashMap(5);
      }
      hints.put(ae.getType(), ae);
    }
    public boolean removeHint(AddressEntry ae) {
      String type = ae.getType();
      AddressEntry oldAE = (AddressEntry) hints.get(type);
      if (oldAE != null && oldAE.equals(ae)) {
        hints.remove(type);
        if (hints.isEmpty()) {
          hints = NO_HINTS;
        }
      }
      return false;
    }

    public int numResponses() {
      return responses.size();
    }
    public boolean containsGetAll() {
      boolean isPending = false;
      for (int i = 0, n = numResponses(); i < n; i++) {
        Response res = getResponse(i);
        Request req = res.getRequest();
        if (req instanceof Request.GetAll) {
          isPending = true;
          break;
        }
      }
      return isPending;
    }
    public boolean containsGet(String type) {
      boolean isPending = false;
      for (int i = 0, n = numResponses(); i < n; i++) {
        Response res = getResponse(i);
        Request req = res.getRequest();
        if (req instanceof Request.Get) {
          Request.Get greq = (Request.Get) req;
          if (type.equals(greq.getType())) {
            isPending = true;
            break;
          }
        }
      }
      return isPending;
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
        " hints["+hints.size()+"]="+hints+
        " responses["+responses.size()+"]="+responses+
        " firstResponseTime="+
        Timestamp.toString(firstResponseTime)+
        ")";
    }
  }
}

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

package org.cougaar.core.naming;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventContext; // inlined
import javax.naming.event.EventDirContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.*;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * JDNI implementation of the WhitePagesService, backed by the
 * NamingService.
 */
public final class JNDIWhitePagesServiceComponent
extends GenericStateModelAdapter
implements Component {

  private ServiceBroker sb;
  private NamingService ns;
  private LoggingService log;
  private MessageAddress nodeId;
  private ThreadService threadService;

  private final Object wpContextLock = new Object();
  private EventDirContext wpContext;
  private final NamingListener wpListener = new WPListener();
  private static final String WP_DIR="WP";

  private WhitePagesService wpS;
  private WhitePagesServiceProvider wpSP;

  // map from  (name -> CacheEntry)
  private final Map cache = new HashMap();

  private final Set cacheWaiters = new HashSet(13);
  private final List queuePending = new ArrayList(13);
  private Schedulable queueThread;
  private final Map queueResponses = new HashMap(13);

  public void setBindingSite(BindingSite bs) {
    //this.sb = bs.getServiceBroker();
  }

  public void setNodeControlService(NodeControlService ncs) {
    this.sb = ncs.getRootServiceBroker();
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setNamingService(NamingService ns) {
    this.ns = ns;
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  public void load() {
    super.load();

    NodeIdentificationService nodeIdService = 
      (NodeIdentificationService)
      sb.getService(this, NodeIdentificationService.class, null);
    if (nodeIdService != null) {
      this.nodeId = nodeIdService.getMessageAddress();
      sb.releaseService(
          this, NodeIdentificationService.class, nodeIdService);
    }

    // create and advertise our service
    this.wpS = new WhitePagesServiceImpl();
    this.wpSP = new WhitePagesServiceProvider();
    sb.addService(WhitePagesService.class, wpSP);

    startFetcher();
  }

  public void unload() {
    // clean up ns?
    stopFetcher();
    // revoke our service
    if (wpSP != null) {
      sb.revokeService(WhitePagesService.class, wpSP);
      wpSP = null;
    }
    if (ns != null) {
      sb.releaseService(this, NamingService.class, ns);
      ns = null;
    }
    if (threadService != null) {
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }
    if (log != null) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
    super.unload();
  }

  // ------------------------------------------------------------------------
  // service provider & impl

  private class WhitePagesServiceProvider
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, 
          Object requestor, 
          Class serviceClass) {
        if (serviceClass == WhitePagesService.class) {
          return wpS;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, 
          Object requestor, 
          Class serviceClass, 
          Object service) {
      }
    }

  private class WhitePagesServiceImpl extends WhitePagesService {
    public Response submit(Request req) {
      Response res = req.createResponse();
      submitLater(res);
      return res;
    }
  }

  // ------------------------------------------------------------------------
  // non-cache non-queued actions:

  private Set filterNames(Set names, String suffix) {
    int n = (names == null ? 0 : names.size());
    if (n == 0) {
      return Collections.EMPTY_SET;
    }
    // assert (suffix.charAt(0) == '.');
    String suf = suffix;
    int lsuf = suf.length();
    if (".".equals(suf)) {
      // root
      suf = "";
      lsuf--;
    }
    Set ret = new HashSet();
    Iterator iter = names.iterator();
    for (int i = 0; i < n; i++) {
      String name = (String) iter.next();
      String tmp = null;
      if (name.endsWith(suf)) {
        int tailIdx = (name.length()-lsuf);
        int sep = name.lastIndexOf('.', tailIdx-1);
        tmp = name;
        if (sep > 0) {
          tmp = name.substring(sep);
        }
      }
      if (tmp != null) {
        ret.add(tmp);
      }
    }
    return ret;
  }

  private void bindNow(AddressEntry ae) throws Exception {
    long now = System.currentTimeMillis();
    if (ae.getTTL() < now) {
      throw new IllegalArgumentException(
          "TTL is less than current time ("+
          ae.getTTL()+" < "+now+")");
    }
    String key = createKey(ae);
    nsBind(key, ae);
  }

  private void rebindNow(AddressEntry ae) throws Exception {
    long now = System.currentTimeMillis();
    if (ae.getTTL() < now) {
      throw new IllegalArgumentException(
          "TTL is less than current time ("+
          ae.getTTL()+" < "+now+")");
    }
    String key = createKey(ae);
    nsRebind(key, ae);
  }

  private void unbindNow(AddressEntry ae) throws Exception {
    // ignore TTL
    String key = createKey(ae);
    nsUnbind(key);
  }

  // ------------------------------------------------------------------------
  // queue "submit".

  private void submitLater(Response res) {
    submitCached(res);
  }

  // ------------------------------------------------------------------------
  // ugly cache
  //
  // only caches "get" and "list"
  // "refresh" is handled as a "get" (still hits cache!)
  // don't cache "*bind", but correctly clears cache
  //
  // removed pending/stale support

  // ns callback:
  private void handleInit(List allKeys) {
    int n = (allKeys == null ? 0 : allKeys.size());
    if (log.isDebugEnabled()) { 
      log.debug("handleInit["+n+"]("+allKeys+")");
    }
    for (int i = 0; i < n; i++) {
      String ki = (String) allKeys.get(i);
      addKey(ki);
    }
  }
  private void handleAdd(String key) {
    if (log.isDebugEnabled()) { 
      log.debug("handleAdd("+key+")");
    }
    addKey(key);
  }
  private void handleChange(String key) {
    if (log.isDebugEnabled()) { 
      log.debug("handleChange("+key+")");
    }
    addKey(key);
  }
  private void handleRemove(String key) {
    if (log.isDebugEnabled()) { 
      log.debug("handleRemove("+key+")");
    }
    removeKey(key);
  }

  private void addKey(String key) {
    String name = extractName(key);
    synchronized (cache) {
      CacheEntry ce = (CacheEntry) cache.get(name);
      if (ce == null) {
        ce = new CacheEntry();
        cache.put(name, ce);
      }
      ce.isStale = true;
      ce.keys.add(key);
    }
  }
  private void removeKey(String key) {
    String name = extractName(key);
    synchronized (cache) {
      CacheEntry ce = (CacheEntry) cache.get(name);
      if (ce != null) {
        ce.isStale = true;
        ce.keys.remove(key);
        if (ce.keys.isEmpty()) {
          cache.remove(name);
        }
      }
    }
  }

  private void submitCached(final Response res) {
    final Request req = res.getRequest();
    final long timeout = req.getTimeout();
    if (timeout < 0) {
      throw new IllegalArgumentException("Negative timeout: "+timeout);
    }

    if (req instanceof Request.Get) {
      // "get"
      String name = ((Request.Get) req).getName();
      Object ret;
      synchronized (cache) {
        CacheEntry ce = (CacheEntry) cache.get(name);
        if (ce == null) {
          // not listed
          ret = AddressEntry.EMPTY_ARRAY;
        } else if (!ce.isStale) {
          // already cached
          List l = ce.entries;
          int n = l.size();
          AddressEntry[] a = new AddressEntry[n];
          for (int i = 0; i < n; i++) {
            a[i] = (AddressEntry) l.get(i);
          }
          ret = a;
        } else {
          // must fetch
          ret = null;
        }
      }
      if (ret != null) {
        if (log.isDebugEnabled()) { 
          log.debug("HIT "+res+" "+ret);
        }
        res.setResult(ret);
        return;
      }
      // wrap timeout response to allow removal
      final Object qe;
      if (req.getTimeout() <= 0) {
        qe = res;
      } else {
        qe = new WrappedResponse(res);
      }
      boolean mustQueue;
      synchronized (cacheWaiters) {
        // issue a big "fetch-all" to update the cache
        mustQueue = cacheWaiters.isEmpty();
        cacheWaiters.add(qe);
      }
      if (mustQueue) {
        fetchLater(null);
      }
      if (timeout > 0) {
        TimerTask timerTask = new TimerTask() {
          public void run() {
            synchronized (cacheWaiters) {
              cacheWaiters.remove(qe);
            }
            if (log.isDebugEnabled()) { 
              log.debug("TIMEOUT "+res+" "+this);
            }
            res.setResult(Response.TIMEOUT);
          }
          public String toString() {
            return 
              "(timer oid="+System.identityHashCode(this)+
              " sched="+scheduledExecutionTime()+
              " timeout="+timeout+" qe="+qe+")";
          }
        };
        if (log.isDebugEnabled()) {
          log.debug("Schedule "+timerTask);
        }
        threadService.schedule(timerTask, timeout);
      }
      return;
    }

    if (req instanceof Request.List) {
      // "list"
      Set ret;
      String suffix = ((Request.List) req).getSuffix();
      synchronized (cache) {
        ret = filterNames(cache.keySet(), suffix);
      }
      if (log.isDebugEnabled()) { 
        log.debug("LIST "+suffix+" ["+ret.size()+"]");
      }
      res.setResult(ret);
      return;
    } 

    if (req instanceof Request.Refresh) {
      // special case using "get"
      refreshCached((Response.Refresh) res);
      return;
    }

    {
      // must be a *bind
      AddressEntry ae;
      if (req instanceof Request.Bind) {
        ae = ((Request.Bind) req).getAddressEntry();
      } else if (req instanceof Request.Rebind) {
        ae = ((Request.Rebind) req).getAddressEntry();
      } else if (req instanceof Request.Unbind) {
        ae = ((Request.Unbind) req).getAddressEntry();
      } else {
        throw new RuntimeException(
            "Unexpected type: "+
            (req == null ? "null" : req.getClass().getName()));
      }
      // optional: invalidate cache entry before NS update?
      // we'll let the callback do this
      fetchLater(res);
      if (timeout > 0) {
        TimerTask timerTask = new TimerTask() {
          public void run() {
            res.setResult(Response.TIMEOUT);
            if (log.isDebugEnabled()) {
              log.debug("TIMEOUT "+res+" "+this);
            }
          }
          public String toString() {
            return 
              "(timer oid="+System.identityHashCode(this)+
              " sched="+scheduledExecutionTime()+
              " timeout="+timeout+")";
          }
        };
        if (log.isDebugEnabled()) {
          log.debug("Scheduled "+timerTask);
        }
        threadService.schedule(timerTask, timeout);
      }
    }
  }

  private void refreshCached(final Response.Refresh origRes) {
    // translate a "refresh(entry)" to a "get(entry.name)"
    Request.Refresh origReq = (Request.Refresh) origRes.getRequest();
    long timeout = origReq.getTimeout();
    AddressEntry ae = origReq.getOldEntry();
    final Application app = ae.getApplication();
    final String scheme = ae.getAddress().getScheme();
    Request getReq = new Request.Get(ae.getName(), timeout);
    Callback getC = new Callback() {
      public void execute(Response r) {
        if (log.isDebugEnabled()) {
          log.debug("Refresh callback origRes="+origRes+" r="+r);
        }
        Response.Get gr = (Response.Get) r;
        Object ret = Response.Refresh.NULL;
        AddressEntry[] a = gr.getAddressEntries();
        int n = (a==null?0:a.length);
        for (int i = 0; i < n; i++) {
          AddressEntry ai = a[i];
          if (ai.getApplication().equals(app) &&
              ai.getAddress().getScheme().equals(scheme)) {
            if (ai.getTTL() >= System.currentTimeMillis()) {
              ret = ai;
            }
            break;
          }
        }
        origRes.setResult(ret);
        if (log.isDebugEnabled()) {
          log.debug("Refresh callback set result for "+origRes);
        }
      }
    };
    Response getRes = getReq.createResponse();
    getRes.addCallback(getC);
    submitCached(getRes);
  }

  private static class WrappedResponse {
    public final Response res;
    public WrappedResponse(Response res) {
      this.res = res;
    }
    public String toString() { 
      return "(wrapped "+res+")";
    }
  }

  private void fetchNow(Response res) {
    if (log.isDebugEnabled()) {
      log.debug("Starting fetch "+res);
    }
    if (res == null) {
      // fetch all due to "get"
      Map fullMap = new HashMap();
      Exception e = null;
      // IDEA: return if cacheWaiters.isEmpty()?
      try {
        fetchAll(fullMap);
      } catch (Exception x) {
        e = x;
      }
      synchronized (cacheWaiters) {
        Iterator iter = cacheWaiters.iterator();
        for (int i = 0, n = cacheWaiters.size(); i < n; i++) {
          Object oi = iter.next();
          queueResponses.put(oi, e);
        }
        cacheWaiters.clear();
      }
      if (e == null) {
        synchronized (cache) {
          // update entire cache
          //
          // clear cache -- FIXME is this safe?  suppose a
          // callback "handleAdd" occurs just after the 
          // "fetchAll"?  TODO maybe fix later...
          cache.clear();
          Iterator fullMapIter = fullMap.entrySet().iterator();
          for (int i = 0, n = fullMap.size(); i < n; i++) {
            Map.Entry me = (Map.Entry) fullMapIter.next();
            String key = (String) me.getKey();
            AddressEntry ae = (AddressEntry) me.getValue();
            String name = ae.getName();
            CacheEntry ce = (CacheEntry) cache.get(name);
            if (ce == null) {
              ce = new CacheEntry();
              cache.put(name, ce);
            }
            ce.keys.add(key);
            ce.isStale = false;
            ce.entries.add(ae);
          }
          // prepare responses
          Iterator qrIter = queueResponses.entrySet().iterator();
          for (int i = 0, n = queueResponses.size(); i < n; i++) {
            Map.Entry me = (Map.Entry) qrIter.next();
            Object o = me.getKey();
            Response ri;
            if (o instanceof WrappedResponse) {
              ri = ((WrappedResponse) o).res;
            } else {
              ri = (Response) o;
            }
            Request.Get req = (Request.Get) ri.getRequest();
            String name = req.getName();
            CacheEntry ce = (CacheEntry) cache.get(name);
            Object val;
            if (ce == null) {
              val = AddressEntry.EMPTY_ARRAY;
            } else {
              // at this point we ignore the stale flag
              List l = ce.entries;
              int m = l.size();
              AddressEntry[] a = new AddressEntry[m];
              for (int j = 0; j < m; j++) {
                a[j] = (AddressEntry) l.get(j);
              }
              val = a;
            }
            me.setValue(val);
          }
        }
      }
      // IDEA: do this in a separate thread?
      Iterator qrIter = queueResponses.entrySet().iterator();
      for (int i = 0, n = queueResponses.size(); i < n; i++) {
        Map.Entry me = (Map.Entry) qrIter.next();
        Object o = me.getKey();
        Response ri;
        if (o instanceof WrappedResponse) {
          ri = ((WrappedResponse) o).res;
        } else {
          ri = (Response) o;
        }
        Object val = me.getValue();
        if (log.isDebugEnabled()) {
          log.debug("MISS "+ri+" "+val);
        }
        ri.setResult(val);
      }
      queueResponses.clear();
      return;
    } 

    {
      // must be a *bind
      Request req = res.getRequest();
      Object val;
      AddressEntry ae;
      try {
        if (req instanceof Request.Bind) {
          ae = ((Request.Bind)req).getAddressEntry();
          bindNow(ae);
        } else if (req instanceof Request.Rebind) {
          ae = ((Request.Rebind)req).getAddressEntry();
          rebindNow(ae);
        } else if (req instanceof Request.Unbind) {
          ae = ((Request.Unbind)req).getAddressEntry();
          unbindNow(ae);
        } else {
          throw new InternalError(
              "Invalid request class: "+
              (req==null?"null":req.getClass().getName()));
        }
        val = ae;
      } catch (Exception e) {
        val = e;
      }
      // IDEA: do this in a separate thread?
      if (log.isDebugEnabled()) {
        log.debug("MISS "+res+" "+val);
      }
      res.setResult(val);
    }
  }

  // fetch-later queue

  private void fetchLater(Response res) {
    synchronized (queuePending) {
      if (log.isDebugEnabled()) {
        log.debug("Locked fetchLater("+res+"), adding entry");
      }
      queuePending.add(res);
      if (log.isDebugEnabled()) {
        log.debug(
            "Unlocking fetchLater("+res+")");
      }
    }
    queueThread.start();
  }

  private void startFetcher() {
    // add trim-cache TimerTask?
    if (log.isDebugEnabled()) {
      log.debug("Starting queue fetcher thread");
    }
    Runnable fetchRunner = new Runnable() {
      private final Object lock = new Object();
      private Thread lockOwner = null;
      public void run() {
        // This outer lock shouldn't be necessary, but debugging has
        // shown the "ThreadService.start()" to not guarantee a
        // single-threaded run behavior.
        synchronized (lock) {
          Thread myThread = Thread.currentThread();
          if (lockOwner != null) {
            if (log.isInfoEnabled()) {
              log.info(
                  "Multiple simultaneous queue runners?"+
                  " Current thread: "+myThread+
                  " Lock owned by: "+lockOwner);
            }
            do {
              try {
                lock.wait();
              } catch (Exception e) {
                if (log.isErrorEnabled()) {
                  log.error(null, e);
                }
              }
            } while (lockOwner != null);
          }
          lockOwner = myThread;
        }
        try {
          syncRun();
        } finally {
          synchronized (lock) {
            lockOwner = null;
            lock.notifyAll();
          }
        }
      }

      private final List tmp = new ArrayList();
      private void syncRun() {
        synchronized (queuePending) {
          if (log.isDebugEnabled()) {
            log.debug(
                "Take "+queuePending.size()+
                " entries off work queue");
          }
          tmp.addAll(queuePending);
          queuePending.clear();
        }
        for (int i = 0, n = tmp.size(); i < n; i++) {
          Response res = (Response) tmp.get(i);
          fetchNow(res);
        }
        tmp.clear();
      }
    };
    queueThread = 
      threadService.getThread(
          this, fetchRunner, "WhitePages JNDI Fetcher");
    if (log.isDebugEnabled()) {
      log.debug("Launched queue fetcher thread");
    }
  }

  private void stopFetcher() {
    // maybe "queueThread.cancel()"?
  }

  private static final class CacheEntry {
    // set of String keys (name#app#scheme)
    // mutable, access under cache lock
    public final Set keys = new HashSet(13);
    // true if the entries are stale
    public boolean isStale = true;
    // fetched entries, immutable but maybe stale
    public final List entries = new ArrayList();
    public String toString() {
      return "(keys="+keys+" stale="+isStale+" entries="+entries+")";
    }
  }

  // ------------------------------------------------------------------------
  // NS expired-TTL cleaner:

  // TODO

  // ------------------------------------------------------------------------
  // JNDI WP representation:

  private String createKey(AddressEntry ae) {
    String name = ae.getName();
    String app = ae.getApplication().toString();
    String scheme = ae.getAddress().getScheme();
    if (name.indexOf('#') >= 0 ||
        app.indexOf('#') >= 0 ||
        scheme.indexOf('#') >= 0) {
      String msg = 
        "AddressEntry contains unexpected \"#\" character: "+
        ae;
      if (log.isErrorEnabled()) {
        log.error(msg);
      }
      throw new RuntimeException(msg);
    }
    String key = name+"#"+app+"#"+scheme;
    return key;
  }

  private String extractName(String key) {
    int i = (key == null ? -1 : key.indexOf('#'));
    if (i < 0) {
      if (log.isErrorEnabled()) {
        log.error("Illegal key: "+key);
      }
      return null;
    }
    return key.substring(0, i);
  }

  // ------------------------------------------------------------------------
  // JNDI guts:

  private void fetchAll(Map toMap) throws Exception {
    // this looks bad, but our NamingService impl is
    // a client-side filter anyways...
    DirContext ctx = getWPContext();
    NamingEnumeration e = ctx.listBindings("");
    while (e.hasMore()) {
      Binding b = (Binding) e.next();
      toMap.put(b.getName(), b.getObject());
    }
  }
  
  private void nsBind(
      String name, Object obj) throws NamingException {
    DirContext ctx = getWPContext();
    ctx.bind(name, obj, new BasicAttributes());
  }

  private void nsRebind(
      String name, Object obj) throws NamingException {
    DirContext ctx = getWPContext();
    ctx.rebind(name, obj, new BasicAttributes());
  }

  private void nsUnbind(String name) throws NamingException {
    DirContext ctx = getWPContext();
    ctx.unbind(name);
  }

  private EventDirContext getWPContext() throws NamingException {
    synchronized (wpContextLock) {
      if (wpContext == null) {
        try {
          DirContext ctx = (DirContext) ns.getRootContext();
          try {
            wpContext = (EventDirContext) ctx.lookup(WP_DIR);
          } catch (NameNotFoundException nnfe) {
            wpContext = (EventDirContext) 
              ctx.createSubcontext(WP_DIR, new BasicAttributes());
          }
          if (wpListener != null) {
            wpContext.addNamingListener(
                "", EventContext.SUBTREE_SCOPE, wpListener);
            NamingEnumeration en = wpContext.list("");
            List allKeys = new ArrayList();
            while (en.hasMore()) {
              NameClassPair ncp = (NameClassPair) en.next();
              allKeys.add(ncp.getName());
            }
            handleInit(allKeys);
          }
        } catch (NamingException ne) {
          throw ne;
        } catch (Exception e) {
          NamingException x = 
            new NamingException("Unable to access name-server");
          x.setRootCause(e);
          throw x;
        }
      }
      return wpContext;
    }
  }

  private class WPListener 
    implements NamespaceChangeListener, ObjectChangeListener {
      public void objectAdded(NamingEvent evt) {
        String key = evt.getNewBinding().getName();
        try {
          wpContext.addNamingListener(
              key, EventContext.OBJECT_SCOPE, wpListener);
          if (log.isDebugEnabled()) {
            log.debug(
                "Installing NamingListener for added "+key);
          }
        } catch (NamingException ne) {
          log.error("Error installing naming listener", ne);
          return;
        }
        handleAdd(key);
      }
      public void objectRemoved(NamingEvent evt) {
        String key = evt.getOldBinding().getName();
        handleRemove(key);
      }
      public void objectRenamed(NamingEvent evt) {
        /*
           String key = evt.getNewBinding().getName();
           handleRename(key);
         */
      }
      public void objectChanged(NamingEvent evt) {
        String key = evt.getNewBinding().getName();
        handleChange(key);
      }
      public void namingExceptionThrown(NamingExceptionEvent evt) {
      }
    }
}

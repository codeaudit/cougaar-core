/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import javax.naming.Binding;
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
 * <p>
 * Load into the node agent:<pre>
 *   plugin = org.cougaar.core.naming.JNDIWhitePagesServiceComponent
 * </pre>
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
  private static final String NAME_ATTR="NAME";
  private static final String APP_ATTR ="APP";
  private static final String URI_ATTR ="URI";
  private static final String CERT_ATTR="CERT";
  private static final String TTL_ATTR ="TTL";

  private WhitePagesService wpS;
  private WhitePagesServiceProvider wpSP;

  // map from  (name -> Request.Get.Response)
  private final Map cache = new HashMap();
  // special entry for "getAll"
  private CacheEntry cacheAllEntry = null;
  private final List queuePending = new ArrayList();
  private Schedulable queueThread;
  private final List queueResponses = new ArrayList();

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
      // res.setResult(submitNow(req));
      submitLater(res);
      return res;
    }
  }

  // ------------------------------------------------------------------------
  // non-cache non-queued actions:

  private Object submitNow(Request req) {
    Object ret;
    try {
      if (req instanceof Request.Get) {
        String name = ((Request.Get)req).getName();
        ret = getNow(name);
      } else if (req instanceof Request.GetAll) {
        ret = getAllNow();
      } else if (req instanceof Request.Refresh) {
        AddressEntry ae = ((Request.Refresh)req).getOldEntry();
        ret = refreshNow(ae);
        if (ret == null) {
          ret = Response.Refresh.NULL;
        }
      } else if (req instanceof Request.Bind) {
        AddressEntry ae = ((Request.Bind)req).getAddressEntry();
        bindNow(ae);
        ret = ae;
      } else if (req instanceof Request.Rebind) {
        AddressEntry ae = ((Request.Rebind)req).getAddressEntry();
        rebindNow(ae);
        ret = ae;
      } else if (req instanceof Request.Unbind) {
        AddressEntry ae = ((Request.Unbind)req).getAddressEntry();
        unbindNow(ae);
        ret = ae;
      } else {
        throw new IllegalArgumentException(
            "Invalid request class: "+
            (req==null?"null":req.getClass().getName()));
      }
    } catch (Exception e) {
      ret = e;
    }
    return ret;
  }

  private AddressEntry[] getNow(String name) throws Exception {
    Attributes match = new BasicAttributes();
    if (name != null) {
      match.put(NAME_ATTR, name);
    }
    List allAts = fetchAll(match);
    AddressEntry[] ret;
    int n = (allAts==null?0:allAts.size());
    if (n <= 0) {
      ret = AddressEntry.EMPTY_ARRAY;
    } else {
      ret = new AddressEntry[n];
      long now = System.currentTimeMillis();
      int j = 0;
      for (int i = 0; i < n; i++) {
        Attributes ats = (Attributes) allAts.get(i);
        AddressEntry ae = createAddressEntry(ats);
        if (ae.getTTL() > now) {
          ret[j++]=ae;
        }
      }
      if (j < n) {
        // some expired
        if (j == 0) {
          ret = AddressEntry.EMPTY_ARRAY;
        } else {
          AddressEntry[] x = new AddressEntry[j];
          System.arraycopy(ret, 0, x, 0, j);
          ret = x;
        }
      }
    }
    return ret;
  }

  private AddressEntry[] getAllNow() throws Exception {
    return getNow(null);
  }

  private AddressEntry refreshNow(AddressEntry ae) throws Exception {
    Application app = ae.getApplication();
    String scheme = ae.getAddress().getScheme();
    AddressEntry[] a = getNow(ae.getName()); // lazy!
    int n = (a==null?0:a.length);
    AddressEntry ret = null;
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
    Attributes ats = createAttributes(ae);
    register(key, ats);
  }

  private void rebindNow(AddressEntry ae) throws Exception {
    long now = System.currentTimeMillis();
    if (ae.getTTL() < now) {
      throw new IllegalArgumentException(
          "TTL is less than current time ("+
          ae.getTTL()+" < "+now+")");
    }
    bindNow(ae);
  }

  private void unbindNow(AddressEntry ae) throws Exception {
    // ignore TTL
    String key = createKey(ae);
    unregister(key);
  }

  // ------------------------------------------------------------------------
  // queue "submit".

  private void submitLater(Response res) {
    submitCached(res);
  }

  // ------------------------------------------------------------------------
  // ugly cache
  //
  // only caches "get" and "getAll"
  // "refresh" is handled as a "get" (still hits cache!)
  // don't cache "*bind", but correctly clears cache
  //
  // removed pending/stale support

  // ns callback:
  private void dirtyName(String name) {
    synchronized (cache) {
      if (log.isDebugEnabled()) { 
        log.debug("dirtyName("+name+")");
      }
      cache.remove(name);
      cacheAllEntry = null;
    }
  }

  private void submitCached(Response res) {
    Request req = res.getRequest();
    long timeout = req.getTimeout();
    if (timeout < 0) {
      throw new IllegalArgumentException("Negative timeout: "+timeout);
    }
    CacheEntry entry;
    if (req instanceof Request.Get) {
      // "get"
      String name = ((Request.Get) req).getName();
      synchronized (cache) {
        entry = (CacheEntry) cache.get(name);
        if (entry == null) {
          entry = new CacheEntry(req);
          cache.put(name, entry);
        }
      }
      submitCached(entry, res);
    } else if (req instanceof Request.GetAll) {
      // "getAll"
      synchronized (cache) {
        if (cacheAllEntry == null) {
          cacheAllEntry = new CacheEntry(req);
        }
        entry = cacheAllEntry;
      }
      submitCached(entry, res);
    } else if (req instanceof Request.Refresh) {
      // special case using "get"
      refreshCached((Response.Refresh) res);
    } else {
      // *bind
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
      // invalidate cache entry
      synchronized (cache) {
        cache.remove(ae.getName());
      }
      // not cached, use a dummy entry
      submitCached((new CacheEntry(req)), res);
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
    submitCached(getRes); // lazy!
  }

  /** @return Request or Throwable */
  private void submitCached(
      final CacheEntry entry,
      final Response res) {
    Request req = entry.req;
    boolean isFirst;
    final Object qe;
    synchronized (entry) {
      // check for already fetched value
      Object value = entry.value;
      if (value != null) {
        // check TTL
        boolean isOkay = true;
        if (value instanceof AddressEntry) {
          long ttl = ((AddressEntry) value).getTTL();
          if (ttl < System.currentTimeMillis()) {
            isOkay = false;
          }
        } else if (value instanceof AddressEntry[]) {
          AddressEntry[] a = (AddressEntry[]) value;
          int n = a.length;
          if (n > 0) {
            long now = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
              long ttl = a[i].getTTL();
              if (ttl < now) {
                isOkay = false;
                break;
              }
            }
          }
        }
        if (isOkay) {
          if (log.isDebugEnabled()) {
            log.debug("HIT "+req+" "+entry);
          }
          res.setResult(value);
          return;
        } else {
          if (log.isDebugEnabled()) {
            log.debug("MISS <expired> "+req+" "+entry);
          }
          entry.value = null;
        }
      }
      // wrap timeout response to allow removal
      if (req.getTimeout() <= 0) {
        qe = res;
      } else {
        qe = new WrappedResponse(res);
      }
      isFirst = entry.responses.isEmpty();
      entry.responses.add(qe);
    }

    if (isFirst) {
      if (log.isDebugEnabled()) {
        log.debug("MISS <new> "+req+" "+entry);
      }
      fetchLater(entry);
    } else {
      if (log.isDebugEnabled()) {
        log.debug("MISS <queued> "+req+" "+entry);
      }
    }

    final long timeout = req.getTimeout();
    if (timeout > 0) {
      TimerTask timerTask = new TimerTask() {
        public void run() {
          synchronized (entry) {
            if (log.isDebugEnabled()) {
              log.debug("Timeout "+this);
            }
            boolean b = entry.responses.remove(qe);
            if (log.isDebugEnabled()) {
              log.debug("Timeout removed "+this);
            }
            res.setResult(Response.TIMEOUT);
            if (log.isDebugEnabled()) {
              log.debug("Timeout set res timeout "+res);
            }
          }
        }
        public String toString() {
          return 
            "(timer oid="+System.identityHashCode(this)+
            " sched="+scheduledExecutionTime()+
            " timeout="+timeout+" entry="+entry+
            " qe="+qe+")";
        }
      };
      if (log.isDebugEnabled()) {
        long now = System.currentTimeMillis();
        log.debug(
            "Scheduled timeout (now="+now+" + t="+timeout+" == "+
            (now+timeout)+") "+timerTask);
      }
      threadService.schedule(timerTask, timeout);
    }
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

  private void fetchNow(CacheEntry entry) {
    if (log.isDebugEnabled()) {
      log.debug("Starting fetch "+entry);
    }
    // IDEA: return if entry.responses.isEmpty()?
    Object value = submitNow(entry.req);
    // assert value TTL >= now
    synchronized (entry) {
      entry.value = value;
      if (log.isDebugEnabled()) {
        log.debug("Set value "+entry);
      }
      if (entry.responses.isEmpty()) {
        if (log.isDebugEnabled()) {
          log.debug("No responses to set");
        }
        return;
      }
      queueResponses.addAll(entry.responses);
      entry.responses.clear();
      if (log.isDebugEnabled()) {
        log.debug("Will set "+queueResponses.size()+" responses");
      }
    }
    // IDEA: do this in a separate thread?
    for (int i = 0, n = queueResponses.size(); i < n; i++) {
      Object o = queueResponses.get(i);
      Response ri;
      if (o instanceof WrappedResponse) {
        ri = ((WrappedResponse) o).res;
      } else {
        ri = (Response) o;
      }
      if (log.isDebugEnabled()) {
        log.debug("Setting result["+i+" / "+n+"]: "+ri);
      }
      ri.setResult(value);
    }
    if (log.isDebugEnabled()) {
      log.debug("Cleaning temporary responses list");
    }
    queueResponses.clear();
  }

  // fetch-later queue

  private void fetchLater(CacheEntry entry) {
    synchronized (queuePending) {
      if (log.isDebugEnabled()) {
        log.debug("Locked fetchLater("+entry+"), adding entry");
      }
      queuePending.add(entry);
      if (log.isDebugEnabled()) {
        log.debug(
            "Unlocking fetchLater("+entry+")");
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
      private final List tmp = new ArrayList();
      public void run() {
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
          CacheEntry entry = (CacheEntry) tmp.get(i);
          fetchNow(entry);
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
    public final Request req;
    public Object value;
    public final Set responses = new HashSet(13);
    public CacheEntry(Request req) {
      this.req = req;
    }
    public String toString() {
      synchronized (this) {
        return 
          "(entry"+
          " oid="+System.identityHashCode(this)+
          " req="+req+
          " val="+value+
          " res["+responses.size()+"]="+responses+")";
      }
    }
  }

  // ------------------------------------------------------------------------
  // NS expired-TTL cleaner:

  // TODO

  // ------------------------------------------------------------------------
  // JNDI WP representation:

  private Attributes createAttributes(AddressEntry ae) {
    Attributes ats = new BasicAttributes();
    ats.put(NAME_ATTR, ae.getName());
    ats.put(APP_ATTR,  ae.getApplication());
    ats.put(URI_ATTR,  ae.getAddress());
    ats.put(CERT_ATTR, ae.getCert());
    ats.put(TTL_ATTR,  Long.toString(ae.getTTL()));
    return ats;
  }

  private AddressEntry createAddressEntry(Attributes ats) {
    String name = (String) getAttribute(ats,NAME_ATTR);
    Application app = (Application) getAttribute(ats,APP_ATTR);
    URI uri  = (URI) getAttribute(ats,URI_ATTR);
    Cert cert = (Cert) getAttribute(ats,CERT_ATTR);
    long ttl  = Long.parseLong((String) getAttribute(ats,TTL_ATTR));
    return new AddressEntry(name,app,uri,cert,ttl);
  }

  private void dirtyCache(String key) {
    String name = extractName(key);
    if (name != null) {
      dirtyName(name);
    }
  }

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

  private List fetchAll(Attributes match) {
    List ret;
    try {
      NamingEnumeration e =
        getWPContext().search(
            "",
            match,
            null);
      if (!(e.hasMore())) {
        ret = Collections.EMPTY_LIST;
      } else {
        ret = new ArrayList(13);
        do {
          SearchResult result = (SearchResult) e.next();
          if (result != null) {
            Attributes ats = result.getAttributes();
            if (ats != null) {
              ret.add(ats);
            }
          }
        } while (e.hasMore());
      }
    } catch (NamingException e) {
      if (log.isDebugEnabled()) {
        log.debug("Unable to fetchAll("+match+")", e);
      }
      throw new RuntimeException(
          "Unable to access name server", e);
    }
    return ret;
  }

  private void unregister(String name) throws NamingException {
    DirContext ctx = getWPContext();
    ctx.unbind(name);
  }

  private void register(
      String name, Attributes ats) throws NamingException {
    DirContext ctx = getWPContext();
    ctx.rebind(name, "ignored", ats);
  }

  // util:
  private Object getAttribute(Attributes ats, String id) {
    return getAttributeValue(ats, id, true);
  }
  private Object getAttributeValue(Attributes ats, String id) {
    return getAttributeValue(ats, id, false);
  }
  private Object getAttributeValue(
      Attributes ats, String id, boolean confirm) {
    if (ats == null) {
      if (confirm) {
        throw new RuntimeException(
            "Null attributes set");
      } else {
        return null;
      }
    }
    Attribute at = ats.get(id);
    if (at == null) {
      if (confirm) {
        throw new RuntimeException(
            "Unknown attribute \""+id+"\"");
      } else {
        return null;
      }
    }
    Object val;
    try {
      val = at.get();
    } catch (NamingException ne) {
      throw new RuntimeException(
          "Unable to get value for attribute \""+id+"\"",
          ne);
    }
    if (val == null) {
      if (confirm) {
        throw new RuntimeException(
            "Null value for attribute \""+id+"\"");
      } else {
        return null;
      }

    }
    return val;
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
        dirtyCache(key);
      }
      public void objectRemoved(NamingEvent evt) {
        String key = evt.getOldBinding().getName();
        dirtyCache(key);
      }
      public void objectRenamed(NamingEvent evt) {
        /*
           String key = evt.getNewBinding().getName();
           dirtyCache(key);
         */
      }
      public void objectChanged(NamingEvent evt) {
        String key = evt.getNewBinding().getName();
        dirtyCache(key);
      }
      public void namingExceptionThrown(NamingExceptionEvent evt) {
      }
    }
}

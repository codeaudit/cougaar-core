/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.core.node;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.IncarnationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.IdentityHashSet;

/**
 * This component provides the {@link IncarnationService} that
 * monitors agent incarnation (version) numbers in the {@link
 * WhitePagesService} and notifies clients of any changes.
 * 
 * @property org.cougaar.core.node.incarnation.period
 * Milliseconds between white pages incarnation polling to detect
 * agent restarts, defaults to 43000. 
 */
public final class Incarnation
extends GenericStateModelAdapter
implements Component
{

  private static final long RESTART_CHECK_INTERVAL = 
    Long.getLong(
        "org.cougaar.core.node.incarnation.period",
        43000L).longValue();

  private ServiceBroker sb;

  private LoggingService log;
  private ServiceBroker rootsb;
  private WhitePagesService wps;

  private IncarnationSP isp;

  private Schedulable updateThread;

  // map of agent name to an entry with the most recently observed
  // incarnation and listener callbacks
  private final Map incarnationMap = new HashMap();

  // WP callbacks for non-blocking lookups
  private final Map pendingMap = new HashMap();

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void load() {
    super.load();

    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);

    // get root sb
    NodeControlService ncs = (NodeControlService)
     sb.getService(this, NodeControlService.class, null);
    if (ncs == null) {
      throw new RuntimeException(
          "Unable to obtain NodeControlService");
    }
    rootsb = ncs.getRootServiceBroker();
    sb.releaseService(this, NodeControlService.class, ncs);

    // get wp
    wps = (WhitePagesService) 
      sb.getService(this, WhitePagesService.class, null);
    if (wps == null) {
      throw new RuntimeException(
          "Unable to obtain WhitePagesService");
    }

    // get thread
    ThreadService threadService = (ThreadService)
      sb.getService(this, ThreadService.class, null);
    Runnable updateRunner = 
      new Runnable() {
        public void run() {
          updateIncarnations();
        }
      };
    updateThread = threadService.getThread(
        this, updateRunner, "Incarnation");
    sb.releaseService(this, ThreadService.class, threadService);

    // assume we're running
    updateThread.schedule(
        RESTART_CHECK_INTERVAL,
        RESTART_CHECK_INTERVAL);

    // advertise our service
    isp = new IncarnationSP();
    rootsb.addService(IncarnationService.class, isp);
  }

  public void unload() {
    if (updateThread != null) {
      updateThread.cancelTimer();
      updateThread = null;
    }
    if (isp != null) {
      sb.revokeService(IncarnationService.class, isp);
      isp = null;
    }
    if (wps != null) {
      sb.releaseService(
          this, WhitePagesService.class, wps);
      wps = null;
    }
    super.unload();
  }

  private long getIncarnation(
      MessageAddress agentId,
      IncarnationService.Callback cb) {
    synchronized (incarnationMap) {
      Entry e = (Entry) incarnationMap.get(agentId);
      if (e == null) {
        return 0;
      }
      return e.getIncarnation(cb);
    }
  }

  public boolean subscribe(
      MessageAddress agentId,
      IncarnationService.Callback cb,
      long initialInc) {
    if (agentId == null || cb == null) {
      throw new IllegalArgumentException(
          "null "+(agentId == null ? "addr" : "cb"));
    }
    synchronized (incarnationMap) {
      Entry e = (Entry) incarnationMap.get(agentId);
      if (e == null) {
        if (log.isInfoEnabled()) {
          log.info("Adding "+agentId);
        }
        e = new Entry();
        incarnationMap.put(agentId, e);
      }
      boolean ret = e.addCallback(cb, initialInc);
      if (log.isDetailEnabled()) {
        log.detail(
            "addCallback("+agentId+", "+cb+", "+initialInc+")="+ret);
      }
      // if initialInc != e.inc, we'll update it on our
      // next updateIncarnations()
      return ret;
    }
  }

  public boolean unsubscribe(
      MessageAddress agentId,
      IncarnationService.Callback cb) {
    if (agentId == null || cb == null) {
      throw new IllegalArgumentException(
          "null "+(agentId == null ? "addr" : "cb"));
    }
    synchronized (incarnationMap) {
      Entry e = (Entry) incarnationMap.get(agentId);
      if (e == null) {
        return false;
      }
      if (!e.removeCallback(cb)) {
        return false;
      }
      if (!e.hasCallbacks()) {
        incarnationMap.remove(agentId);
        if (log.isInfoEnabled()) {
          log.info("Removing "+agentId);
        }
      }
      return true;
    }
  }

  /**
   * Periodically called to poll for remote agent incarnation
   * changes.
   */
  private void updateIncarnations() {
    if (log.isDebugEnabled()) {
      log.debug("updateIncarnations");
    }
    // snapshot the agent names
    Set agentIds;
    synchronized (incarnationMap) {
      if (incarnationMap.isEmpty()) {
        return; // nothing to do
      }
      agentIds = new HashSet(incarnationMap.keySet());
    }
    // update the latest incarnations from the white pages
    for (Iterator iter = agentIds.iterator();
        iter.hasNext();
        ) {
      MessageAddress agentId = (MessageAddress) iter.next();
      long currentInc = lookupIncarnation(agentId);
      updateIncarnation(agentId, currentInc);
    }
  }

  private void updateIncarnation(
      MessageAddress agentId,
      long currentInc) {
    if (currentInc <= 0) {
      // wp cache miss or unknown
      return;
    }
    List callbacks = null;
    synchronized (incarnationMap) {
      Entry e = (Entry) incarnationMap.get(agentId);
      if (e == null) {
        // unsubscribed?
        return;
      }
      long cachedInc = e.getIncarnation(null);
      if (cachedInc < currentInc) {
        // increase
        if (log.isInfoEnabled()) {
          log.info(
              "Update agent "+agentId+
              " from "+cachedInc+
              " to "+currentInc);
        }
      }
      callbacks = e.updateIncarnation(currentInc);
    }
    // invoke callbacks in our thread
    int n = (callbacks == null ? 0 : callbacks.size());
    for (int i = 0; i < n; i++) {
      IncarnationService.Callback cb = (IncarnationService.Callback)
        callbacks.get(i);
      if (log.isDebugEnabled()) {
        log.debug(
            "Invoking callback("+agentId+", "+currentInc+")["+
            i+" / "+n+"]: "+cb);
      }
      cb.incarnationChanged(agentId, currentInc);
    }
  }

  /**
   * White pages lookup to get the latest incarnation number for
   * the specified agent.
   *
   * @return -1 if the WP lacks an entry, -2 if a WP background
   * lookup is in progress, or &gt; 0 for a valid incarnation
   */
  private long lookupIncarnation(MessageAddress agentId) {
    AddressEntry entry;

    // runs in the updateThread, so no locking required
    BlockingWPCallback callback = (BlockingWPCallback)
      pendingMap.get(agentId);
    if (callback == null) {
      // no pending callback yet.
      callback = new BlockingWPCallback();
      wps.get(agentId.getAddress(), "version", callback);
      if (callback.completed) {
        // cache hit
        entry = callback.entry;
      } else {
        // no cache hit.  Remember the callback.
        pendingMap.put(agentId, callback);
        return -2;
      }
    } else if (callback.completed) {
      // pending callback completed
      entry = callback.entry;
      pendingMap.remove(agentId);
    } else {
      // pending callback not completed yet
      return -2;
    }

    if (entry == null) {
      // log this?
      // return error code
      return -1;
    }

    // parse the entry
    String path = entry.getURI().getPath();
    int end = path.indexOf('/', 1);
    String incn_str = path.substring(1, end);
    return Long.parseLong(incn_str);
  }

  /** an incarnationMap entry */
  private static final class Entry {

    // marker for an incarnation of zero
    private static final Long ZERO = new Long(0);

    // comparator that puts non-comparables first
    private static final Comparator CALLBACK_COMP =
      new Comparator() {
        public int compare(Object o1, Object o2) {
          if (o1 instanceof Comparable) {
            if (o2 instanceof Comparable) {
              return ((Comparable) o1).compareTo(o2);
            } else {
              return 1;
            }
          } else if (o2 instanceof Comparable) {
            return -1;
          } else {
            return 0;
          }
        }
      };

    // map from Callback to Long incarnation, which is
    // typically ZERO to match our shared "inc"
    private Map callbacks;

    // cached incarnation.
    //
    // all callbacks with ZERO values are treated as if
    // they have our "inc".  If all our callbacks are ZERO
    // then we set "allZero" for a little optimization`
    private long inc;

    // true iff all "callbacks.values()" are ZERO
    private boolean allZero = true;

    public long getIncarnation(IncarnationService.Callback cb) {
      if (cb == null) {
        return inc;
      }
      Long l = (Long) callbacks.get(cb);
      if (l == null || l == ZERO) {
        return inc;
      }
      return l.longValue();
    }

    public boolean hasCallbacks() {
      return (callbacks != null && !callbacks.isEmpty());
    }

    public boolean addCallback(
        IncarnationService.Callback cb,
        long cbInc) {
      if (callbacks == null) {
        // use a regular HashMap and sort on "update", instead of
        // using a more expensive TreeMap.
        callbacks = new HashMap();
      } else if (callbacks.containsKey(cb)) {
        return false;
      }
      Long l;
      if (cbInc == 0) {
        // client doesn't have an initial incarnation setting
        l = ZERO; 
      } else if (cbInc == inc) {
        // same as us
        l = ZERO; 
      } else {
        // ahead or behind us?
        l = new Long(cbInc);
        allZero = false;
      }
      callbacks.put(cb, l);
      return true;
    }

    public boolean removeCallback(IncarnationService.Callback cb) {
      if (callbacks == null) {
        return false;
      }
      Long l = (Long) callbacks.remove(cb);
      if (l == null) {
        return false;
      }
      if (l != ZERO) {
        if (!allZero) {
          // recalculate allZero
          allZero = true;
          for (Iterator iter = callbacks.values().iterator();
              iter.hasNext();
              ) {
            Long l2 = (Long) iter.next();
            long cbInc = (l2 == null ? 0 : l2.longValue());
            if (cbInc != 0 && cbInc != inc) {
              allZero = false;
              break;
            }
          }
        }
      }
      return true;
    }

    /**
     * update callbacks with the new incarnation, return
     * a sorted list of callbacks to notify.
     */
    public List updateIncarnation(long currentInc) {
      boolean updated = false;
      if (inc == 0) {
        // first time
        inc = currentInc;
      } else if (inc == currentInc) {
        // no change
      } else if (inc < currentInc) {
        // increase
        updated = true;
        inc = currentInc;
      } else {
        // huh?  ignore stale wp entry
      }
      List ret = null;
      if (callbacks.isEmpty()) {
        // no callbacks?
      } else if (allZero) {
        // all callback.inc's are zero
        if (updated) {
          // update all callbacks
          ret = new ArrayList(callbacks.keySet());
        } else {
          // no change
        }
      } else {
        // must scan entry for callback incarnation overrides
        allZero = true;
        for (Iterator iter = callbacks.entrySet().iterator();
            iter.hasNext();
            ) {
          Map.Entry me = (Map.Entry) iter.next();
          Object cb = me.getKey();
          Long l = (Long) me.getValue();
          long cbInc = (l == null ? 0 : l.longValue());
          boolean add = false;
          if (cbInc == 0) {
            // same as allZero case
            add = updated;
          } else if (cbInc < currentInc) {
            // update this callback, then it's in sync
            callbacks.put(cb, ZERO); 
            add = true;
          } else if (cbInc == currentInc) {
            // already saw this change, but now in sync
            callbacks.put(cb, ZERO); 
          } else if (cbInc > currentInc) {
            // in the future?
            allZero = false;
          }
          if (!add) {
            continue;
          }
          if (ret == null) {
            ret = new ArrayList();
          }
          ret.add(cb);
        }
      }
      // sort callbacks
      if (ret != null) {
        Collections.sort(ret, CALLBACK_COMP);
      }
      return ret;
    }

    public String toString() {
      return
        "(entry inc="+inc+" callbacks["+
        (callbacks == null ? 0 : callbacks.size())+
        "]="+callbacks+")";
    }
  }

  private final class IncarnationSP
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (IncarnationService.class.isAssignableFrom(serviceClass)) {
          return new Impl();
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor, 
          Class serviceClass, Object service) {
        if (!(service instanceof Impl)) {
          return;
        }
        ((Impl) service).unsubscribeAll();
      }

      private class Impl implements IncarnationService {
        // map from agentId to callbacks
        private final Map subs = new HashMap();

        public long getIncarnation(
            MessageAddress addr) throws IncarnationNotKnownException {
          MessageAddress agentId = addr.getPrimary();
          long ret = Incarnation.this.getIncarnation(agentId, null);
          if (ret <= 0) {
            throw new IncarnationNotKnownException();
          }
          return ret;
        }

        public long getIncarnation(
            MessageAddress addr,
            Callback cb) {
          MessageAddress agentId = addr.getPrimary();
          // verify that our subs contains this callback?
          long ret = Incarnation.this.getIncarnation(agentId, cb);
          return ret;
        }

        public Map getSubscriptions() {
          synchronized (subs) {
            return
              (subs.isEmpty() ?
               Collections.EMPTY_MAP :
               (new HashMap(subs)));
          }
        }

        public boolean subscribe(MessageAddress addr, Callback cb) {
          return subscribe(addr, cb, 0);
        }

        public boolean subscribe(
            MessageAddress addr,
            Callback cb,
            long initialInc) {
          MessageAddress agentId = addr.getPrimary();
          synchronized (subs) {
            Set s = (Set) subs.get(agentId);
            if (s == null) {
              s = new IdentityHashSet();
              subs.put(agentId, s);
            }
            if (!s.add(cb)) {
              // already have this subscription
              return false;
            }
            return Incarnation.this.subscribe(agentId, cb, initialInc);
          }
        }

        public boolean unsubscribe(MessageAddress addr, Callback cb) {
          MessageAddress agentId = addr.getPrimary();
          synchronized (subs) {
            Set s = (Set) subs.get(agentId);
            if (s == null) {
              return false;
            }
            if (!s.remove(cb)) {
              return false;
            }
            return Incarnation.this.unsubscribe(agentId, cb);
          }
        }

        private void unsubscribeAll() {
          synchronized (subs) {
            for (Iterator iter = subs.entrySet().iterator();
                iter.hasNext();
                ) {
              Map.Entry me = (Map.Entry) iter.next();
              MessageAddress agentId = (MessageAddress) me.getKey();
              Set s = (Set) me.getValue();
              for (Iterator i2 = s.iterator();
                  i2.hasNext();
                  ) {
                Callback cb = (Callback) i2.next();
                Incarnation.this.unsubscribe(agentId, cb);
              }
            }
            subs.clear();
          }
        }
      }
    }

  // replace with a Latch?
  private class BlockingWPCallback implements Callback {
    AddressEntry entry;
    boolean completed = false;

    public void execute(Response response) {
      completed = true;
      if (response.isSuccess()) {
        if (log.isDetailEnabled()) {
          log.detail("wp response: "+response);
        }
        entry = ((Response.Get) response).getAddressEntry();
      } else {
        if (log.isDetailEnabled()) {
          log.detail("wp error: "+response);
        }
      }
    }
  }

}

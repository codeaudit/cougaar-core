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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is a base class for bootstrap components
 * that handle registry bootstrapping.
 * <p>
 * The jobs include:
 * <ul>
 *   <li>Resolve bootstrap registry URLs to resolved "hint" entries
 *       by looking in external registries</li><p>
 *   <li>Watch for local binds that should be passed to
 *       external registries.</li><p>
 *   <li>Create alias "hint" entries for bootstrap entries that have
 *       (name != path) values, which is used to find the first
 *       agent that registers.</li><p>
 * </ul>
 */
public abstract class BootstrapLookupBase
extends GenericStateModelAdapter
implements Component
{
  protected ServiceBroker sb;

  protected LoggingService logger;
  protected MessageAddress agentId;
  protected ThreadService threadService;

  private BindObserverService bindObserverService;

  protected WhitePagesService wps;

  protected String agentName;

  // Map<String, LookupTimer>
  protected final Map table = new HashMap();

  private final BindObserverService.Client myClient = 
    new BindObserverService.Client() {
      public void submit(Request req) {
        BootstrapLookupBase.this.mySubmit(req);
      }
    };

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  public void load() {
    super.load();

    // which agent are we in?
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);
    agentName = agentId.getAddress();

    // register our bind-observer
    bindObserverService = (BindObserverService)
      sb.getService(
          myClient, BindObserverService.class, null);
    if (bindObserverService == null) {
      throw new RuntimeException(
          "Unable to obtain BindObserverService");
    }
  }

  public void unload() {
    super.unload();

    if (bindObserverService != null) {
      sb.releaseService(
          myClient, BindObserverService.class, bindObserverService);
      bindObserverService = null;
    }

    if (threadService != null) {
      // halt our threads?
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }

    if (wps != null) {
      sb.releaseService(this, WhitePagesService.class, wps);
      wps = null;
    }
  }

  /**
   * Returns true if the entry in the bootstrap table
   * is of interest as a LookupTimer bootEntry.
   * <p>
   * E.g.<pre>
   *   (WP, -RMI_REG, rmi://foo.com:123/AgentX)
   * </pre>
   */
  protected abstract boolean isBootEntry(AddressEntry entry);

  /**
   * Returns true if the entry bound by a local WP client
   * is of interest as a LookupTimer bindEntry.
   * <p>
   * E.g.<pre>
   *   (AgentX, -RMI, rmi://1.2.3.4:9876/oidA)
   * </pre>
   */
  protected abstract boolean isBindEntry(AddressEntry entry);

  /**
   * Create a non-abstract instance of a LookupTimer.
   */
  protected abstract LookupTimer createLookupTimer(
      AddressEntry bootEntry);

  /**
   * Get the delay for the initial lookup, where subsequent failed
   * lookups double the delay until it reaches the
   * {@link #getMaxLookupDelay}.
   */
  protected abstract long getMinLookupDelay();
  protected abstract long getMaxLookupDelay();

  /**
   * Get the delay for the initial verify, where subsequent
   * successful verifications double the delay until it
   * reaches the {@link #getMaxLookupDelay}.
   */ 
  protected abstract long getMinVerifyDelay();
  protected abstract long getMaxVerifyDelay();

  /**
   * Return true if an alias is allowed to change.
   * <p>
   * E.g. if we did a lookup and created an alias:<pre>
   *   (WP, alias, name:///AgentX)
   * </pre>
   * and later found that AgentY is in the registry,
   * the "allow alias" flag tells us if we should
   * update our local alias or keep the old one and
   * wait for AgentX to come back.
   */
  protected abstract boolean allowAliasChange();

  protected void mySubmit(Request req) {
    // watch bind/unbind activity
    if (req instanceof Request.Bind) {
      Request.Bind rb = (Request.Bind) req;
      AddressEntry ae = rb.getAddressEntry();
      if (req.hasOption(Request.CACHE_ONLY)) {
        hint(ae);
      } else {
        if (rb.isRenewal()) {
          // ignore
        } else if (rb.isOverWrite()) {
          rebind(ae);
        } else {
          bind(ae);
        }
      }
    } else if (req instanceof Request.Unbind) {
      Request.Unbind ub = (Request.Unbind) req;
      AddressEntry ae = ub.getAddressEntry();
      if (req.hasOption(Request.CACHE_ONLY)) {
        unhint(ae);
      } else {
        unbind(ae);
      }
    } else {
      // ignore
    }
  }

  protected void hint(AddressEntry bootEntry) {
    if (!isBootEntry(bootEntry)) {
      return;
    }
    URI uri = bootEntry.getURI();
    String id = uri.getPath().substring(1);
    if (id.equals("*")) {
      // wildcard race for binding, use node name
      id = agentName;
    }
    synchronized (table) {
      LookupTimer f = (LookupTimer) table.get(id);
      if (f != null) {
        f.destroy();
      }
      f = createLookupTimer(bootEntry);
      table.put(id, f);
    }
  }

  protected void unhint(AddressEntry bootEntry) {
    if (!isBootEntry(bootEntry)) {
      return;
    }
    String id = bootEntry.getURI().getPath().substring(1);
    if (id.equals("*")) {
      id = agentName;
    }
    synchronized (table) {
      LookupTimer f = (LookupTimer) table.remove(id);
      if (f != null) {
        f.destroy();
      }
    }
  }

  protected void bind(AddressEntry bindEntry) {
    if (!isBindEntry(bindEntry)) {
      return;
    }
    String name = bindEntry.getName();
    synchronized (table) {
      LookupTimer f = (LookupTimer) table.get(name);
      if (f != null) {
        f.bind(bindEntry);
      }
    }
  }

  protected void rebind(AddressEntry bindEntry) {
    bind(bindEntry);
  }

  protected void unbind(AddressEntry bindEntry) {
    if (!isBindEntry(bindEntry)) {
      return;
    }
    String name = bindEntry.getName();
    synchronized (table) {
      LookupTimer f = (LookupTimer) table.get(name);
      if (f != null) {
        f.unbind(bindEntry);
      }
    }
  }

  /** Generic utility method to check if a host is localhost */
  protected boolean isLocalHost(String addr) {
    // quick test for localhost...
    if (addr.equals("localhost") ||
        addr.equals("127.0.0.1") ||
        addr.equals("localHost") // bogus
       ) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "isLocalHost("+addr+") is true");
      }
      return true;
    }
    try {
      InetAddress de = InetAddress.getByName(addr);
      // quick test for "getLocalHost"
      InetAddress lh = InetAddress.getLocalHost();
      if (logger.isDetailEnabled()) {
        logger.detail(
            "isLocalHost("+addr+
            "), getByName("+addr+")="+de+
            ", getLocalHost()="+lh+
            ", equal="+(lh.equals(de)));
      }
      if (lh.equals(de)) {
        return true;
      }
      // check all network interfaces
      for (Enumeration e1 = NetworkInterface.getNetworkInterfaces();
          e1.hasMoreElements();
          ) {
        NetworkInterface iface = (NetworkInterface) e1.nextElement();
        for (Enumeration e2 = iface.getInetAddresses();
            e2.hasMoreElements();
            ) {
          InetAddress me = (InetAddress) e2.nextElement();
          if (logger.isDetailEnabled()) {
            logger.detail(
                "isLocalHost("+addr+
                "), getByName("+addr+")="+de+
                ", network_interface[?]="+me+
                ", equal="+(me.equals(de)));
          }
          if (me.equals(de)) {
            return true;
          }
        }
      }
    } catch (UnknownHostException e) {
      if (logger.isErrorEnabled()) {
        logger.error("isLocalHost("+addr+") failed", e);
      }
    } catch (SocketException e) {
      if (logger.isErrorEnabled()) {
        logger.error("isLocalHost("+addr+") failed", e);
      }
    }
    return false;
  }


  /**
   * Utility method to replace the host name in an AddressEntry with
   * the IP address of that host.
   * <p>
   * This is useful for protocols that are picky about partial or
   * aliased host names.  For example, if the specified entry is:<pre>
   *   (WP, -RMI_REG, rmi://foo.com:123/AgentX)
   * <pre> then this method will return:<pre>
   *   (WP, -RMI_REG, rmi://123.45.67.89:123/AgentX)
   * </pre>  This can be used in <code>createLookupTimer</code> to
   * override the bootstrap entry.
   */
  protected static AddressEntry resolveHostAddress(
      AddressEntry entry) throws UnknownHostException {
    URI uri = entry.getURI();
    String host = uri.getHost();
    if (host == null) {
      // host not specified?
      return entry;
    }
    InetAddress ia = InetAddress.getByName(host);
    String ip = ia.getHostAddress();
    if (host.equals(ip)) {
      // already an IP address
      return entry;
    }
    String scheme = uri.getScheme();
    int port = uri.getPort();
    String path = uri.getPath();
    String query = uri.getQuery();
    String fragment = uri.getFragment();
    String suri = 
      (scheme == null ? "" : scheme+":")+
      "//"+ip+
      (port < 0 ? "" : ":"+port)+
      (path == null ? "" : path)+
      (query == null ? "" : "?"+query)+
      (fragment == null ? "" : "#"+fragment);
    URI newURI = URI.create(suri);
    AddressEntry ret =
      AddressEntry.getAddressEntry(
          entry.getName(),
          entry.getType(),
          newURI);
    return ret;
  }

  /**
   * This manages the lookup and verification for a single
   * bootEntry.
   */
  protected abstract class LookupTimer
    implements Runnable {

      protected static final int LOOKUP = 1;
      protected static final int VERIFY = 2;
      protected static final int CANCEL = 3;

      //
      // construction-time finals:
      //

      /**
       * Our symbolic bootstrapped entry, e.g.<pre>
       *   (WP, -RMI_REG, rmi://foo.com:123/AgentX)
       * </pre>
       * where the name is "WP" and the id is "AgentX"
       * <p>
       * If the id and name don't match then an alias entry
       * will be created when the lookup completes.
       */
      protected final AddressEntry bootEntry;

      /** The identifier from the bootEntry path */
      protected final String id;

      /** our thread */
      protected final Schedulable thread;

      //
      // external interaction (outside the "run()" method)
      //
      // this is a very simple queue, since the only change
      // is a single queued bind/unbind entry.  We don't
      // need a "has_changed" flag since we can compare the
      // value to the bindEntry.
      //

      /** this lock is used to set the queuedBoundEntry */
      protected final Object lock = new Object();
      protected AddressEntry queuedBindEntry;


      //
      // private fields, only used within the "run()" method.
      //
      // The "run" is synchronized by the thread service.
      //

      protected int state;

      protected long delay;

      protected long wakeTime;

      /**
       * This is set when the agent with the bootEntry id
       * attempts to bind in the WP.
       * <p>
       * e.g.<pre>
       *   (AgentX, -RMI, rmi://1.2.3.4:9876/oidA)
       * </pre>
       * Note that the bootEntry id is used, not the name.
       */
      protected AddressEntry bindEntry;

      /**
       * When the state is VERIFY, this is the entry that was
       * found in the registry.  This entry is added to
       * the TableService.
       * <p>
       * E.g.<pre>
       *   (AgentY, -RMI, rmi://1.3.5.9:1234/oidB)
       * </pre>
       * If the name doesn't match the bootEntry id then
       * an alias should exist.
       */
      protected AddressEntry foundEntry;

      /**
       * This is the current id of the alias entry.
       * <p>
       * This matches the id if the bindEntry and foundEntry
       * are identical, otherwise it matches the aliasEntry's
       * path.
       * <p>
       * E.g.<pre>
       *    AgentY
       * </pre>
       * <p>
       * If `allowAliasChange()` is false, then this field
       * can't change.
       * <p>
       * For example, suppose that we initially locate AgentY
       * and alias WP to AgentY.  A subsequent verification
       * fails, we do a lookup again, and we find AgentZ's
       * entry in the registry.  If `allowAliasChange()`
       * is true, we should alias WP to AgentZ, otherwise
       * we should leave it as AgentY.
       * <p>
       * This also applies when (bindEntry == foundEntry) and
       * the aliasEntry is null, indicating a one-to-one
       * mapping.
       */
      protected String aliasId;

      /**
       * This is the alias entry that was created if the
       * bootEntry id doesn't match the foundEntry name.
       * <p>
       * This entry is added to the TableService.
       * <p>
       * E.g.<pre>
       *   (WP, alias, name:///AgentY)
       * </pre>
       * <p>
       * If the aliasId is immutable due to the
       * `allowAliasChange()` flag, then this entry is also
       * immutable.  The only difference is that the aliasId
       * will be non-null, whereas this field will be stuck
       * at null if the bindEntry and foundEntry initially
       * matched.
       */
      protected AddressEntry aliasEntry;

      public LookupTimer(AddressEntry bootEntry) {
        // configure
        this.bootEntry = bootEntry;
        URI uri = bootEntry.getURI();
        String id = uri.getPath().substring(1);
        if (id.equals("*")) {
          // wildcard race for binding
          id = agentName;
        }
        this.id = id;
        this.delay = getMinLookupDelay();
        this.state = LOOKUP;

        this.thread = threadService.getThread(
            BootstrapLookupBase.this, 
            this, 
            "White pages bootstrap "+bootEntry);

        // kick off thread
        thread.start();
      }

      public void bind(AddressEntry newBindEntry) {
        if (!id.equals(newBindEntry.getName())) {
          throw new IllegalArgumentException(
              "Bind entry doesn't match id ("+id+"): "+
              newBindEntry);
        }
        if (logger.isInfoEnabled()) {
          logger.info(
              "Observed local bind of "+newBindEntry+
              ", queueing relookup/bind of "+bootEntry);
        }
        queueBindAction(newBindEntry);
      }

      public void unbind(AddressEntry oldBindEntry) {
        if (!id.equals(oldBindEntry.getName())) {
          throw new IllegalArgumentException(
              "Unbind entry doesn't match id ("+id+"): "+
              oldBindEntry);
        }
        if (logger.isInfoEnabled()) {
          logger.info(
              "Observed local unbind of "+oldBindEntry+
              ", queueing relookup/bind of "+bootEntry);
        }
        queueBindAction(null);
      }

      public void destroy() {
        // fixme
      }

      protected void queueBindAction(AddressEntry entry) {
        synchronized (lock) {
          queuedBindEntry = entry;
        }
        thread.start();
      }

      public void run() {
        if (logger.isDetailEnabled()) {
          logger.detail("Running "+this);
        }

        // get the queued bind/unbind
        AddressEntry qEntry;
        synchronized (lock) {
          qEntry = queuedBindEntry;
        }

        // check for queued bind/unbind
        if (qEntry == bindEntry) {
          // no change
          if (System.currentTimeMillis() < wakeTime) {
            // unnecessary wake?
            return;
          }
        } else if (qEntry == null) {
          // unbound
          // assert (bindEntry != null);
          removeBinding();
        } else {
          if (bindEntry == null) {
            // bound
          } else {
            // rebound?
            removeBinding();
          }
          state = LOOKUP;
          bindEntry = qEntry;
        }

        // maybe transition state
        int oldState = state;
        if (state == LOOKUP) {
          lookup();
        } else if (state == VERIFY) {
          verify();
        }

        // set our next wake time
        if (state == oldState) {
          delay <<= 1;
          long max =
            (state == LOOKUP ?
             getMaxLookupDelay() :
             getMaxVerifyDelay());
          if (max < delay) {
            delay = max;
          }
        } else {
          long min =
            (state == LOOKUP ?
             getMinLookupDelay() :
             getMinVerifyDelay());
          delay = min;
        }

        if (state == CANCEL) {
          delay = 0;
        }

        if (0 <= delay) {
          wakeTime = System.currentTimeMillis() + delay;
          thread.schedule(delay);
        }

        if (logger.isDetailEnabled()) {
          logger.detail("Ran "+this);
        }
      }

      protected void removeBinding() {
        // on "unbind", if we exported our bindEntry to
        // external registries we should remove it now.
        //
        // implement me later...
        bindEntry = null;
        state = LOOKUP;
      }

      /**
       * Lookup the bootEntry in the registry, maybe register
       * ourselves if a local WP client has bound the name
       * (bindEntry != null).
       */
      protected abstract AddressEntry doLookup();

      protected void lookup() {
        String name = bootEntry.getName();
        URI uri = bootEntry.getURI();

        AddressEntry newFound = doLookup();
        if (newFound == null) {
          // lookup failed, try again later
          return;
        }

        // compare our id with the found id
        String foundId = newFound.getName();
        if ((allowAliasChange() || aliasId == null) ||
            aliasId.equals(foundId)) {
          // okay, we can create our alias
        } else {
          // we're not allowed to change the alias
          if (logger.isInfoEnabled()) {
            logger.info(
                "Ignoring lookup in "+uri+
                " under name ("+name+") where the foundId ("+
                foundId+") doesn't match the prior aliasId ("+
                aliasId+"), will attempt another lookup later");
          }
          // discard what we found, lookup again later
          return;
        }

        // create our alias (name => foundId)
        AddressEntry newAlias =
          ((allowAliasChange() || aliasId == null) ?
           (name.equals(foundId) ?
            (null) :
            createAlias(name, foundId)) :
           aliasEntry); // keep the old value

        // register in table service
        if (!newFound.equals(bindEntry)) {
          try {
            wps.hint(newFound);
          } catch (Exception e) {
            if (logger.isErrorEnabled()) {
              logger.error(
                  "Unable to create hint for found "+newFound+
                  ", which doesn't match the local "+bindEntry,
                  e);
            }
            // FIXME
          }
        }
        if ((allowAliasChange() || aliasId == null) &&
            newAlias != null) {
          try {
            wps.hint(newAlias);
          } catch (Exception e) {
            if (logger.isErrorEnabled()) {
              logger.error(
                  "Unable to create alias hint for "+newAlias,
                  e);
            }
            // FIXME
          }
        }

        // transition state
        state = VERIFY;
        foundEntry = newFound;
        aliasId = foundId;
        aliasEntry = newAlias;

        if (logger.isInfoEnabled()) {
          long now = System.currentTimeMillis();
          long verifyTime = now + getMinVerifyDelay();
          logger.info(
              (foundEntry.equals(bindEntry) ?
               "Bound" : // well, now they're ==
               "Located")+
              " "+bootEntry+
              ", found "+foundEntry+
              ", "+
              ((bindEntry == null) ?
               ("added bootstrap bind "+bindEntry) :
               !foundEntry.equals(bindEntry) ?
               ("ignore local bind "+bindEntry) :
               ("same as local bind"))+
              ", "+
              (newAlias == null ?
               ("no alias necessary") :
               ("added bootstrap alias "+aliasEntry))+
              ", will verify at "+
              Timestamp.toString(verifyTime,now));
        }
      }

      /**
       * Fetch the current entry that's in the registry.
       */
      protected abstract AddressEntry doVerify();

      protected void verify() {
        // get the current entry in the registry
        AddressEntry currEntry = doVerify();

        // make sure it hasn't changed
        if (currEntry != null &&
            currEntry.equals(foundEntry)) {
          // okay, still valid
          if (logger.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            long t = (delay << 1);
            if (getMaxVerifyDelay() < t) {
              t = getMaxVerifyDelay();
            }
            long verifyTime = now + t;
            logger.debug(
                "Verified that the bootstrap "+
                bootEntry+" still matches the found "+
                foundEntry+", will verify again at "+
                Timestamp.toString(verifyTime,now));
          }
          return;
        }

        // lookup again
        if (logger.isInfoEnabled()) {
          logger.info(
              "Forcing a lookup: the bootstrap "+bootEntry+
              ", which was previously resolved to "+
              foundEntry+", is now "+currEntry);
        }

        // remove bootstrap
        if (allowAliasChange() &&
            aliasEntry != null) {
          try {
            wps.unhint(aliasEntry);
          } catch (Exception e) {
            if (logger.isErrorEnabled()) {
              logger.error(
                  "Unable to remove alias hint: "+aliasEntry, e);
            }
          }
        }
        if (foundEntry != null) {
          try {
            wps.unhint(foundEntry);
          } catch (Exception e) {
            if (logger.isErrorEnabled()) {
              logger.error(
                  "Unable to remove found hint: "+foundEntry, e);
            }
          }
        }

        foundEntry = null;
        if (allowAliasChange()) {
          aliasEntry = null;
        }
        state = LOOKUP;
      }

      protected AddressEntry createAlias(String id, String name) {
        if (id.equals(name)) {
          throw new IllegalArgumentException(
              "No alias necessary for id="+id+" == name="+name);
        }
        return AddressEntry.getAddressEntry(
              id, "alias", URI.create("name:///"+name));
      }

      public String toString() {
        return
          " oid="+System.identityHashCode(this)+
          ", id="+id+
          ", boot="+bootEntry+
          ", state="+state+
          ", found="+foundEntry+
          ", alias="+aliasEntry+
          ", aliasId="+aliasId+
          ", delay="+delay;
      }
    }
}

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

package org.cougaar.core.wp.resolver.bootstrap;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is a base class for bootstrap components
 * that handle registry bootstrapping.
 * <p>
 * The jobs include:
 * <ul>
 *   <li>Resolve bootstrap registry URLs to resolved entries
 *       by looking in external registries</li><p>
 *   <li>Watch for local binds that should be passed to
 *       external registries.</li><p>
 *   <li>Create alias entries for bootstrap entries that have
 *       (name != path) values, which is used to find the first
 *       agent that registers.</li><p>
 * </ul>
 */
public abstract class BootstrapLookupBase
extends GenericStateModelAdapter
implements Component
{
  protected ServiceBroker sb;
  protected LoggingService logger = LoggingService.NULL;
  protected AgentIdentificationService agentIdService;
  protected ThreadService threadService;
  protected TableService tableService;

  protected String agentName;

  protected final TableService.Watcher watcher =
    createWatcher();

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setLoggingService(LoggingService ls) {
    logger = (ls == null ? LoggingService.NULL : ls);
  }

  public void setAgentIdentificationService(
      AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais != null) {
      this.agentName = ais.getMessageAddress().getAddress();
    }
  }

  public void setThreadService(ThreadService ts) {
    this.threadService =  ts;
  }

  public void setTableService(TableService ts) {
    this.tableService =  ts;
  }

  public void load() {
    super.load();
    tableService.register(watcher);
  }

  public void unload() {
    super.unload();

    if (tableService != null) {
      tableService.unregister(watcher);
      sb.releaseService(this, TableService.class, tableService);
      tableService = null;
    }

    if (threadService != null) {
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }

    if (agentIdService != null) {
      sb.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }

    if (logger != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, logger);
      logger = null;
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
   * Get the delay between LookupTimer lookups.
   * <p>
   * E.g. 30000  <i>milliseconds</i>.
   */
  protected abstract long getDelayForLookup();

  /**
   * Get the delay between LookupTimer alias retry, which is
   * only used if `allowAliasChange()` is false.
   * <p>
   * E.g. 120000  <i>milliseconds</i>.
   */
  protected abstract long getDelayForRetryAlias();

  /**
   * Get the delay between LookupTimer verification.
   * <p>
   * E.g. 120000  <i>milliseconds</i>.
   */
  protected abstract long getDelayForVerify();

  /**
   * Return true if an alias is allowed to change.
   * <p>
   * E.g. if we did a lookup and created an alias:<pre>
   *   (WP, alias, name://AgentX)
   * </pre>
   * and later found that AgentY is in the registry,
   * the "allow alias" flag tells us if we should
   * update our local alias or keep the old one and
   * wait for AgentX to come back.
   */
  protected abstract boolean allowAliasChange();

  /**
   * Create our Bootstrap/WP watcher.
   */
  protected TableService.Watcher createWatcher() {
    return new LookupManagementWatcher();
  }

  /** Generic utility method to check if a host is localhost */
  protected static boolean isLocalHost(String addr) {
    // quick test for localhost...
    if (addr.equals("localhost") ||
        addr.equals("localHost")        // bogus
       ) {
      return true;
    }
    try {
      InetAddress de = InetAddress.getByName(addr);
      InetAddress me = InetAddress.getLocalHost();
      return de.equals(me);
    } catch (java.net.UnknownHostException e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * A table service watcher that creates and controls the
   * LookupTimers.
   */
  protected class LookupManagementWatcher
    implements TableService.BindWatcher, TableService.TableWatcher {

      // Map<String, LookupTimer>
      protected final Map table = new HashMap();

      public void init(List bootEntries) {
        int n = (bootEntries == null ? 0 : bootEntries.size());
        for (int i = 0; i < n; i++) {
          AddressEntry bootEntry = (AddressEntry) bootEntries.get(i);
          added(bootEntry);
        }
      }

      public void added(AddressEntry bootEntry) {
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

      public void removed(AddressEntry bootEntry) {
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

      public void bind(AddressEntry bindEntry) {
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

      public void rebind(AddressEntry bindEntry) {
        bind(bindEntry);
      }

      public void unbind(AddressEntry bindEntry) {
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
    }

  /**
   * Base class for a TimerTask that uses the ThreadService.
   */
  protected abstract class TimerBase
    implements Runnable {

      private TimerTask restartTask;
      private Schedulable thread;

      public TimerBase() {
        thread = threadService.getThread(
            this, this, getName());
      }

      /**
       * Get the name of this timer.
       */
      public abstract String getName();

      /**
       * Run the timer task.
       *
       * Only one run will occur at a time, as defined by the
       * ThreadService's Schedulable restart API.
       */
      public abstract void run();

      /**
       * Schedule to run again later.
       */
      protected void scheduleRestart(long delay) {
        if (delay <= 0) {
          thread.start();
        } else {
          restartTask = new TimerTask() {
            public void run() {
              // don't run in the timer's thread!
              thread.start();
            }
          };
          threadService.schedule(restartTask, delay);
        }
      }

      /**
       * Schedule to run again as soon as possible.
       */
      protected void restart() {
        scheduleRestart(0);
      }
    }

  /**
   * This manages the lookup and verification for a single
   * bootEntry.
   */
  protected abstract class LookupTimer
    extends TimerBase {

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
       *   (WP, alias, name://AgentY)
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
        super();

        // configure
        this.bootEntry = bootEntry;
        URI uri = bootEntry.getURI();
        String id = uri.getPath().substring(1);
        if (id.equals("*")) {
          // wildcard race for binding
          id = agentName;
        }
        this.id = id;
        String host = uri.getHost();
        this.state = LOOKUP;

        // kick off thread
        restart();
      }

      public String getName() {
        return "(id="+id+" boot="+bootEntry+")";
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
        restart();
      }

      public void run() {
        if (logger.isDebugEnabled()) {
          logger.debug("Running "+this);
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

        long t;
        switch (state) {
          case LOOKUP:
            t = lookup();
            break;
          case VERIFY:
            t = verify();
            break;
          case CANCEL:
          default:
            t = -1;
            break;
        }
        if (t >= 0) {
          wakeTime = System.currentTimeMillis()+t;
          scheduleRestart(t);
        }

        if (logger.isDebugEnabled()) {
          logger.debug("Ran "+this);
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

      protected long lookup() {
        String name = bootEntry.getName();
        URI uri = bootEntry.getURI();

        AddressEntry newFound = doLookup();
        if (newFound == null) {
          // lookup failed, try again later
          long delay = getDelayForLookup();
          // enhancement idea: should we back off by doubling this
          // delay up to some max?
          return delay;
        }

        // compare our id with the found id
        String foundId = newFound.getName();
        if ((allowAliasChange() || aliasId == null) ||
            aliasId.equals(foundId)) {
          // okay, we can create our alias
        } else {
          // we're not allowed to change the alias
          long delay = getDelayForRetryAlias();
          if (logger.isInfoEnabled()) {
            logger.info(
                "Ignoring lookup in "+uri+
                " under name ("+name+") where the foundId ("+
                foundId+") doesn't match the prior aliasId ("+
                aliasId+"), will attempt another lookup in "+
                delay+" millis");
          }
          // discard what we found, lookup again later
          return delay;
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
          tableService.add(newFound);
        }
        if ((allowAliasChange() || aliasId == null) &&
            newAlias != null) {
          tableService.add(newAlias);
        }

        // transition state
        state = VERIFY;
        foundEntry = newFound;
        aliasId = foundId;
        aliasEntry = newAlias;

        if (logger.isInfoEnabled()) {
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
              ", will verify in "+
              getDelayForVerify()+" millis");
        }
        return getDelayForVerify();
      }

      /**
       * Fetch the current entry that's in the registry.
       */
      protected abstract AddressEntry doVerify();

      protected long verify() {
        // get the current entry in the registry
        AddressEntry currEntry = doVerify();

        // make sure it hasn't changed
        if (currEntry != null &&
            currEntry.equals(foundEntry)) {
          // okay, still valid
          long delay = getDelayForVerify();
          // enhancement idea: should be back off on a series
          // of successes?
          if (logger.isInfoEnabled()) {
            logger.info(
                "Verified that the bootstrap "+
                bootEntry+" still matches the found "+
                foundEntry+", will verify again in "+
                delay+" millis");
          }
          return delay;
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
          tableService.remove(aliasEntry);
        }
        if (foundEntry != null) {
          tableService.remove(foundEntry);
        }

        foundEntry = null;
        if (allowAliasChange()) {
          aliasEntry = null;
        }
        state = LOOKUP;

        // go right to lookup:
        return lookup();
      }

      protected AddressEntry createAlias(String id, String name) {
        AddressEntry alias = null;
        if (id.equals(name)) {
          throw new IllegalArgumentException(
              "No alias necessary for id="+id+" == name="+name);
        }
        return AddressEntry.getAddressEntry(
              id, "alias", URI.create("name://"+name));
      }

      public String toString() {
        return
          "LookupTimer {"+
          "\n  oid: "+System.identityHashCode(this)+
          "\n  id: "+id+
          "\n  boot: "+bootEntry+
          "\n  state: "+state+
          "\n  found: "+foundEntry+
          "\n  alias: "+aliasEntry+
          "\n  aliasId: "+aliasId+
          "\n}";
      }
    }
}

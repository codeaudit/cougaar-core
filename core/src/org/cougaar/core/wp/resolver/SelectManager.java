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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.wp.MessageTimeoutUtils;
import org.cougaar.core.wp.Parameters;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.core.wp.bootstrap.BootstrapService;
import org.cougaar.core.wp.bootstrap.ServersService;
import org.cougaar.core.wp.resolver.SelectService.Client;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.RarelyModifiedList;

/**
 * This class advertises the SelectService, which controls the
 * ClientTransport's server selection.
 * <p>
 * This implementation uses the BootstrapService to discover
 * servers and the PingService to send ping messages to the found
 * servers, measuring the round-trip-time (RTT).  Once we have a
 * server, we stop the discovery and use the found servers, selecting
 * between them based upon the RTT.  If the best RTT becomes
 * unacceptably low, we rediscover and re-ping new servers.
 * <p>
 * This component is pluggable, so it can be replaced with an
 * alternate implementation. 
 */
public class SelectManager
extends GenericStateModelAdapter
implements Component
{
  private ServiceBroker sb;
  private LoggingService logger;
  private ThreadService threadService;

  private SelectManagerConfig config;

  private Schedulable checkPingsThread;

  private BootstrapService bootstrapService;

  private SelectSP selectSP;

  private RarelyModifiedList selectClients = new RarelyModifiedList();

  private PingService pingService;

  private final PingService.Client pingClient =
    new PingService.Client() {
      public void pingAnswer(MessageAddress addr, long rtt) {
        SelectManager.this.pingAnswer(addr, rtt);
      }
    };

  private ServersService serversService;

  private final ServersService.Client serversClient =
    new ServersService.Client() {
      public void add(MessageAddress addr) {
        SelectManager.this.addServer(addr);
      }
      public void addAll(Set s) {
        for (Iterator iter = s.iterator(); iter.hasNext(); ) {
          addServer((MessageAddress) iter.next());
        }
      }
      public void remove(MessageAddress addr) {
        SelectManager.this.removeServer(addr);
      }
      public void removeAll(Set s) {
        for (Iterator iter = s.iterator(); iter.hasNext(); ) {
          removeServer((MessageAddress) iter.next());
        }
      }
    };

  private final Object lock = new Object();

  private boolean searching;
  private boolean foundAny;
  private long pingDeadline;

  // map from MessageAddress String address to Entry
  //
  // Map<String, Entry>
  private final Map entries = new HashMap();

  private long selectTime;
  private long selectTimeout;
  private MessageAddress selectAddr;

  // memoize the target, since messages with attributes don't
  // implement "equals()" or "hashCode()" correctly.  By using
  // a memoized value we allow our client to use a map of
  // addresses.
  private MessageAddress memoAddr;
  private long memoDeadline;
  private MessageAddress memoTarget;

  public void setParameter(Object o) {
    configure(o);
  }

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  private void configure(Object o) {
    if (config != null) {
      return;
    }
    config = new SelectManagerConfig(o);
  }

  public void load() {
    super.load();

    if (logger.isDebugEnabled()) {
      logger.debug("Loading resolver select manager");
    }

    configure(null);

    // monitor ping-based searching
    Runnable checkPingsRunner =
      new Runnable() {
        public void run() {
          // assert (thread == checkPingsThread);
          checkPings();
        }
      };
    checkPingsThread = threadService.getThread(
        this,
        checkPingsRunner,
        "White pages server selector check pings");

    // listen for the services
    ServiceFinder.Callback sfc =
      new ServiceFinder.Callback() {
        public void foundService(Service s) {
          SelectManager.this.foundService(s);
        }
      };
    ServiceFinder.findServiceLater(
        sb, PingService.class, pingClient, sfc);
    ServiceFinder.findServiceLater(
        sb, BootstrapService.class, null, sfc);
    ServiceFinder.findServiceLater(
        sb, ServersService.class, serversClient, sfc);

    // advertise our service
    selectSP = new SelectSP();
    sb.addService(SelectService.class, selectSP);
  }

  private void foundService(Service s) {
    synchronized (lock) {
      if (s instanceof PingService) {
        pingService = (PingService) s;
        if (searching) {
          sendPings();
        }
      } else if (s instanceof BootstrapService) {
        bootstrapService = (BootstrapService) s;
        if (searching) {
          bootstrapService.startSearching();
        }
      } else if (s instanceof ServersService) {
        serversService = (ServersService) s;
      }
    }
  }

  public void unload() {
    if (selectSP != null) {
      sb.revokeService(
          SelectService.class, selectSP);
      selectSP = null;
    }
    if (serversService != null) {
      sb.releaseService(
          serversClient,
          ServersService.class,
          serversService);
      serversService = null;
    }
    if (bootstrapService != null) {
      sb.releaseService(
          this,
          BootstrapService.class,
          bootstrapService);
    }
    if (pingService != null) {
      sb.releaseService(
          pingClient,
          PingService.class,
          pingService);
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }
    super.unload();
  }

  // timer fired, see if we've found our servers
  private void checkPings() {
    synchronized (lock) {
      if (!searching) {
        return;
      }
      // do we have at least one server?
      if (selectAddr != null && foundAny) {
        stopSearching();
        return;
      }
      // keep searching
      sendPings();
    }
  }

  private void startSearching() {
    if (searching) {
      return;
    }
    if (logger.isInfoEnabled()) {
      logger.info(
          "Starting bootstrap search, sending pings to servers["+
          entries.size()+"]="+entries.keySet());
    }
    searching = true;
    if (bootstrapService != null) {
      bootstrapService.startSearching();
    }
    // send pings to the servers we know about
    sendPings();
  }

  private void stopSearching() {
    if (!searching) {
      return;
    }
    searching = false;
    if (logger.isInfoEnabled()) {
      logger.info(
          "Stopping bootstrap search, found servers["+
          entries.size()+"]="+entries.keySet());
    }
    pingDeadline = -1;
    foundAny = false;
    if (bootstrapService != null) {
      bootstrapService.stopSearching();
    }
  }

  private void sendPings() {
    long now = System.currentTimeMillis();
    pingDeadline = now + config.pingTimeout;
    if (pingService == null) {
      return;
    }
    checkPingsThread.schedule(config.pingTimeout);
    long dropTime =
      (config.dropAge >= 0 ?
       (now - config.dropAge) :
       0);
    for (Iterator iter = entries.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      String name = (String) me.getKey();
      Entry e = (Entry) me.getValue();
      if (e.getUpdateTime() < dropTime) {
        if (logger.isInfoEnabled()) {
          logger.info("Dropping unused server: "+e.toString(now));
        }
        iter.remove();
        continue;
      }
      MessageAddress addr = e.getMessageAddress();
      if (logger.isDebugEnabled()) {
        logger.debug("Sending ping["+entries.size()+"] to "+addr);
      }
      pingService.ping(addr, pingDeadline);
    }
  }

  // receive a ping-ack.
  // if it's our first server, makeSelection and tell our clients.
  // wait for the timer to stopSearching, to allow more servers.
  private void pingAnswer(MessageAddress addr, long rtt) {
    if (logger.isDetailEnabled()) {
      logger.detail("pingAnswer("+addr+", "+rtt+")");
    }
    synchronized (lock) {
      String s = addr.getAddress();
      Entry e = (Entry) entries.get(s);
      if (e == null) {
        return;
      }
      long now = System.currentTimeMillis();
      e.reset((int) rtt, now);
      if (!searching) {
        return;
      }
      if (selectAddr != null && foundAny) {
        // another server, keep searching
        return;
      }
      makeSelection(now);
      if (selectAddr == null) {
        // server is bad?
        return;
      }
      foundAny = true;
    }
    // now that we have a server, tell our clients
    tellClients();
  }

  // add a server.
  // if we're searching, send a ping to it.
  // wait for a ping-ack before stopSearching or makeSelection
  private void addServer(MessageAddress addr) {
    if (logger.isDebugEnabled()) {
      logger.debug("Adding server "+addr);
    }
    synchronized (lock) {
      String s = addr.getAddress();
      Entry e = (Entry) entries.get(s);
      if (e != null) {
        return;
      }
      e = new Entry(addr);
      long now = System.currentTimeMillis();
      e.reset((int) config.lousyScore, now);
      entries.put(s, e);
      if (searching && pingService != null) {
        pingService.ping(addr, pingDeadline);
      }
      if (logger.isInfoEnabled()) {
        logger.info(
            "Added white pages server "+addr+" to servers["+
            entries.size()+"]="+entries.keySet());
      }
    }
  }

  // remove a server.
  private void removeServer(MessageAddress addr) {
    if (logger.isDebugEnabled()) {
      logger.debug("Removing server "+addr);
    }
    synchronized (lock) {
      String s = addr.getAddress();
      if (entries.remove(s) == null) {
        return;
      }
      clearSelection(addr);
      // if searching, fix foundAny?
      if (logger.isInfoEnabled()) {
        logger.info(
            "Removed white pages server "+addr+" from servers["+
            entries.size()+"]="+entries.keySet());
      }
      return;
    }
  }

  private MessageAddress select(boolean lookup, String name) {
    // select the entry with the min score
    synchronized (lock) {
      long now = System.currentTimeMillis();
      if (now > selectTime) {
        makeSelection(now);
      }
      if (selectAddr == null) {
        return null;
      }
      long deadline = now + selectTimeout;
      // round up the deadline for better memoizing
      if (config.deadlineMod > 1) {
        long mod = deadline % config.deadlineMod;
        if (mod > 0) {
          deadline += config.deadlineMod - mod;
        }
      }
      MessageAddress target;
      // memoize the target w/ deadline
      if ((selectAddr == memoAddr) &&
          (deadline == memoDeadline)) {
        target = memoTarget;
      } else {
        target =
          MessageTimeoutUtils.setDeadline(
              selectAddr,
              deadline);
        memoAddr = selectAddr;
        memoDeadline = deadline;
        memoTarget = target;
      }
      return target;
    }
  }

  private void makeSelection(long now) {
    selectTime = now + config.period;
    int n = entries.size();
    if (n <= 0) {
      // no servers yet
      selectAddr = null;
      selectTimeout = 1; // unused
      startSearching();
      return;
    }
    // select the best server
    Entry bestEntry;
    double bestScore = 0.0;
    Iterator iter = entries.entrySet().iterator();
    if (n == 1) {
      Map.Entry me = (Map.Entry) iter.next();
      bestEntry = (Entry) me.getValue();
      bestScore = computeScore(bestEntry, now);
    } else {
      bestEntry = null;
      for (int i = 0; i < n; i++) {
        Map.Entry me = (Map.Entry) iter.next();
        Entry e = (Entry) me.getValue();
        double score = computeScore(e, now);
        if (i == 0 || score < bestScore) {
          bestEntry = e;
          bestScore = score;
        }
      }
    }
    // if all servers are lousy, start searching for new ones
    if (bestScore >= config.lousyScore) {
      startSearching();
    }
    // attach timeout
    int timeout = bestEntry.getTimeout();
    if (timeout == 0) {
      timeout = config.defaultTimeout;
    } else if (timeout <= config.minTimeout) {
      timeout = config.minTimeout;
    } else if (config.maxTimeout < timeout) {
      timeout = config.maxTimeout;
    }
    MessageAddress addr = bestEntry.getMessageAddress();
    if (logger.isInfoEnabled()) {
     if (selectAddr == null ||
         !selectAddr.getAddress().equals(addr.getAddress())) {
       logger.info(
           "Selected server "+bestEntry.toString(now)+
           " with timeout "+timeout+
           " and score "+bestScore+
           ", prior selection was "+selectAddr);
     } else if (logger.isDebugEnabled()) {
       logger.debug(
           "Keeping server "+bestEntry.toString(now)+
           " with timeout "+timeout+
           " and score "+bestScore);
     }
    }
    selectTimeout = timeout;
    selectAddr = addr;
  }

  /**
   */
  private double computeScore(Entry e, long now) {
    // assert (Thread.holdsLock(lock));
    double ret = (double) e.getAverage();
    // favor primary server by adjusting its score
    boolean isPrimary =
      (config.primaryAddress != null &&
       config.primaryAddress.equals(
         e.getMessageAddress().getAddress()));
    if (isPrimary) {
      ret *= config.primaryWeight;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Score is "+ret+" for "+e.toString(now));
    }
    return ret;
  }

  /** clear the memoized selection if it matches the passed address */
  private void clearSelection(MessageAddress addr) {
    // assert (Thread.holdsLock(lock));
    if (selectAddr == null ||
        addr == null ||
        (!selectAddr.getAddress().equals(addr.getAddress()))) {
      return;
    }
    selectTime = 0;
    selectAddr = null;
  }

  /**
   * Update an entry score based upon measured performance.
   */
  private void update(Entry e, long rtt, boolean timeout, long now) {
    // assert (Thread.holdsLock(lock));
    e.update((int) rtt, now);
    if (logger.isDetailEnabled()) {
      logger.detail(
          "updated entry with "+
          (timeout ? "timeout" : "success")+
          " rtt="+rtt+": "+e.toString(now));
    }
    // force re-select if this is our selectAddr and it's bad now?
  }

  //
  // the rest is fairly straight-forward...
  //

  private void register(SelectService.Client c) {
    selectClients.add(c);
  }
  private void unregister(SelectService.Client c) {
    selectClients.remove(c);
  }
  private void tellClients() {
    List cl = selectClients.getUnmodifiableList();
    for (int i = 0, ln = cl.size(); i < ln; i++) {
      SelectService.Client c = (SelectService.Client) cl.get(i);
      c.onChange();
    }
  }

  private boolean contains(MessageAddress addr) {
    synchronized (lock) {
      String s = addr.getAddress();
      return entries.containsKey(s);
    }
  }

  private void update(
      MessageAddress addr, long rtt, boolean timeout) {
    synchronized (lock) {
      String s = addr.getAddress();
      Entry e = (Entry) entries.get(s);
      if (e != null) {
        long now = System.currentTimeMillis();
        update(e, rtt, timeout, now);
      }
    }
  }

  private String my_toString() {
    synchronized (lock) {
      return "(servers="+entries.toString()+")";
    }
  }

  /**
   * An entry monitors the performance for a single server.
   */
  private static class Entry {

    // alpha for average updates is 1/8
    private static final int GAIN_AVG = 3;

    // use a larger alpha for stdDev weight: 1/4
    private static final int GAIN_VAR = 2;

    // the timeout quartile is the same as the GAIN_VAR

    private final MessageAddress addr;
    private long updateTime;

    /** @see update */
    private int sa;
    private int sv;
    private int rto;

    public Entry(MessageAddress addr) {
      this.addr = addr;
      //
      String s =
        (addr == null ? "null addr" :
         null);
      if (s != null) {
        throw new IllegalArgumentException(s);
      }
    }

    public void reset(int rtt, long now) {
      updateTime = now;
      sa = rtt << GAIN_AVG;
      sv = 0;
      rto = rtt;
    }

    /**
     * Update our rtt average and std-dev.
     * <p>
     * For math see <i>Jacobson88</i>, sec2, appendix A.  This is
     * the standard TCP rtt-based timeout smoothing algorithm.
     *
     * @param rtt the measured round-trip-time
     * @param now the current time
     */
    public void update(int rtt, long now) {
      if (updateTime <= 0) {
        reset(rtt, now);
        return;
      }
      updateTime = now;
      int m = rtt;
      m -= (sa >> GAIN_AVG);
      sa += m;
      if (m < 0) {
        m = -m;
      }
      m -= (sv >> GAIN_VAR);
      sv += m;
      rto = (sa >> GAIN_AVG) + sv;
    }

    /** @return the time of the most recent update */
    public long getUpdateTime() {
      return updateTime;
    }

    /** @return the average round-trip-time */
    public int getAverage() {
       return sa >> GAIN_AVG;
    }

    /** @return the std-dev of the round-trip-time */
    public int getStdDev() {
       return sv >> GAIN_VAR;
    }

    /**
     * @return the proposed timeout, ignoring any decay based
     * upon the age of the update time.
     */
    public int getTimeout() {
      return rto;
    }

    public MessageAddress getMessageAddress() {
      return addr;
    }

    public String toString() {
      long now = System.currentTimeMillis();
      return toString(now);
    }

    public String toString(long now) {
      return
        "(server address="+addr+
        " updateTime="+
        Timestamp.toString(getUpdateTime(), now)+
        " average="+getAverage()+
        " stdDev="+getStdDev()+
        " timeout="+getTimeout()+
        ")";
    }
  }

  private class SelectSP extends ServiceProviderBase {
    protected void register(Object client) {
      SelectManager.this.register((SelectService.Client) client);
    }
    protected void unregister(Object client) {
      SelectManager.this.unregister((SelectService.Client) client);
    }
    protected Class getServiceClass() { return SelectService.class; }
    protected Class getClientClass() { return SelectService.Client.class; }
    protected Service getService(Object client) { return new SI(client); }
    protected class SI extends MyServiceImpl implements SelectService {
      public SI(Object client) { super(client); }
        public boolean contains(MessageAddress addr) {
          return SelectManager.this.contains(addr);
        }
        public MessageAddress select(boolean lookup, String name) {
          return SelectManager.this.select(lookup, name);
        }
        public void update(
            MessageAddress addr, long duration, boolean timeout) {
          SelectManager.this.update(addr, duration, timeout);
        }
        public String toString() {
          return SelectManager.this.my_toString();
        }
    }
  }

  /** config options */
  private static class SelectManagerConfig {
    public final long period;
    public final long dropAge;
    public final long deadlineMod;
    public final int minTimeout;
    public final int defaultTimeout;
    public final int maxTimeout;
    public final String primaryAddress;
    public final double primaryWeight;
    public final long pingTimeout;
    public final double lousyScore;

    public SelectManagerConfig(Object o) {
      Parameters p =
        new Parameters(o, "org.cougaar.core.wp.resolver.select.");
      period = p.getLong("period", 30000);
      dropAge = p.getLong("dropAge", 300000);
      double defaultScore = p.getDouble("defaultScore", 750.0);
      deadlineMod = p.getLong("deadlineMod", 25);
      minTimeout = p.getInt("minTimeout", 3000);
      defaultTimeout = p.getInt("defaultTimeout", 20000);
      maxTimeout = p.getInt("maxTimeout", 90000);
      String s = p.getString("primary");
      // in form "alias(:addr)?", extract "addr" suffix
      int idx = (s == null ? -1 : s.indexOf(':'));
      primaryAddress = (idx <= 0 ? s : s.substring(idx+1));
      double d = p.getDouble("favorPrimaryBy", 0.0);
      d = 1.0 - d;
      if (d > 1.0) {
        d = 1.0;
      } else if (d < 0.0) {
        d = 0.0;
      }
      primaryWeight = d;
      pingTimeout = p.getLong("pingTimeout", 30000);
      d = p.getDouble("lousyScore", 3000);
      d = Math.min(d, 4*defaultScore);
      lousyScore = d;
    }
  }
}

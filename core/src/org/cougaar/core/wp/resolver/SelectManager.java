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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.wp.MessageTimeoutUtils;
import org.cougaar.util.RarelyModifiedList;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This class advertises the SelectService, which manages the list
 * of white pages servers and ranks them based upon measured
 * performance. 
 * <p>
 * We measure the average and std-dev of the round-trip-time (RTT),
 * using the standard TCP smoothing algorithm (<i>Jacobson88</i>,
 * sec 2, appendix A).
 */
public class SelectManager
extends GenericStateModelAdapter
implements Component
{
  private SelectManagerConfig config;

  private ServiceBroker sb;
  private LoggingService logger;

  private SelectSP selectSP;

  private RarelyModifiedList clients = new RarelyModifiedList();

  private BindObserverService bindObserverService;

  private final BindObserverService.Client myClient = 
    new BindObserverService.Client() {
      public void submit(Request req) {
        SelectManager.this.observeBind(req);
      }
    };

  private final Object lock = new Object();

  // map from MessageAddress String address to Entry
  //
  // Map<String, Entry> 
  final Map entries = new HashMap();

  long selectTime;
  long selectTimeout;
  MessageAddress selectAddr;

  // memoize the target, since messages with attributes don't
  // implement "equals()" or "hashCode()" correctly.  By using
  // a memoized value we allow our client to use a map of
  // addresses.
  MessageAddress memoAddr;
  long memoDeadline;
  MessageAddress memoTarget;

  public void setParameter(Object o) {
    this.config = new SelectManagerConfig(o);
  }

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void load() {
    super.load();

    if (logger.isDebugEnabled()) {
      logger.debug("Loading resolver select manager");
    }

    // register to observe local binds
    bindObserverService = (BindObserverService)
      sb.getService(myClient, BindObserverService.class, null);
    if (bindObserverService == null) {
      throw new RuntimeException(
          "Unable to register bind observer");
    }

    // advertise our service
    selectSP = new SelectSP();
    sb.addService(SelectService.class, selectSP);
  }

  public void unload() {
    if (bindObserverService != null) {
      sb.releaseService(
          this, BindObserverService.class, bindObserverService);
      bindObserverService = null;
    }
    if (selectSP != null) {
      sb.revokeService(
          SelectService.class, selectSP);
      selectSP = null;
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }
    super.unload();
  }

  private MessageAddress select(boolean lookup, String name) {
    // select the entry with the min score
    synchronized (lock) {
      long now = System.currentTimeMillis();
      if (selectTime < now) {
        makeSelection(now);
      }
      if (selectAddr == null) {
        return null;
      }
      long deadline = now + selectTimeout;
      // round up the deadline for better memoizing
      if (1 < config.deadlineMod) {
        long mod = deadline % config.deadlineMod;
        if (0 < mod) {
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
      // no servers (yet?)
      selectAddr = null;
      selectTimeout = 1; // unused
      return;
    }
    // select the best server, using our "computeScore" method
    Entry bestEntry;
    double bestScore = 0.0;
    Iterator iter = entries.entrySet().iterator();
    if (n == 1) {
      Map.Entry me = (Map.Entry) iter.next();
      bestEntry = (Entry) me.getValue();
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
    double ret;
    long updateTime = e.getUpdateTime();
    long age = now - updateTime;
    double ageRatio;
    if (updateTime <= 0 || config.maxAge < age) {
      ret = config.defaultScore;
      ageRatio = 1.0;
    } else {
      ret = (double) e.getAverage();
      // make unused entries look better by decaying their score
      ageRatio = ((double) age / config.maxAge); 
      ret = ret + ageRatio*(config.defaultScore - ret);
    }
    // throw a little randomness into the mix
    double rnd = 1.0 + config.randomWeight*(Math.random() - 0.5);
    ret *= rnd;
    if (logger.isDetailEnabled()) {
      logger.detail(
          "adjust "+
          ((updateTime <= 0 || config.maxAge < age) ?
           ("default score "+config.defaultScore+" by ") :
           ("rtt avg "+e.getAverage()+
            " by age ("+age+" of "+config.maxAge+")="+
            ((int)(100.0*ageRatio))+
            "% and "))+
          "rand "+rnd+" yields a score of "+ret);
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
    int i = (int) rtt; 
    if (timeout) {
      // timeouts count double!
      i <<= 1;
    }
    e.update(i, now);
    if (logger.isDetailEnabled()) {
      logger.detail(
          "updated entry with "+
          (timeout ? "timeout" : "success")+
          " rtt="+rtt+": "+e.toString(now));
    }
    // force re-select if this is our selectAddr and it's bad now?
  }

  /**
   * If an address entry is in the form:<pre>
   *   (name=<i>ALIAS</i> type=alias uri=name:///<i>NAME</i>)
   * </pre> then this method parses out the <i>NAME</i>,
   * otherwise we return null.
   */
  public static String parseAlias(AddressEntry ae) {
    if (!"alias".equals(ae.getType())) {
      return null;
    }
    String alias = ae.getName();
    if (!alias.matches("WP(-\\d+)?")) {
      return null;
    }
    String path = ae.getURI().getPath();
    if (path == null && path.length() < 1) {
      return null;
    } 
    return path.substring(1);
  }

  private void observeBind(Request req) {
    boolean bind;
    AddressEntry ae;
    if (req instanceof Request.Bind) {
      bind = true;
      ae = ((Request.Bind) req).getAddressEntry();
    } else if (req instanceof Request.Unbind) {
      bind = false;
      ae = ((Request.Unbind) req).getAddressEntry();
    } else {
      return;
    }
    if (logger.isDetailEnabled()) {
      logger.detail("observe "+req);
    }
    String alias = ae.getName();
    String name = parseAlias(ae);
    if (name == null) {
      return;
    }
    MessageAddress addr = MessageAddress.getMessageAddress(name);
    if (bind) {
      add(alias, addr);
    } else {
      remove(alias);
    }

    if (logger.isInfoEnabled()) {
      logger.info(
          (bind ? "Added" : "Removed")+
          " white pages server "+addr+
          " based upon local "+
          (bind ? "" : "un")+
          "bind: "+ae+
          ", servers="+my_toString());
    }

    // tell our clients to check their sent messages, since we've
    // either added a new server (important if we had zero servers)
    // or we've removed a server (must revisit any messages we sent
    // to that server).
    tellClients(); 
  }

  //
  // the rest is fairly straight-forward...
  //

  private void register(SelectService.Client c) {
    clients.add(c);
  }
  private void unregister(SelectService.Client c) {
    clients.remove(c);
  }
  private void tellClients() {
    List cl = clients.getUnmodifiableList();
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

  private void add(String alias, MessageAddress addr) {
    synchronized (lock) {
      remove(alias);
      String s = addr.getAddress();
      Entry e = (Entry) entries.get(s);
      if (e == null) {
        if (entries.isEmpty()) {
          // force selection, we've found a server
          selectTime = 0;
        }
        e = new Entry(addr);
        entries.put(s, e);
      }
      e.addAlias(alias);
    }
  }

  private void remove(String alias) {
    synchronized (lock) {
      Entry match = null;
      for (Iterator iter = entries.values().iterator();
          iter.hasNext();
          ) {
        Entry e = (Entry) iter.next();
        if (e.containsAlias(alias)) {
          match = e;
          break;
        }
      }
      if (match == null) {
        return;
      }
      //
      match.removeAlias(alias);
      if (match.numAliases() == 0) {
        clearSelection(match.addr);
        String s = match.addr.getAddress();
        entries.remove(s);
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
  static class Entry {

    // alpha for average updates is 1/8
    private static final int GAIN_AVG = 3;

    // use a larger alpha for stdDev weight: 1/4
    private static final int GAIN_VAR = 2;

    // the timeout quartile is the same as the GAIN_VAR

    private final MessageAddress addr;
    private Set aliases = new HashSet();
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
        updateTime = now;
        sa = rtt << GAIN_AVG;
        sv = 0;
        rto = rtt;
      } else {
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

    public int numAliases() {
      return aliases.size();
    }
    public boolean containsAlias(String alias) {
      return aliases.contains(alias);
    }
    public void addAlias(String alias) {
      if (alias == null) {
        throw new IllegalArgumentException(
            "null alias");
      }
      aliases.add(alias);
    }
    public void removeAlias(String alias) {
      aliases.remove(alias);
    }

    public String toString() {
      long now = System.currentTimeMillis();
      return toString(now);
    }

    public String toString(long now) {
      return 
        "(server address="+addr+
        " alias="+aliases+
        " updateTime="+
        Timestamp.toString(getUpdateTime(), now)+
        " average="+getAverage()+
        " stdDev="+getStdDev()+
        " timeout="+getTimeout()+
        ")";
    }
  }

  private class SelectSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!SelectService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        if (!(requestor instanceof SelectService.Client)) {
          throw new IllegalArgumentException(
              "SelectService"+
              " requestor must implement "+
              "SelectService.Client");
        }
        SelectService.Client client = (SelectService.Client) requestor;
        SelectService ssi = new SelectServiceImpl(client);
        SelectManager.this.register(client);
        return ssi;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
        if (!(service instanceof SelectServiceImpl)) {
          return;
        }
        SelectServiceImpl ssi = (SelectServiceImpl) service;
        SelectService.Client client = ssi.client;
        SelectManager.this.unregister(client);
      }
      private class SelectServiceImpl 
        implements SelectService {
          private final Client client;
          public SelectServiceImpl(Client client) {
            this.client = client;
          }
          public boolean contains(MessageAddress addr) {
            return SelectManager.this.contains(addr);
          }
          public MessageAddress select(
              boolean lookup,
              String name) {
            return SelectManager.this.select(
                lookup, name);
          }
          public void update(
              MessageAddress addr,
              long duration,
              boolean timeout) {
            SelectManager.this.update(
                addr, duration, timeout);
          }
          public String toString() {
            return SelectManager.this.my_toString();
          }
        }
    }

  /** config options, soon to be parameters/props */
  private static class SelectManagerConfig {
    public static final long period =
      Long.parseLong(
          System.getProperty(
            "org.cougaar.core.wp.resolver.select.period",
            "30000"));
    public static final long maxAge =
      Long.parseLong(
          System.getProperty(
            "org.cougaar.core.wp.resolver.select.maxAge",
            "300000"));
    public static final double defaultScore =
      Double.parseDouble(
          System.getProperty(
            "org.cougaar.core.wp.resolver.select.defaultScore",
            "750.0"));
    public static final double randomWeight =
      Double.parseDouble(
          System.getProperty(
            "org.cougaar.core.wp.resolver.select.randomWeight",
            "0.3"));
    public static final long deadlineMod =
      Long.parseLong(
          System.getProperty(
            "org.cougaar.core.wp.resolver.select.deadlineMod",
            "25"));
    public static final int minTimeout =
      Integer.parseInt(
          System.getProperty(
            "org.cougaar.core.wp.resolver.select.minTimeout",
            "3000"));
    public static final int defaultTimeout =
      Integer.parseInt(
          System.getProperty(
            "org.cougaar.core.wp.resolver.select.defaultTimeout",
            "20000"));
    public static final int maxTimeout =
      Integer.parseInt(
          System.getProperty(
            "org.cougaar.core.wp.resolver.select.maxTimeout",
            "90000"));

    public SelectManagerConfig(Object o) {
      // FIXME parse!
    }
  }
}

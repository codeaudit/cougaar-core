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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageHandler;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.WhitePagesProtectionService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.util.UID;
import org.cougaar.core.wp.MessageTimeoutUtils;
import org.cougaar.util.RarelyModifiedList;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component sends and receives messages for the resolver.
 * <p>
 * This is the last stop for the resolver -- the request wasn't
 * in the cache and can't be batched with other already-pending
 * requests.
 * <p>
 * This component is responsible for the resolver-side hierarchy
 * traversal and replication.
 *
 * <pre>
 * @property org.cougaar.core.wp.resolver.transport.nagleMillis
 *   Delay in milliseconds before sending messages, to improve
 *   batching.  Defaults to zero.
 * @property org.cougaar.core.wp.resolver.transport.noListNagle
 *   Ignore the "nagleMillis" delay if the request is a new
 *   name list (e.g. "list ."), which is often a user request.
 *   Defaults to false. 
 * @property org.cougaar.core.wp.resolver.transport.graceMillis
 *   Extended message timeout deadline after startup.  Defaults to
 *   zero.
 * @property org.cougaar.core.wp.resolver.transport.checkDeadlinesPeriod
 *   Time in milliseconds between checks for message timeouts if
 *   there are any outstanding messages.  Defaults to 10000.
 * </pre> 
 */
public class ClientTransport
extends GenericStateModelAdapter
implements Component
{


  // this is a dummy address for messages that can't be
  // sent yet, e.g. because there are no WP servers.
  private static final MessageAddress NULL_ADDR =
    MessageTimeoutUtils.setTimeout(
        MessageAddress.getMessageAddress("wp-null"),
        15000);

  private ClientTransportConfig config;

  private ServiceBroker sb;
  private LoggingService logger;
  private MessageAddress agentId;
  private ThreadService threadService;
  private WhitePagesProtectionService protectS;

  private SelectService selectService;

  private LookupSP lookupSP;
  private ModifySP modifySP;

  private RarelyModifiedList lookupClients = 
    new RarelyModifiedList();
  private RarelyModifiedList modifyClients = 
    new RarelyModifiedList();

  private final SelectService.Client myClient = 
    new SelectService.Client() {
      public void onChange() {
        ClientTransport.this.onServerChange();
      }
    };

  //
  // output (send to WP server):
  //

  private final Object sendLock = new Object();

  private MessageSwitchService messageSwitchService;

  // this is our startup grace-time on message timeouts, which is
  // based upon the time we obtained our messageSwitchService plus
  // the configuration's "graceMillis".
  //
  // this is used to allow more delivery time when the system is
  // starting, since unusual costs usually occur (e.g. cryto
  // handshaking).
  private long graceTime;

  // lookup requests (name => Entry) that are either being delayed
  // (nagle) or have been sent but not ack'ed (outstanding).
  //
  // Map<String, Entry>
  private final Map lookups = new HashMap();

  // modify requests (name => Entry) that are either being delayed
  // (nagle) or have been sent but not ack'ed (outstanding).
  //
  // Map<String, Entry>
  private final Map mods = new HashMap();

  // temporary fields for use in "send" and related methods.
  // accessed within sendLock.
  private long now;
  private boolean sendNow;
  private boolean sendLater;
  private final Set lookupNames = new HashSet();
  private final Set modifyNames = new HashSet();
  private final Map lookupAddrs = new HashMap();
  private final Map modifyAddrs = new HashMap();
  
  // "nagle" delayed release
  private long releaseTime;
  private Schedulable releaseThread;

  // periodic check for late message acks
  private long checkDeadlinesTime;
  private Schedulable checkDeadlinesThread;

  //
  // input (receive from WP server):
  //

  private Schedulable receiveThread;

  // received messages
  //
  // List<WPAnswer>
  private final List receiveQueue = new ArrayList();

  // temporary list for use within "receiveNow()"
  //
  // List<Object>
  private final List receiveTmp = new ArrayList();

  //
  // statistics
  //

  private final Stats lookupStats = new Stats();
  private final Stats modifyStats = new Stats();

  public void setParameter(Object o) {
    this.config = new ClientTransportConfig(o);
  }

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  public void load() {
    super.load();

    if (logger.isDebugEnabled()) {
      logger.debug("Loading resolver remote handler");
    }

    // which agent are we in?
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);

    protectS =
      (WhitePagesProtectionService)
      sb.getService(this, WhitePagesProtectionService.class, null);
    if (logger.isDebugEnabled()) {
      logger.debug("White pages protection service: "+protectS);
    }

    // create threads
    if (config.nagleMillis > 0) { 
      Runnable releaseRunner =
        new Runnable() {
          public void run() {
            // assert (thread == releaseThread);
            releaseNow();
          }
        };
      releaseThread = threadService.getThread(
          this,
          releaseRunner,
          "White pages client \"nagle\" delayed sendler");
    }
    Runnable receiveRunner =
      new Runnable() {
        public void run() {
          // assert (thread == receiveThread);
          receiveNow();
        }
      };
    receiveThread = threadService.getThread(
        this,
        receiveRunner,
        "White pages client handle incoming responses");

    Runnable checkDeadlinesRunner =
      new Runnable() {
        public void run() {
          // assert (thread == checkDeadlinesThread);
          checkDeadlinesNow();
        }
      };
    checkDeadlinesThread = threadService.getThread(
        this,
        checkDeadlinesRunner,
        "White pages client transport send queue checker");

    // register to select servers
    selectService = (SelectService)
      sb.getService(myClient, SelectService.class, null);
    if (selectService == null) {
      throw new RuntimeException(
          "Unable to obtain SelectService");
    }

    // register our message switch (now or later)
    if (sb.hasService(MessageSwitchService.class)) {
      registerMessageSwitch();
    } else {
      ServiceAvailableListener sal =
        new ServiceAvailableListener() {
          public void serviceAvailable(ServiceAvailableEvent ae) {
            Class cl = ae.getService();
            if (MessageSwitchService.class.isAssignableFrom(cl)) {
              registerMessageSwitch();
            }
          }
        };
      sb.addServiceListener(sal);
    }

    // advertise our service
    lookupSP = new LookupSP();
    sb.addService(LookupService.class, lookupSP);
    modifySP = new ModifySP();
    sb.addService(ModifyService.class, modifySP);
  }

  public void unload() {
    if (modifySP != null) {
      sb.revokeService(ModifyService.class, modifySP);
      modifySP = null;
    }
    if (lookupSP != null) {
      sb.revokeService(LookupService.class, lookupSP);
      lookupSP = null;
    }

    MessageSwitchService mss;
    synchronized (sendLock) {
      mss = messageSwitchService;
      messageSwitchService = null;
    }
    if (mss != null) {
      //mss.removeMessageHandler(myMessageHandler);
      sb.releaseService(this, MessageSwitchService.class, mss);
      mss = null;
    }

    if (selectService != null) {
      sb.releaseService(
          myClient, SelectService.class, selectService);
      selectService = null;
    }

    if (protectS != null) {
      sb.releaseService(
          this, WhitePagesProtectionService.class, protectS);
      protectS = null;
    }

    if (threadService != null) {
      // halt our threads?
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }
    if (logger != null) {
      sb.releaseService(this, LoggingService.class, logger);
      logger = null;
    }

    super.unload();
  }

  private void registerMessageSwitch() {
    // service broker now has the MessageSwitchService
    //
    // should we do this in a separate thread?
    synchronized (sendLock) {
      if (messageSwitchService != null) {
        if (logger.isErrorEnabled()) {
          logger.error("Already obtained our message switch");
        }
        return;
      }
      // we could lock out additional register attempts
      // at this point, but they are not expected.
    }
    MessageSwitchService mss = (MessageSwitchService)
      sb.getService(this, MessageSwitchService.class, null);
    if (mss == null) {
      if (logger.isErrorEnabled()) {
        logger.error("Unable to obtain MessageSwitchService");
      }
      return;
    }
    MessageHandler myMessageHandler =
      new MessageHandler() {
        public boolean handleMessage(Message m) {
          return receive(m);
        }
      };
    mss.addMessageHandler(myMessageHandler);
    if (logger.isInfoEnabled()) {
      logger.info("Registered with message transport");
    }
    // should this be synchronized?
    synchronized (sendLock) {
      this.messageSwitchService = mss;
      long now = System.currentTimeMillis();
      if (config.graceMillis >= 0) {
        this.graceTime = now + config.graceMillis;
      }
      // schedule a "send"
      checkDeadlinesTime = now;
      checkDeadlinesThread.start();
    }
  }

  private void register(LookupService.Client lsc) {
    lookupClients.add(lsc);
  }
  private void unregister(LookupService.Client lsc) {
    lookupClients.remove(lsc);
  }

  private void register(ModifyService.Client usc) {
    modifyClients.add(usc);
  }
  private void unregister(ModifyService.Client usc) {
    modifyClients.remove(usc);
  }

  private void onServerChange() {
    // the list of servers has changed
    //
    // kick the thread, since either we've added a new server
    // (important if we had zero servers) or we've removed a
    // server (must revisit any messages we sent to that server).
    synchronized (sendLock) {
      checkDeadlinesTime = System.currentTimeMillis();
      checkDeadlinesThread.start();
    }
  }

  //
  // send:
  //

  private void lookup(Map m) {
    send(true, m);
  }

  private void modify(Map m) {
    send(false, m);
  }
  
  private void releaseNow() {
    // call "send" with null, which will examine the releaseTime
    send(true, null);
  }

  private void checkDeadlinesNow() {
    // call "send" with null, which will examine the checkDeadlinesTime
    send(false, null);
  }

  private void send(boolean lookup, Map m) {
    stats(lookup).send(m);

    // The various callers are:
    //   - our clients (cache, leases)
    //   - our own releaseThread (adds batching delay)
    //   - our own check checkDeadlinesThread (check for timeouts
    //     or new servers)
    // These last two clients pass a null map.

    // stuff we will send:  (target => map(name => sendObj))
    Map lookupsToSend;
    Map modifiesToSend;

    synchronized (sendLock) {
      try {
        // initialize temporary variables
        init();

        // create entries for the new queries
        checkSendMap(lookup, m);

        if (!hasMessageTransport()) {
          // no MTS yet?  We'll kick a thread when the MTS shows up
          return;
        }

        // check for delayed release entries, even if we're not the
        // releaseThread
        checkReleaseTimer();

        // check for message timeouts, even if we're not the
        // checkDeadlinesThread
        checkDeadlineTimer();

        if (!shouldReleaseNow()) {
          // our releaseThread will wake us later, allowing us to
          // batch these requests.
          return;
        }

        if (!collectMessagesToSend()) {
          // nothing to send.  Another possibility is that there are
          // no WP servers yet, in which case we'll kick a thread when
          // they show up.
          return;
        }

        // we're sending something now, so make sure we'll wake
        // up later to check timeouts
        ensureDeadlineTimer();

        // take maps stuff we will send
        lookupsToSend = takeMessagesToSend(true);
        modifiesToSend = takeMessagesToSend(false);
      } finally {
        cleanup();
      }
    }

    // send messages
    sendAll(lookupsToSend, modifiesToSend);
  }

  private void init() {
    now = System.currentTimeMillis();

    sendNow =
      (config.nagleMillis <= 0 ||
       (releaseTime > 0 && releaseTime <= now));

    sendLater = false;

    // these should already be cleared by "cleanup()":
    lookupNames.clear();
    modifyNames.clear();
    lookupAddrs.clear();
    modifyAddrs.clear();
  }

  private void checkSendMap(boolean lookup, Map m) {
    int n = (m == null ? 0 : m.size());
    if (n == 0) {
      return;
    }

    // check to see if this map contains a forced sendNow
    if (config.noListNagle && !sendNow) {
      Iterator iter = m.entrySet().iterator();
      for (int i = 0; i < n; i++) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Object query = me.getValue();
        if (mustSendNow(lookup, name, query)) {
          if (logger.isDetailEnabled()) {
            logger.detail(
                "mustSendNow("+lookup+", "+name+", "+query+")");
          }
          sendNow = true;
          break;
        }
      }
    }

    Map table = (lookup ? lookups : mods);
    Set names = (lookup ? lookupNames : modifyNames);
    Iterator iter = m.entrySet().iterator();
    for (int i = 0; i < n; i++) {
      Map.Entry me = (Map.Entry) iter.next();
      String name = (String) me.getKey();
      Object query = me.getValue();
      // select the target, add to queue
      if (!shouldSend(lookup, name, query, now)) {
        continue;
      }
      // add or replace the entry
      Entry e = new Entry(query, now);
      table.put(name, e);
      if (sendNow) {
        names.add(name);
        continue;
      }
      sendLater = true;
      if (logger.isDetailEnabled()) {
        logger.detail(
            "delaying initial release of "+
            (lookup ? "lookup" : "modify")+
            " "+name+"="+query);
      }
      stats(lookup).later();
    }
  }

  private boolean hasMessageTransport() {
    if (messageSwitchService != null) {
      return true;
    }
    if (logger.isDetailEnabled()) {
      logger.detail("waiting for message transport");
    }
    return false;
  }

  private void checkReleaseTimer() {
    if (!sendNow || config.nagleMillis <= 0) {
      return;
    }
    if (releaseTime > 0) {
      // timer is due
      releaseTime = 0;
      // cancel the timer.  This is a no-op if it's our thread.
      releaseThread.cancelTimer();
    }
    // find due entries (optimize me?)
    for (int t = 0; t < 2; t++) { 
      boolean tlookup = (t == 0);
      Map table = (tlookup ? lookups : mods);
      int tsize = table.size();
      if (tsize <= 0) {
        continue;
      }
      Set names = (tlookup ? lookupNames : modifyNames);
      Iterator iter = table.entrySet().iterator();
      for (int i = 0; i < tsize; i++) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Entry e = (Entry) me.getValue();
        if (e.getTarget() != null) {
          continue;
        }
        names.add(name);
      }
    }
  }

  private void checkDeadlineTimer() {
    if (checkDeadlinesTime <= 0 || checkDeadlinesTime > now) {
      return;
    }
    // timer is due
    checkDeadlinesTime = 0;
    // now's a good time to dump debugging info
    debugQueues();
    boolean anyStillPending = false;
    // find due entries (optimize me?)
    for (int t = 0; t < 2; t++) { 
      boolean tlookup = (t == 0);
      Map table = (tlookup ? lookups : mods);
      int tsize = table.size();
      if (tsize <= 0) {
        continue;
      }
      Set names = (tlookup ? lookupNames : modifyNames);
      Iterator iter = table.entrySet().iterator();
      for (int i = 0; i < tsize; i++) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Entry e = (Entry) me.getValue();
        MessageAddress target = e.getTarget();
        if (target == null) {
          // waiting for releaseThread
          continue;
        }
        if (target != NULL_ADDR) {
          if (selectService.contains(target)) {
            long deadline = e.getDeadline();
            if (deadline <= 0 || deadline > now) {
              // give it more time for the ack
              anyStillPending = true; 
              continue;
            } 
            if (shortcutNodeModify(tlookup, name, e, now)) {
              // unusual case: local-node uid-based modify
              continue;
            }
          }
          // update server stats
          selectService.update(
              target,
              (now - e.getSendTime()),
              true);
        }
        if (!sendNow && logger.isDetailEnabled()) {
          logger.detail(
              "delaying retry release of "+
              (tlookup ? "lookup" : "modify")+
              " "+name+"="+e.getQuery()+", entry="+e.toString(now));
        }
        stats(tlookup).retry();
        e.setTarget(null);
        if (sendNow) {
          names.add(name);
          continue;
        }
        sendLater = true;
        stats(tlookup).later();
      }
    }
    if (anyStillPending) {
      // schedule our next deadline check
      ensureDeadlineTimer();
    }
  }

  private boolean shouldReleaseNow() {
    if (sendNow || !sendLater) {
      return true;
    }
    // make sure timer is running to send later
    if (releaseTime == 0) {
      // start timer
      releaseTime = now + config.nagleMillis; 
      if (logger.isDetailEnabled()) {
        logger.detail("starting delayed release timer");
      }
      releaseThread.schedule(config.nagleMillis);
    }
    // wait for timer
    if (logger.isDetailEnabled()) {
      logger.detail(
          "waiting "+(releaseTime - now)+" for release timer");
    }
    return false;
  }

  private boolean collectMessagesToSend() {
    boolean anyToSend = false;
    for (int x = 0; x < 2; x++) {
      boolean xlookup = (x == 0);
      Set names = (xlookup ? lookupNames : modifyNames);
      if (names.isEmpty()) {
        continue;
      }
      Iterator iter = names.iterator();
      for (int i = 0, nsize = names.size(); i < nsize; i++) {
        String name = (String) iter.next();
        Map table = (xlookup ? lookups : mods);
        Entry e = (Entry) table.get(name);
        // accessing the "selectService" within our lock may be an
        // issue someday, but for now we'll assume it's allowed
        MessageAddress target = 
          selectService.select(xlookup, name);
        if (target == null) {
          // no target?  mark entry
          e.setTarget(NULL_ADDR); 
          if (logger.isDetailEnabled()) {
            logger.detail(
                "queuing message until WP servers are available: "+
                (xlookup ? "lookup" : "modify")+" "+e.toString(now));
          }
          continue;
        }
        e.setTarget(target);

        // wrap query for security
        Object query = e.getQuery();
        Object sendObj = query;
        if (query != null) {
          sendObj = wrapQuery(xlookup, name, query);
          if (sendObj == null) {
            // wrapping rejected this query
            table.remove(name); 
            continue;
          }
        }

        anyToSend = true;

        // set timestamps
        e.setSendTime(now);
        long deadline = MessageTimeoutUtils.getDeadline(target);
        if (deadline > 0 && graceTime > 0 && graceTime > deadline) {
          // extend deadline to match initial "grace" period
          deadline = graceTime;
        }
        e.setDeadline(deadline);

        // add to (target => map(name => sendObj))
        Map xaddrs = (xlookup ? lookupAddrs : modifyAddrs);
        if (nsize == 1) {
          // minor optimization for single-element map
          xaddrs.put(target, Collections.singletonMap(name, sendObj));
          break;
        }
        Map addrMap = (Map) xaddrs.get(target);
        if (addrMap == null) {
          addrMap = new HashMap();
          xaddrs.put(target, addrMap);
        }
        // assert (!addrMap.containsKey(name));
        addrMap.put(name, sendObj);
      }
    }

    return anyToSend;
  }

  private void ensureDeadlineTimer() {
    if (checkDeadlinesTime > 0) {
      return;
    }
    // schedule our next deadline check
    checkDeadlinesTime = now + config.checkDeadlinesPeriod;
    if (logger.isDetailEnabled()) {
      logger.detail(
          "will send messages, scheduling timer to check deadlines");
    }
    checkDeadlinesThread.schedule(config.checkDeadlinesPeriod);
  }

  private Map takeMessagesToSend(boolean lookup) {
    Map addrs = (lookup ? lookupAddrs : modifyAddrs);
    int n = addrs.size();
    if (n == 0) {
      return null;
    }
    if (n == 1) {
      Iterator iter = addrs.entrySet().iterator();
      Map.Entry me = (Map.Entry) iter.next();
      return Collections.singletonMap(me.getKey(), me.getValue());
    }
    return new HashMap(addrs);
  }

  private void cleanup() {
    now = 0;
    sendNow = false;
    sendLater = false;
    lookupNames.clear();
    modifyNames.clear();
    lookupAddrs.clear();
    modifyAddrs.clear();
  }

  private void sendAll(Map lookupsToSend, Map modifiesToSend) {
    // send messages
    //
    // send the modifications first, so a lookup that matches our
    // own modifications will see our modifications instead of
    // the pre-modification state.
    //
    // we send the lookups and modifies separately, even if they're
    // going to the same target.  We lose some of our batching, but
    // this simplfies the security message-content checks.
    long now = System.currentTimeMillis();
    sendAll(false, modifiesToSend, now);
    sendAll(true, lookupsToSend, now);
  }

  private void sendAll(boolean lookup, Map addrMap, long now) {
    stats(lookup).sendAll(addrMap);
    int n = (addrMap == null ? 0 : addrMap.size());
    if (n == 0) {
      return;
    }
    Iterator iter = addrMap.entrySet().iterator();
    for (int i = 0; i < n; i++) {
      Map.Entry me = (Map.Entry) iter.next();
      MessageAddress target = (MessageAddress) me.getKey();
      Map map = (Map) me.getValue();
      send(lookup, target, map, now);
    }
  }

  private void send(
      boolean lookup,
      MessageAddress target,
      Map map,
      long now) {
    if (target == NULL_ADDR) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "queuing message until WP servers are available: "+
            (lookup ? "lookup" : "modify")+" "+ map);
      }
    } else {
      WPQuery wpq = new WPQuery(
          agentId, target, now,
          (lookup ? WPQuery.LOOKUP : WPQuery.MODIFY),
          map);
      if (logger.isDetailEnabled()) {
        logger.detail("sending message: "+wpq);
      }
      messageSwitchService.sendMessage(wpq);
    }
  }
  
  private Object wrapQuery(
      boolean lookup,
      String name,
      Object query) {
    if (lookup || protectS == null) {
      return query;
    }
    // wrap sendObj using protection service
    String agent; 
    if (query instanceof NameTag) {
      agent = ((NameTag) query).getName();
    } else {
      agent = agentId.getAddress();
    }
    WhitePagesProtectionService.Wrapper wrapper;
    try {
      wrapper = protectS.wrap(agent, query);
      if (wrapper == null) {
        throw new RuntimeException("Wrap returned null");
      }
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error(
            "Unable to wrap (agent="+agent+" name="+name+
            " query="+query+")", e);
      }
      wrapper = null;
    }
    Object ret = new NameTag(agent, wrapper);
    if (logger.isDetailEnabled()) {
      logger.detail(
          "wrapped (agent="+agent+" name="+name+" query="+
          query+") to "+ret);
    }
    return ret;
  }

  //
  // receive:
  //

  private boolean receive(Message m) {
    if (m instanceof WPAnswer) {
      WPAnswer wpa = (WPAnswer) m;
      if (wpa.getAction() != WPAnswer.FORWARD) {
        receiveLater(wpa);
        return true;
      }
    }
    return false;
  }

  private void receiveLater(WPAnswer wpa) {
    // queue to run in our thread
    synchronized (receiveQueue) {
      receiveQueue.add(wpa);
    }
    receiveThread.start();
  }

  private void receiveNow() {
    synchronized (receiveQueue) {
      if (receiveQueue.isEmpty()) {
        if (logger.isDetailEnabled()) {
          logger.detail("input queue is empty");
        }
        return;
      }
      receiveTmp.addAll(receiveQueue);
      receiveQueue.clear();
    }
    // receive messages
    for (int i = 0, n = receiveTmp.size(); i < n; i++) {
      WPAnswer wpa = (WPAnswer) receiveTmp.get(i);
      receiveNow(wpa);
    }
    receiveTmp.clear();
  }

  private void receiveNow(WPAnswer wpa) {
    if (logger.isDetailEnabled()) {
      logger.detail("receiving message: "+wpa);
    }

    boolean lookup = (wpa.getAction() == WPQuery.LOOKUP);
    Map m = wpa.getMap();

    stats(lookup).receiveNow(m);

    int n = (m == null ? 0 : m.size());
    if (n == 0) {
      return;
    }

    MessageAddress addr = wpa.getOriginator();
    long sendTime = wpa.getSendTime();
    long replyTime = wpa.getReplyTime();
    boolean useServerTime = wpa.useServerTime();

    long now = System.currentTimeMillis();
    long rtt = (now - sendTime);

    Map answerMap = null;

    // remove from pending queue
    synchronized (sendLock) {
      Iterator iter = m.entrySet().iterator();
      for (int i = 0; i < n; i++) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Object answer = me.getValue();
        // tell a queue
        if (!shouldReceive(lookup, addr, name, answer, now)) {
          continue;
        }
        if (n == 1) {
          answerMap = m;
          continue;
        }
        // add to the per-name map
        if (answerMap == null) {
          answerMap = new HashMap();
        }
        answerMap.put(name, answer);
      }

      if (answerMap == null) {
        return;
      }

      // reward the server
      selectService.update(addr, rtt, false);
    }

    // compute the base time
    long baseTime;
    if (useServerTime) {
      // use the server's clock
      baseTime = replyTime;
    } else {
      // use a round-trip-time estimate as defined in WPAnswer
      baseTime = sendTime + (rtt >> 1);
    }

    stats(lookup).accept(answerMap);

    // tell our clients
    if (lookup) {
      List l = lookupClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        LookupService.Client c = (LookupService.Client) l.get(i);
        c.lookupAnswer(baseTime, answerMap);
      }
    } else {
      List l = modifyClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        ModifyService.Client c = (ModifyService.Client) l.get(i);
        c.modifyAnswer(baseTime, answerMap);
      }
    }
  }

  //
  // debug printer:
  //

  private Stats stats(boolean lookup) {
    return (lookup ? lookupStats : modifyStats);
  }

  private void debugQueues() {
    if (!logger.isDebugEnabled()) {
      return;
    }

    // stats
    logger.debug("header, agent, "+stats(true).getHeader());
    logger.debug("lookup, "+agentId+", "+stats(true).getStats());
    logger.debug("modify, "+agentId+", "+stats(false).getStats());

    synchronized (receiveQueue) {
      String s = "";
      s += "\n##### client transport input queue ########################";
      int n = receiveQueue.size();
      s += "\nreceive["+n+"]: ";
      for (int i = 0; i < n; i++) {
        WPAnswer wpa = (WPAnswer) receiveQueue.get(i);
        s += "\n   "+wpa;
      }
      s += "\n###########################################################";
      logger.debug(s);
    }

    String currentServers = selectService.toString();
    synchronized (sendLock) {
      String s = "";
      s += "\n##### client transport output queue #######################";
      s += "\nservers="+currentServers;
      s += "\nmessageSwitchService="+messageSwitchService;
      long now = System.currentTimeMillis();
      boolean firstPass = true;
      while (true) {
        Map m = (firstPass ? lookups : mods);
        int n = m.size();
        s += 
          "\n"+
          (firstPass ? "lookup" : "modify")+
          "["+n+"]: ";
        if (n > 0) { 
          for (Iterator iter = m.entrySet().iterator();
              iter.hasNext();
              ) {
            Map.Entry me = (Map.Entry) iter.next();
            String name = (String) me.getKey();
            Entry e = (Entry) me.getValue();
            s += "\n   "+name+"\t => "+e.toString(now);
          }
        }
        if (firstPass)  {
          firstPass = false;
        } else {
          break;
        }
      }
      s += "\n###########################################################";
      logger.debug(s);
    }
  }

  //
  // The following methods have a lot of knowledge that's specific to
  // the cache and lease managers, so perhaps it should be refactored
  // to the client APIs.
  //

  /**
   * Avoid nagle on first-time name listings, since these are
   * typically user-interface requests.
   */
  private boolean mustSendNow(
      boolean lookup,
      String name,
      Object query) {
    // we could do an expensive stack check for a UI thread
    return
      lookup &&
      name != null &&
      name.length() > 0 &&
      name.charAt(0) == '.' &&
      query == null;
  }

  /**
   * Figure out if we should send this request, either because it's
   * new or it supercedes the pending request. 
   */
  private boolean shouldSend(
      boolean lookup,
      String name,
      Object query,
      long now) {
    // assert (Thread.holdsLock(sendLock));

    Map table = (lookup ? lookups : mods);

    Entry e = (Entry) table.get(name);
    if (e == null) {
      return true;
    }

    Object sentObj = e.getQuery();
    Object sentO = sentObj;
    if (sentO instanceof NameTag) {
      sentO = ((NameTag) sentO).getObject();
    }
    Object q = query;
    if (q instanceof NameTag) {
      q = ((NameTag) q).getObject();
    }

    boolean create = false;
    if (sentO == null ? q == null : sentO.equals(q)) {
      // already sent?
    } else {
      if (lookup) {
        if (q == null) {
          // promote from uid-based validate to full-lookup
          create = true;
        } else if (q instanceof UID) {
          if (sentO == null) {
            // already sent a full-lookup
          } else {
            // possible bug in cache manager, which is supposed to
            // prevent unequal uid-based validate races.  The uids
            // may have different owners, so we can't figure out
            // the correct order by comparing uid counters.
            if (logger.isErrorEnabled()) {
              logger.error(
                "UID mismatch in WP uid-based lookup validation, "+
                "sentObj="+sentObj+", query="+query+", entry="+
                e.toString(now));
            }
          }
        } else {
          // invalid
        }
      } else {
        UID sentUID = 
          (sentO instanceof UID ? ((UID) sentO) :
           sentO instanceof Record ? ((Record) sentO).getUID() :
           null);
        UID qUID = 
          (q instanceof UID ? ((UID) q) :
           q instanceof Record ? ((Record) q).getUID() :
           null);
        if (sentUID != null &&
            qUID != null &&
            sentUID.getOwner().equals(qUID.getOwner())) {
          if (sentUID.getId() < qUID.getId()) {
            // send the query, since it has a more recent uid.
            // Usually q is a full-renew that should replace an
            // pending sentObj (either uid-renew or full-renew)
            // that's now stale.
            create = true;
          } else if (
              sentUID.getId() == qUID.getId() &&
              sentO instanceof UID &&
              q instanceof Record) {
            // promote from uid-renew to full-renew.  This is
            // necessary to handle a "lease-not-known" response
            // while a uid-renew is pending.
            create = true;
          } else {
            // ignore this query.  Usually the uids match, q is
            // a uid-renew, and we're still waiting for the
            // sentObj full-renew.  This also handles rare race
            // conditions, where the order of multi-threaded queries
            // passing through the lease manager is jumbled.
          }
        } else {
          // invalid
        }
      }
    }

    if (!create && logger.isDebugEnabled()) {
      logger.debug(
          "Not sending "+
          (lookup ? "lookup" : "modify")+
          " (name="+name+
          " query="+query+
          "), since we've already sent: "+
          (e == null ? "null" : e.toString(now)));
    }

    return create;
  }

  /**
   * Special test for local-node uid-based modify requests.
   */
  private boolean shortcutNodeModify(
      boolean lookup,
      String name,
      Entry e,
      long now) {
    // see if this is a uid-based modify for our own node
    if (lookup || !name.equals(agentId.getAddress())) {
      return false;
    }
    Object query = e.getQuery();
    Object q = query;
    if (q instanceof NameTag) {
      q = ((NameTag) q).getObject();
    }
    if (!(q instanceof UID)) {
      return false;
    }
    // this is a uid-based renewal of our node, but maybe the server
    // crashed and forgot the record data necessary to send back a
    // "lease-not-known" response!
    //
    // the ugly fix is to pretend that the server sent back a
    // lease-not-known response.  This will force the lease
    // manager to send a renewal that contains the full record.
    //
    // if we're correct then the server's queued lease-not-known
    // messages may stream back, which is wasteful but okay.
    UID uid = (UID) q;
    Object answer = new LeaseNotKnown(uid);
    Map m = Collections.singletonMap(name, answer);
    WPAnswer wpa = new WPAnswer(
        e.getTarget(),   // from the server
        agentId,         // back to us
        e.getSendTime(), // our sendTime
        now,             // the "server" sendTime
        true,            // use the above time
        WPAnswer.MODIFY, // modify
        m);              // the lease-not-known answer
    if (logger.isInfoEnabled()) {
      logger.info(
          "Timeout waiting for uid-based modify response"+
          " (uid="+uid+"), pretending that the server"+
          " sent back a lease-not-known response: "+
          wpa);
    }
    receiveLater(wpa);
    return true;
  }

  /**
   * Figure out if we should accept this request response, including
   * whether or not we sent it and any necessary ordering/version
   * tests.
   */
  private boolean shouldReceive(
      boolean lookup, 
      MessageAddress addr,
      String name,
      Object answer,
      long now) {
    // assert (Thread.holdsLock(sendLock));

    Map table = (lookup ? lookups : mods);

    boolean accepted = false;
    Entry e = (Entry) table.get(name);
    if (e == null) {
      // not sent?
    } else {
      Object sentObj = e.getQuery();
      // see if this matches what we sent
      if (lookup) {
        if (answer instanceof Record) {
          // we accept this, even if we sent a different UID,
          // since this is the latest value
          accepted = true;
        } else if (answer instanceof RecordIsValid) {
          if (sentObj == null) {
            // either we didn't send a uid-based lookup
            // or we just sent a full-record look request,
            // so ignore this.
          } else {
            UID uid = ((RecordIsValid) answer).getUID();
            if (uid.equals(sentObj)) {
              // okay, we sent this
              accepted = true;
            } else {
              // we sent a uid-based validation and
              // an ack for a different uid came back.  Our
              // uid-based message is still in flight, so either
              // we didn't send the lookup or this is a stale
              // message.
            }
          }
        } else {
          // invalid response
        }
      } else {
        UID uid;
        if (answer instanceof Lease) {
          uid = ((Lease) answer).getUID();
        } else if (answer instanceof LeaseNotKnown) {
          uid = ((LeaseNotKnown) answer).getUID();
        } else if (answer instanceof LeaseDenied) {
          uid = ((LeaseDenied) answer).getUID();
        } else {
          // invalid response
          uid = null;
        }
        UID sentUID;
        Object sentO = sentObj;
        if (sentO instanceof NameTag) {
          sentO = ((NameTag) sentO).getObject();
        }
        if (sentO instanceof UID) {
          sentUID = (UID) sentO;
        } else if (sentO instanceof Record) {
          sentUID = ((Record) sentO).getUID();
        } else {
          // we sent an invalid query?
          sentUID = null;
        }
        if (uid != null && uid.equals(sentUID)) {
          // okay, we sent this
          accepted = true;
        } else {
          // either we never sent this, or it's stale,
          // or the server is confused.  If we're mistaken
          // then our retry timer will resent the modify.
        }
      }
    }

    if (accepted) {
      // clear the table entry
      table.remove(name);
    }

    if (logger.isInfoEnabled()) {
      logger.info(
          (accepted ? "Accepting" : "Ignoring")+
          " "+
          (lookup ? "lookup" : "modify")+
          " response (name="+
          name+", answer="+answer+
          ") returned by "+addr+
          ", since it "+
          (accepted ? "matches" : "doesn't match")+
          " our sent query: "+
          (e == null ? "<null>" : e.toString(now)));
    }

    return accepted;
  }

  //
  // classes:
  //

  private static class Entry {

    private final Object query;

    private final long creationTime;

    private long sendTime;
    private long deadline;
    private MessageAddress target;

    public Entry(Object query, long now) {
      this.query = query;
      this.creationTime = now;
    }

    public Object getQuery() {
      return query;
    }

    public long getCreationTime() {
      return creationTime;
    }

    public long getSendTime() {
      return sendTime;
    }
    public void setSendTime(long sendTime) {
      this.sendTime = sendTime;
    }

    public long getDeadline() {
      return deadline;
    }
    public void setDeadline(long deadline) {
      this.deadline = deadline;
    }

    public MessageAddress getTarget() {
      return target;
    }
    public void setTarget(MessageAddress target) {
      this.target = target;
    }
    
    public String toString() {
      long now = System.currentTimeMillis();
      return toString(now);
    }

    public String toString(long now) {
      return 
        "(created="+Timestamp.toString(getCreationTime(), now)+
        " sent="+Timestamp.toString(getSendTime(), now)+
        " deadline="+Timestamp.toString(getDeadline(), now)+
        " target="+getTarget()+
        " query="+getQuery()+
        ")";
    }
  }

  private static class Stats {

    private final Object lock = new Object();

    private int count;
    private int size;
    private int later;
    private int sendCount;
    private int sendSize;
    private int retrySize;
    private int receiveCount;
    private int receiveSize;
    private int acceptCount;
    private int acceptSize;

    private String getHeader() {
      return
        "count"+
        ", size"+
        ", later"+
        ", sendC"+
        ", sendS"+
        ", retryS"+
        ", recvC"+
        ", recvS"+
        ", accC"+
        ", accS";
    }

    private String getStats() {
      synchronized (lock) {
        return
          count+
          ", "+size+
          ", "+later+
          ", "+sendCount+
          ", "+sendSize+
          ", "+retrySize+
          ", "+receiveCount+
          ", "+receiveSize+
          ", "+acceptCount+
          ", "+acceptSize;
      }
    }

    private void send(Map m) {
      int s = (m == null ? 0 : m.size());
      if (s <= 0) {
        return;
      }
      synchronized (lock) {
        count++;
        size += s;
      }
    }
    private void later() {
      synchronized (lock) {
        later++;
      }
    }
    private void sendAll(Map addrMap) {
      int n = (addrMap == null ? 0 : addrMap.size());
      if (n <= 0) {
        return;
      }
      synchronized (lock) {
        sendCount += n;
        int s = 0;
        Iterator iter = addrMap.entrySet().iterator();
        for (int i = 0; i < n; i++) {
          Map.Entry me = (Map.Entry) iter.next();
          Map m = (Map) me.getValue();
          s += m.size();
        }
        sendSize += s;
      }
    }
    private void retry() {
      synchronized (lock) {
        retrySize++;
      }
    }
    private void receiveNow(WPAnswer wpa) {
      synchronized (lock) {
        if (wpa == null) {
          return;
        }
        boolean lookup = (wpa.getAction() == WPQuery.LOOKUP);
        Map m = wpa.getMap();
        receiveNow(m);
      }
    }
    private void receiveNow(Map m) {
      synchronized (lock) {
        receiveCount++;
        int n = (m == null ? 0 : m.size());
        receiveSize += n;
      }
    }
    private void accept(Map answerMap) {
      synchronized (lock) {
        acceptCount++;
        int n = (answerMap == null ? 0 : answerMap.size());
        acceptSize += n;
      }
    }
  }

  private class LookupSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!LookupService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        if (!(requestor instanceof LookupService.Client)) {
          throw new IllegalArgumentException(
              "LookupService"+
              " requestor must implement "+
              "LookupService.Client");
        }
        LookupService.Client client = (LookupService.Client) requestor;
        LookupService lsi = new LookupServiceImpl(client);
        ClientTransport.this.register(client);
        return lsi;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
        if (!(service instanceof LookupServiceImpl)) {
          return;
        }
        LookupServiceImpl lsi = (LookupServiceImpl) service;
        LookupService.Client client = lsi.client;
        ClientTransport.this.unregister(client);
      }
      private class LookupServiceImpl 
        implements LookupService {
          private final Client client;
          public LookupServiceImpl(Client client) {
            this.client = client;
          }
          public void lookup(Map m) {
            ClientTransport.this.lookup(m);
          }
        }
    }

  private class ModifySP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!ModifyService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        if (!(requestor instanceof ModifyService.Client)) {
          throw new IllegalArgumentException(
              "ModifyService"+
              " requestor must implement "+
              "ModifyService.Client");
        }
        ModifyService.Client client = (ModifyService.Client) requestor;
        ModifyServiceImpl msi = new ModifyServiceImpl(client);
        ClientTransport.this.register(client);
        return msi;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
        if (!(service instanceof ModifyServiceImpl)) {
          return;
        }
        ModifyServiceImpl msi = (ModifyServiceImpl) service;
        ModifyService.Client client = msi.client;
        ClientTransport.this.unregister(client);
      }
      private class ModifyServiceImpl 
        implements ModifyService {
          private final Client client;
          public ModifyServiceImpl(Client client) {
            this.client = client;
          }
          public void modify(Map m) {
            ClientTransport.this.modify(m);
          }
        }
    }

  /** config options, soon to be parameters/props */
  private static class ClientTransportConfig {
    public final long nagleMillis;
    public final boolean noListNagle;
    public final long checkDeadlinesPeriod;
    public final long graceMillis;

    public ClientTransportConfig(Object o) {
      // FIXME parse!
      nagleMillis =  
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.resolver.transport.nagleMillis",
              "0"));
      noListNagle =
        Boolean.getBoolean(
            "org.cougaar.core.wp.resolver.transport.noListNagle");
      checkDeadlinesPeriod =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.resolver.transport.checkDeadlinesPeriod",
              "10000"));
      graceMillis =  
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.resolver.transport.graceMillis",
              "0"));
    }
  }
}

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

package org.cougaar.core.wp.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.core.agent.service.MessageSwitchService;
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
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.wp.MessageTimeoutUtils;
import org.cougaar.util.RarelyModifiedList;
import org.cougaar.core.wp.WhitePagesMessage;
import org.cougaar.core.wp.resolver.ConfigReader;
import org.cougaar.core.wp.resolver.WPAnswer;
import org.cougaar.core.wp.resolver.WPQuery;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component sends and receives messages for the server.
 * <p>
 * This component is responsible for the server-side hierarchy
 * traversal and replication.
 */
public class ServerTransport
extends GenericStateModelAdapter
implements Component
{

  /**
   * Should timestamps be relative to the server's clock or
   * the client's measured round-trip-time?
   *
   * @see WPAnswer
   */
  private final boolean USE_SERVER_TIME =
    Boolean.getBoolean(
        "org.cougaar.core.wp.server.useServerTime");

  // pick an action that doesn't conflict with WPQuery
  private static final int FORWARD_ANSWER = 3;

  private ServerTransportConfig config;

  private ServiceBroker sb;
  private LoggingService logger;
  private MessageAddress agentId;
  private ThreadService threadService;
  private WhitePagesService wps;

  private LookupAckSP lookupAckSP;
  private ModifyAckSP modifyAckSP;
  private ForwardAckSP forwardAckSP;
  private ForwardSP forwardSP;

  private RarelyModifiedList lookupAckClients = 
    new RarelyModifiedList();
  private RarelyModifiedList modifyAckClients = 
    new RarelyModifiedList();
  private RarelyModifiedList forwardAckClients = 
    new RarelyModifiedList();
  private RarelyModifiedList forwardClients = 
    new RarelyModifiedList();

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

  // messages queued until the messageSwitchService is available
  //
  // List<WhitePagesMessage> 
  private List sendQueue;

  private final Map peers = new HashMap();

  //
  // input (receive from WP server):
  //

  private Schedulable receiveThread;

  // received messages
  //
  // List<WhitePagesMessage>
  private final List receiveQueue = new ArrayList();

  // temporary list for use within "receiveNow()"
  //
  // List<Object>
  private final List receiveTmp = new ArrayList();

  //
  // debug queues:
  //

  private Schedulable debugThread;

  public void setParameter(Object o) {
    this.config = new ServerTransportConfig(o);
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

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  public void load() {
    super.load();

    if (logger.isDebugEnabled()) {
      logger.debug("Loading server remote handler");
    }

    // which agent are we in?
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);

    readConfig();

    // create threads
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
        "White pages server handle incoming responses");

    if (0 < config.debugQueuesPeriod &&
        logger.isDebugEnabled()) {
      Runnable debugRunner =
        new Runnable() {
          public void run() {
            // assert (thread == debugThread);
            debugQueues();
          }
        };
      debugThread = threadService.getThread(
          this,
          debugRunner,
          "White pages server handle outgoing requests");
      debugThread.start();
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

    // advertise our services
    lookupAckSP = new LookupAckSP();
    sb.addService(LookupAckService.class, lookupAckSP);
    modifyAckSP = new ModifyAckSP();
    sb.addService(ModifyAckService.class, modifyAckSP);
    forwardAckSP = new ForwardAckSP();
    sb.addService(ForwardAckService.class, forwardAckSP);
    forwardSP = new ForwardSP();
    sb.addService(ForwardService.class, forwardSP);
  }

  public void unload() {
    if (forwardSP != null) {
      sb.revokeService(ForwardService.class, forwardSP);
      forwardSP = null;
    }
    if (forwardAckSP != null) {
      sb.revokeService(ForwardAckService.class, forwardAckSP);
      forwardAckSP = null;
    }
    if (modifyAckSP != null) {
      sb.revokeService(ModifyAckService.class, modifyAckSP);
      modifyAckSP = null;
    }
    if (lookupAckSP != null) {
      sb.revokeService(LookupAckService.class, lookupAckSP);
      lookupAckSP = null;
    }

    if (messageSwitchService != null) {
      //messageSwitchService.removeMessageHandler(myMessageHandler);
      sb.releaseService(
          this, MessageSwitchService.class, messageSwitchService);
      messageSwitchService = null;
    }

    if (wps != null) {
      sb.releaseService(this, WhitePagesService.class, wps);
      wps = null;
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

    super.unload();
  }

  private void readConfig() {
    // for now we share the same "alpreg.ini" as the client
    //
    // we look for any entries where the name is "WP" or matches
    // ("WP-"+number).  We'll assume that those are potential
    // white pages alias entries, as either:
    //    already an "alias" entry
    //    an entry that will be bootstrapped (e.g. "-RMIREG")
    //
    // before we send messages to these servers we'll ask the
    // local white pages cache if it contains an entry for the
    // name.
    List l = ConfigReader.listEntries();
    Map initPeers = new HashMap();
    for (int i = 0, n = l.size(); i < n; i++) {
      AddressEntry ae = (AddressEntry) l.get(i);
      String alias = ae.getName();
      if (!alias.matches("WP(-\\d+)?")) {
        continue;
      }
      String agent = null;
      String type = ae.getType();
      if (type.equals("alias") ||
          type.equals("-RMI_REG")) { 
        agent = ae.getURI().getPath().substring(1);
        if ("*".equals(agent)) {
          agent = null;
        }
      }
      MessageAddress addr =
        (agent == null ?
         (null) :
         MessageAddress.getMessageAddress(agent));
      Peer p = new Peer(
          alias,
          agent,
          addr,
          (addr != null),
          ae);
      initPeers.put(alias, p);
    }
    synchronized (sendLock) {
      if (logger.isInfoEnabled()) {
        logger.info(
            "Found servers["+initPeers.size()+"]="+
            initPeers);
      }
      if (initPeers.isEmpty() && logger.isErrorEnabled()) {
        logger.error("Empty list of white pages servers found");
      }
      peers.putAll(initPeers);
    }
  }

  private void registerMessageSwitch() {
    // service broker now has the MessageSwitchService
    //
    // should we do this in a separate thread?
    if (messageSwitchService != null) {
      if (logger.isErrorEnabled()) {
        logger.error("Already obtained our message switch");
      }
      return;
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
      logger.info("Registered server message handler");
    }
    synchronized (sendLock) {
      this.messageSwitchService = mss;
      if (0 <= config.graceMillis) {
        this.graceTime = 
          System.currentTimeMillis() + config.graceMillis;
      }
      if (sendQueue != null) {
        // send queued messages
        //
        Runnable flushSendQueueRunner =
          new Runnable() {
            public void run() {
              synchronized (sendLock) {
                flushSendQueue();
              }
            }
          };
        Schedulable flushSendQueueThread = 
          threadService.getThread(
              this,
              flushSendQueueRunner,
              "White pages server flush queued output messages");
        flushSendQueueThread.start();
        // this may race with the normal message-send code,
        // so we also check the sendQueue there.  This means
        // that the above "flushSendQueue()" call may find a
        // null sendQueue by the time it is run.
      }
    }
  }

  private void register(LookupAckService.Client c) {
    lookupAckClients.add(c);
  }
  private void unregister(LookupAckService.Client c) {
    lookupAckClients.remove(c);
  }
  private void register(ModifyAckService.Client c) {
    modifyAckClients.add(c);
  }
  private void unregister(ModifyAckService.Client c) {
    modifyAckClients.remove(c);
  }
  private void register(ForwardAckService.Client c) {
    forwardAckClients.add(c);
  }
  private void unregister(ForwardAckService.Client c) {
    forwardAckClients.remove(c);
  }
  private void register(ForwardService.Client c) {
    forwardClients.add(c);
  }
  private void unregister(ForwardService.Client c) {
    forwardClients.remove(c);
  }

  private void lookupAnswer(
      MessageAddress clientAddr,
      long clientTime,
      Map m) {
    send(WPAnswer.LOOKUP, clientAddr, clientTime, m);
  }

  private void modifyAnswer(
      MessageAddress clientAddr,
      long clientTime,
      Map m) {
    send(WPAnswer.MODIFY, clientAddr, clientTime, m);
  }
  
  private void forwardAnswer(
      MessageAddress clientAddr,
      long clientTime,
      Map m) {
    // verify that our peers still include this target?
    if (peers.size() <= 1) {
      // only one server
      return;
    }
    send(WPAnswer.FORWARD, clientAddr, clientTime, m);
  }
  
  private void send(
      int action,
      MessageAddress clientAddr,
      long clientTime,
      Map m) {
    if (m == null || m.isEmpty()) {
      return;
    }

    long now = System.currentTimeMillis();

    long timeout =
      (action == WPAnswer.LOOKUP ?
       config.lookupTimeoutMillis :
       config.modifyTimeoutMillis);
    if (0 < timeout && 0 < graceTime) {
      long diff = graceTime - now;
      if (0 < diff && timeout < diff) {
        timeout = diff;
      }
    }
    long deadline = now + timeout;

    // tag with optional timeout attribute
    MessageAddress target = 
      MessageTimeoutUtils.setDeadline(
          clientAddr,
          deadline);

    WPAnswer wpa = new WPAnswer(
        agentId,
        target,
        clientTime,
        now,
        USE_SERVER_TIME,
        action,
        m);

    sendOrQueue(wpa);
  }

  private void forward(Map m, long ttd) {
    int n = peers.size();
    if (n <= 1) {
      // only one server, and we're it!
      return;
    }
    long now = System.currentTimeMillis();
    long ttl = now + ttd;
    Iterator iter = peers.entrySet().iterator();
    for (int i = 0; i < n; i++) {
      Map.Entry me = (Map.Entry) iter.next();
      String alias = (String) me.getKey();
      Peer p = (Peer) me.getValue();
      MessageAddress target;
      if (p.fixedAddr) {
        target = p.addr;
      } else {
        if (logger.isDetailEnabled()) {
          logger.detail("forward to non-fixed peer: "+p);
        }
        AddressEntry ae;
        try {
          ae = wps.get(alias, "alias", -1);
        } catch (Exception e) {
          // should never happens, since this is a cache-only request
          ae = null;
        }
        if (ae == null) {
          // alias not listed
          if (p.ae != null) {
            p.ae = null;
            p.name = null;
            p.addr = null;
          }
          if (logger.isDebugEnabled()) {
            logger.debug("Peer not found in local cache: "+p);
          }
          continue;
        }
        if (ae.equals(p.ae)) {
          // same as before
          target = p.addr;
        } else {
          // parse alias entry
          String path = ae.getURI().getPath();
          String name = path.substring(1);
          target = MessageAddress.getMessageAddress(name);
          target = MessageTimeoutUtils.setDeadline(target, ttl);
          p.addr = target;
          p.name = name;
          p.ae = ae;
        }
      }
      // exclude the local server
      if (agentId.equals(target.getPrimary())) {
        continue;
      }
      // send to this target
      WPQuery wpq = new WPQuery(
        agentId,
        target,
        now,
        WPQuery.FORWARD,
        m);
      sendOrQueue(wpq);
    }
  }
  
  private void forward(MessageAddress addr, Map m, long ttd) {
    // verify that our peers still include this target?
    if (peers.size() <= 1) {
      // only one server
      return;
    }
    // send to this target
    long now = System.currentTimeMillis();
    long deadline = now + ttd;
    MessageAddress target =
      MessageTimeoutUtils.setDeadline(addr, deadline);
    WPQuery wpq = new WPQuery(
        agentId,
        target,
        now,
        WPQuery.FORWARD,
        m);
    sendOrQueue(wpq);
  }

  private void sendOrQueue(WhitePagesMessage m) {
    synchronized (sendLock) {
      if (messageSwitchService == null) {
        // queue to send once the MTS is up
        if (sendQueue == null) {
          sendQueue = new ArrayList();
        }
        sendQueue.add(m);
        return;
      } else if (sendQueue != null) {
        // flush pending messages
        flushSendQueue();
      } else {
        // typical case
      }
      send(m);
    }
  }

  private void send(WhitePagesMessage m) {
    // assert (Thread.holdsLock(sendLock));
    // assert (messageSwitchService != null);
    if (logger.isDetailEnabled()) {
      logger.detail("sending message: "+m);
    }
    messageSwitchService.sendMessage(m);
  }

  private void flushSendQueue() {
    // assert (Thread.holdsLock(sendLock));
    // assert (messageSwitchService != null);
    List l = sendQueue;
    sendQueue = null;
    int n = (l == null ? 0 : l.size());
    if (n != 0) {
      // must drain in reverse order, since we appended
      // to the end.
      for (int i = n-1; 0 <= i; i--) {
        WhitePagesMessage m = (WhitePagesMessage) l.get(i);
        send(m);
      }
    }
  }
  
  private void receiveNow(WhitePagesMessage wpm) {
    if (logger.isDetailEnabled()) {
      logger.detail("receiving message: "+wpm);
    }

    MessageAddress clientAddr = wpm.getOriginator();

    Map m;
    long clientTime;
    int action;
    if (wpm instanceof WPQuery) {
      WPQuery wpq = (WPQuery) wpm;
      m = wpq.getMap();
      clientTime = wpq.getSendTime();
      action = wpq.getAction();
    } else {
      WPAnswer wpa = (WPAnswer) wpm;
      m = wpa.getMap();
      clientTime = wpa.getReplyTime();
      action = FORWARD_ANSWER;
    }

    int n = (m == null ? 0 : m.size());
    if (n == 0) {
      return;
    }

    // tell our clients (refactor me?)
    if (action == WPQuery.LOOKUP) {
      List l = lookupAckClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        LookupAckService.Client c = (LookupAckService.Client) l.get(i);
        c.lookup(clientAddr, clientTime, m);
      }
    } else if (action == WPQuery.MODIFY) {
      List l = modifyAckClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        ModifyAckService.Client c = (ModifyAckService.Client) l.get(i);
        c.modify(clientAddr, clientTime, m);
      }
    } else if (action == WPQuery.FORWARD) {
      List l = forwardAckClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        ForwardAckService.Client c = (ForwardAckService.Client) l.get(i);
        c.forward(clientAddr, clientTime, m);
      }
    } else if (action == FORWARD_ANSWER) {
      List l = forwardClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        ForwardService.Client c = (ForwardService.Client) l.get(i);
        c.forwardAnswer(clientAddr, clientTime, m);
      }
    } else if (logger.isErrorEnabled()) {
      logger.error("Unknown action "+action+" for message "+wpm);
    }
  }

  //
  // message receive queue
  //

  private boolean receive(Message m) {
    if (m instanceof WPQuery ||
        (m instanceof WPAnswer &&
         (((WPAnswer) m).getAction() == WPAnswer.FORWARD))) {
      WhitePagesMessage wpm = (WhitePagesMessage) m;
      receiveLater(wpm);
      return true;
    }
    return false;
  }

  private void receiveLater(WhitePagesMessage m) {
    // queue to run in our thread
    synchronized (receiveQueue) {
      receiveQueue.add(m);
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
      WhitePagesMessage m = (WhitePagesMessage) receiveTmp.get(i);
      receiveNow(m);
    }
    receiveTmp.clear();
  }

  private void debugQueues() {
    if (!logger.isDebugEnabled()) {
      return;
    }

    synchronized (receiveQueue) {
      String s = "";
      s += "\n##### server transport input queue ################";
      int n = receiveQueue.size();
      s += "\nreceive["+n+"]: ";
      for (int i = 0; i < n; i++) {
        WhitePagesMessage m = (WhitePagesMessage) receiveQueue.get(i);
        s += "\n   "+m;
      }
      s += "\n###################################################";
      logger.debug(s);
    }

    synchronized (sendLock) {
      String s = "";
      s += "\n##### server transport output queue ###############";
      s += "\nmessageSwitchService="+messageSwitchService;
      int n = (sendQueue == null ? 0 : sendQueue.size());
      s += "\nsendQueue["+n+"]: "+sendQueue;
      s += "\n###################################################";
      logger.debug(s);
    }

    // run me again later
    debugThread.schedule(config.debugQueuesPeriod);
  }

  private static class Peer {
    public final String alias;
    public String name;
    public MessageAddress addr;
    public final boolean fixedAddr;
    public AddressEntry ae;

    public Peer(
        String alias,
        String name,
        MessageAddress addr,
        boolean fixedAddr,
        AddressEntry ae) {
      this.alias = alias;
      this.name = name;
      this.addr = addr;
      this.fixedAddr = fixedAddr;
      this.ae = ae;
      if (alias == null) {
        throw new IllegalArgumentException("null alias");
      }
    }

    public String toString() {
      return 
        "(alias="+alias+
        " name="+name+
        " addr="+addr+
        " fixed="+fixedAddr+
        " ae="+ae+
        ")";
    }
  }

  private class LookupAckSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!LookupAckService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        if (!(requestor instanceof LookupAckService.Client)) {
          throw new IllegalArgumentException(
              "LookupAckService"+
              " requestor must implement "+
              "LookupAckService.Client");
        }
        LookupAckService.Client client = (LookupAckService.Client) requestor;
        LookupAckService si = new LookupAckServiceImpl(client);
        ServerTransport.this.register(client);
        return si;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
        if (!(service instanceof LookupAckServiceImpl)) {
          return;
        }
        LookupAckServiceImpl si = (LookupAckServiceImpl) service;
        LookupAckService.Client client = si.client;
        ServerTransport.this.unregister(client);
      }
      private class LookupAckServiceImpl 
        implements LookupAckService {
          private final Client client;
          public LookupAckServiceImpl(Client client) {
            this.client = client;
          }
          public void lookupAnswer(
              MessageAddress clientAddr,
              long clientTime,
              Map m) {
            ServerTransport.this.lookupAnswer(
                clientAddr,
                clientTime,
                m);
          }
        }
    }

  private class ModifyAckSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!ModifyAckService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        if (!(requestor instanceof ModifyAckService.Client)) {
          throw new IllegalArgumentException(
              "ModifyAckService"+
              " requestor must implement "+
              "ModifyAckService.Client");
        }
        ModifyAckService.Client client = (ModifyAckService.Client) requestor;
        ModifyAckServiceImpl si = new ModifyAckServiceImpl(client);
        ServerTransport.this.register(client);
        return si;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
        if (!(service instanceof ModifyAckServiceImpl)) {
          return;
        }
        ModifyAckServiceImpl si = (ModifyAckServiceImpl) service;
        ModifyAckService.Client client = si.client;
        ServerTransport.this.unregister(client);
      }
      private class ModifyAckServiceImpl 
        implements ModifyAckService {
          private final Client client;
          public ModifyAckServiceImpl(Client client) {
            this.client = client;
          }
          public void modifyAnswer(
              MessageAddress clientAddr,
              long clientTime,
              Map m) {
            ServerTransport.this.modifyAnswer(
                clientAddr,
                clientTime,
                m);
          }
        }
    }

  private class ForwardAckSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!ForwardAckService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        if (!(requestor instanceof ForwardAckService.Client)) {
          throw new IllegalArgumentException(
              "ForwardAckService"+
              " requestor must implement "+
              "ForwardAckService.Client");
        }
        ForwardAckService.Client client = (ForwardAckService.Client) requestor;
        ForwardAckService si = new ForwardAckServiceImpl(client);
        ServerTransport.this.register(client);
        return si;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
        if (!(service instanceof ForwardAckServiceImpl)) {
          return;
        }
        ForwardAckServiceImpl si = (ForwardAckServiceImpl) service;
        ForwardAckService.Client client = si.client;
        ServerTransport.this.unregister(client);
      }
      private class ForwardAckServiceImpl 
        implements ForwardAckService {
          private final Client client;
          public ForwardAckServiceImpl(Client client) {
            this.client = client;
          }
          public void forwardAnswer(
              MessageAddress clientAddr,
              long clientTime,
              Map m) {
            ServerTransport.this.forwardAnswer(
                clientAddr,
                clientTime,
                m);
          }
        }
    }

  private class ForwardSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!ForwardService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        if (!(requestor instanceof ForwardService.Client)) {
          throw new IllegalArgumentException(
              "ForwardService"+
              " requestor must implement "+
              "ForwardService.Client");
        }
        ForwardService.Client client = (ForwardService.Client) requestor;
        ForwardService si = new ForwardServiceImpl(client);
        ServerTransport.this.register(client);
        return si;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
        if (!(service instanceof ForwardServiceImpl)) {
          return;
        }
        ForwardServiceImpl si = (ForwardServiceImpl) service;
        ForwardService.Client client = si.client;
        ServerTransport.this.unregister(client);
      }
      private class ForwardServiceImpl 
        implements ForwardService {
          private final Client client;
          public ForwardServiceImpl(Client client) {
            this.client = client;
          }
          public void forward(Map m, long ttd) {
            ServerTransport.this.forward(m, ttd);
          }
          public void forward(MessageAddress target, Map m, long ttd) {
            ServerTransport.this.forward(target, m, ttd);
          }
        }
    }


  /** config options, soon to be parameters/props */
  private static class ServerTransportConfig {
    public static final long debugQueuesPeriod = 30*1000;

    public static final long graceMillis = 0;

    // this should match the maximum cache TTL
    public static final long lookupTimeoutMillis = 90*1000;

    // this should match the maximum lease TTL
    public static final long modifyTimeoutMillis = 90*1000;

    public ServerTransportConfig(Object o) {
      // FIXME parse!
    }
  }
}

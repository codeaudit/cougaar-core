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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.cougaar.core.wp.RarelyModifiedList;
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
        ClientTransport.this.onChange();
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

  private Schedulable checkSendQueuesThread;

  // lookups that have been sent, where the keys are the names
  // and the values are LookupEntry objects.
  //
  // this could be sorted by the LookupEntry sendTime, by
  // using a LinkedHashMap.  In practice we expect this map to
  // be small.
  //
  // Map<String, LookupEntry>
  private final Map lookups = new HashMap();

  // modify requests that have been sent, where the keys are the
  // names and the values are ModifyEntry objects.
  //
  // this could be sorted by the ModifyEntry sendTime, by
  // using a LinkedHashMap.  In practice we expect this map to
  // be small.
  //
  // Map<String, ModifyEntry>
  private final Map mods = new HashMap();

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

    Runnable checkSendQueuesRunner =
      new Runnable() {
        public void run() {
          // assert (thread == checkSendQueuesThread);
          checkSendQueues();
        }
      };
    checkSendQueuesThread = threadService.getThread(
        this,
        checkSendQueuesRunner,
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
      logger.info("Registered resolver message handler");
    }
    // should this be synchronized?
    synchronized (sendLock) {
      this.messageSwitchService = mss;
      if (0 <= config.graceMillis) {
        this.graceTime = 
          System.currentTimeMillis() + config.graceMillis;
      }
    }
    checkSendQueuesThread.start();
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

  private void onChange() {
    // the list of servers has changed
    //
    // kick the thread, since either we've added a new server
    // (important if we had zero servers) or we've removed a
    // server (must revisit any messages we sent to that server).
    checkSendQueuesThread.start();
  }

  private void lookup(Map m) {
    send(true, m);
  }

  private void modify(Map m) {
    send(false, m);
  }
  
  private void send(boolean lookup, Map m) {
    int n = (m == null ? 0 : m.size());
    if (n == 0) {
      return;
    }

    // if (n == 1) then we just keep a single addr,
    // otherwise we need to split the "m" map into
    // per-target maps
    MessageAddress singleAddr = null;
    Map singleMap = null;
    Map multiAddr = null;

    long now;

    synchronized (sendLock) {
      now = System.currentTimeMillis();
      boolean anyToSend = false;
      Iterator iter = m.entrySet().iterator();
      for (int i = 0; i < n; i++) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Object query = me.getValue();
        // select the target, add to queue
        if (!shouldSend(lookup, name, query, now)) {
          continue;
        }
        anyToSend = true;
        // accessing the "selectService" within our lock may be an
        // issue someday, but for now we'll assume it's allowed
        MessageAddress target = 
          (messageSwitchService == null ?
           (null) :
           selectService.select(lookup, name));
        Object sendObj = query;
        if (target == null) {
          target = NULL_ADDR;
        } else if (query != null) {
          sendObj = wrapQuery(lookup, name, query);
          if (sendObj == null) {
            continue;
          }
        }
        recordSend(lookup, target, name, query, sendObj, now);
        if (n == 1) {
          // minor optimization for single-element map
          singleAddr = target;
          if (sendObj == query) {
            singleMap = m;
          } else {
            singleMap = Collections.singletonMap(name, sendObj);
          }
          continue;
        }
        // add to the per-addr map
        if (multiAddr == null) {
          multiAddr = new HashMap();
        }
        Map nameMap = (Map) multiAddr.get(target);
        if (nameMap == null) {
          nameMap = new HashMap();
          multiAddr.put(target, nameMap);
        }
        // assert (!nameMap.containsKey(name));
        nameMap.put(name, sendObj);
      }

      if (!anyToSend) {
        return;
      }
    }

    if (n == 1) {
      send(lookup, singleAddr, singleMap, now);
    } else {
      sendAll(lookup, multiAddr, now);
    }

    // here we'd need to ensure that the checkSendQueues timer
    // is running if our maps were empty, but for now we never
    // stop that timer.
  }

  private void sendAll(
      boolean lookup,
      Map addrMap,
      long now) {
    if (addrMap == null ||
        addrMap.isEmpty()) {
      return;
    }
    for (Iterator iter = addrMap.entrySet().iterator();
        iter.hasNext();
        ) {
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
  
  private boolean shouldSend(
      boolean lookup,
      String name,
      Object query,
      long now) {
    // assert (Thread.holdsLock(sendLock));

    Map queue = (lookup ? lookups : mods);

    boolean create = false;
    Entry e = (Entry) queue.get(name);
    if (e == null) {
      create = true;
    } else {
      Object sentObj = e.getObject();
      if (sentObj == null ? query == null : sentObj.equals(query)) {
        // already sent?
      } else {
        if (lookup) {
          if (query == null) {
            // promote from validate to full-lookup
            create = true;
          } else if (query instanceof UID) {
            // already sent a full-lookup
          } else {
            // invalid
          }
        } else {
          Object q = query;
          if (q instanceof NameTag) {
            q = ((NameTag) q).getObject();
          }
          if (q instanceof UID) {
            // already sent a full-renew
          } else if (q instanceof Record) {
            // promote from uid-renew to full-renew
            create = true;
          } else {
            // invalid
          }
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

  private void recordSend(
      boolean lookup,
      MessageAddress target,
      String name,
      Object query,
      Object sendObj,
      long now) {
    // assert (Thread.holdsLock(sendLock));

    Map queue = (lookup ? lookups : mods);

    long deadline = MessageTimeoutUtils.getDeadline(target);
    if (0 < deadline && 0 < graceTime && deadline < graceTime) {
      // extend deadline
      deadline = graceTime;
    }

    Entry e = new Entry(now, deadline, target, query);

    queue.put(name, e);
  }

  private void checkSendQueues() {
    debugQueues();

    Map lookupAddrMap;
    Map modifyAddrMap;
    long now;
    synchronized (sendLock) {
      now = System.currentTimeMillis();
      // get modify retries
      modifyAddrMap = checkDeadline(false, now);
      // get lookup retries
      lookupAddrMap = checkDeadline(true, now);
    }

    // send messages
    //
    // send the modifications first, so a lookup that matches our
    // own modifications will see our modifications instead of
    // the pre-modification state.
    sendAll(false, modifyAddrMap, now);
    sendAll(true, lookupAddrMap, now);

    // run me again later
    //
    // note that this isn't necessary if the maps are empty,
    // but bug 3189 complicates the timer management.
    checkSendQueuesThread.schedule(config.checkSendQueuesPeriod);
  }

  private Map checkDeadline(
      boolean lookup,
      long now) {
    // assert (Thread.holdsLock(sendLock));
    Map queue = (lookup ? lookups : mods);
    int n = queue.size();
    if (n == 0) {
      return null;
    }
    Map addrMap = null;
    for (Iterator iter = queue.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      String name = (String) me.getKey();
      Entry e = (Entry) me.getValue();
      if (!shouldResend(lookup, name, e, now)) {
        continue;
      }
      // resend now
      MessageAddress oldTarget = e.getMessageAddress();
      selectService.update(
          oldTarget,
          (now - e.getSendTime()),
          true);
      MessageAddress target =
        (messageSwitchService == null ?
         (null) :
         selectService.select(lookup, name));
      Object query = e.getObject();
      Object sendObj = query;
      if (target == null) {
        target = NULL_ADDR;
      } else if (query != null) {
        sendObj = wrapQuery(lookup, name, query);
        if (sendObj == null) {
          iter.remove();
          continue;
        }
      }
      if (target.equals(oldTarget)) {
        // ugh, we just tried this!  oh well...
      }
      recordSend(lookup, target, name, query, sendObj, now);
      if (n == 1) {
        Map nameMap = Collections.singletonMap(name, sendObj);
        addrMap = Collections.singletonMap(target, nameMap);
        continue;
      }
      // add to the per-addr map
      if (addrMap == null) {
        addrMap = new HashMap();
      }
      Map nameMap = (Map) addrMap.get(target);
      if (nameMap == null) {
        nameMap = new HashMap();
        addrMap.put(target, nameMap);
      }
      nameMap.put(name, sendObj);
    }
    return addrMap;
  }

  private boolean shouldResend(
      boolean lookup,
      String name,
      Entry e,
      long now) {
    MessageAddress oldTarget = e.getMessageAddress();
    long deadline = e.getDeadline();
    if ((deadline <= 0 || now < deadline) &&
        selectService.contains(oldTarget)) {
      // give it more time for the ack
      return false;
    }
    // see if this is a uid-based modify for our own node
    if (lookup ||
        !name.equals(agentId.getAddress())) {
      return true;
    }
    Object query = e.getObject();
    Object q = query;
    if (q instanceof NameTag) {
      q = ((NameTag) q).getObject();
    }
    if (!(q instanceof UID)) {
      return true;
    }
    // this is a uid-based renewal, but maybe the server crashed
    // and forgot the record data necessary to send back a
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
        oldTarget,       // from the server
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
    return false;
  }

  private void receiveNow(WPAnswer wpa) {
    if (logger.isDetailEnabled()) {
      logger.detail("receiving message: "+wpa);
    }

    Map m = wpa.getMap();
    int n = (m == null ? 0 : m.size());
    if (n == 0) {
      return;
    }

    MessageAddress addr = wpa.getOriginator();
    long sendTime = wpa.getSendTime();
    long replyTime = wpa.getReplyTime();
    boolean useServerTime = wpa.useServerTime();
    boolean lookup = (wpa.getAction() == WPQuery.LOOKUP);

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
        boolean accepted =
          receive(lookup, addr, name, answer, now);
        if (!accepted) {
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

    // tell our clients
    if (lookup) {
      List l = lookupClients.getList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        LookupService.Client c = (LookupService.Client) l.get(i);
        c.lookupAnswer(baseTime, m);
      }
    } else {
      List l = modifyClients.getList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        ModifyService.Client c = (ModifyService.Client) l.get(i);
        c.modifyAnswer(baseTime, m);
      }
    }
  }

  private boolean receive(
      boolean lookup, 
      MessageAddress addr,
      String name,
      Object answer,
      long now) {
    // assert (Thread.holdsLock(sendLock));

    Map queue = (lookup ? lookups : mods);

    boolean accepted = false;
    Entry e = (Entry) queue.get(name);
    if (e == null) {
      // not sent?
    } else {
      Object sentObj = e.getObject();
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
      // clear the queue entry
      queue.remove(name);
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
  // message receive queue
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

  private void debugQueues() {
    if (!logger.isDebugEnabled()) {
      return;
    }

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
        if (0 < n) { 
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

  private static class Entry {

    private final long sendTime;
    private final long deadline;
    private final MessageAddress addr;
    private final Object obj;

    public Entry(
        long sendTime,
        long deadline,
        MessageAddress addr,
        Object obj) {
      this.sendTime = sendTime;
      this.deadline = deadline;
      this.addr = addr;
      this.obj = obj;
    }

    public long getSendTime() {
      return sendTime;
    }
    public long getDeadline() {
      return deadline;
    }
    public MessageAddress getMessageAddress() {
      return addr;
    }
    public Object getObject() {
      return obj;
    }
    
    public String toString() {
      long now = System.currentTimeMillis();
      return toString(now);
    }

    public String toString(long now) {
      return 
        "(sendTime="+Timestamp.toString(sendTime, now)+
        " deadline="+Timestamp.toString(deadline, now)+
        " addr="+addr+
        " obj="+obj+
        ")";
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
    public static final long checkSendQueuesPeriod = 10*1000;
    public static final long graceMillis = 0;

    public ClientTransportConfig(Object o) {
      // FIXME parse!
    }
  }
}

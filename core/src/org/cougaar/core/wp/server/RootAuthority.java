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

package org.cougaar.core.wp.server;

import java.util.ArrayList;
import java.util.Collection;
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
import org.cougaar.core.component.ServiceListener;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageHandler;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.wp.Scheduled;
import org.cougaar.core.wp.SchedulableWrapper;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.core.wp.WhitePagesMessage;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This is the single-point white pages server, which is the
 * authority for all naming zones.
 * <p>
 * This class will be split up and enhanced for Cougaar 10.6+.
 */
public class RootAuthority
extends GenericStateModelAdapter
implements Component
{
  // cache successes for 1.5 minutes:
  private static final String DEFAULT_SUCCESS_TTD =  "90000";
  // cache failures for  0.5 minutes:
  private static final String DEFAULT_FAIL_TTD    =  "30000";
  // leases expire after 4.0 minutes:
  private static final String DEFAULT_EXPIRE_TTD  = "240000";

  private ServiceBroker sb;

  private RootConfig config;

  private LoggingService logger;

  private AgentIdentificationService agentIdService;
  private MessageAddress agentId;

  private ThreadService threadService;
  private SchedulableWrapper expireThread;
  private SchedulableWrapper incomingThread;

  private MessageSwitchService messageSwitchService;
  private final MessageHandler myMessageHandler =
    new MessageHandler() {
      public boolean handleMessage(Message m) {
        return myHandleMessage(m);
      }
    };

  // received messages
  // List<WPQuery>
  private final List inQueue = new ArrayList();

  // temporary list for use within "handleQueues"
  // List<Object>
  private final List runTmp = new ArrayList();

  private final Object lock = new Object();

  // name -> (type -> lease)
  // Map<String, Map<String, Lease>>
  private final Map leases = new HashMap(13);

  // FIXME add "getAll" monitor LRU, to see who's asked about
  // our entries recently, and if the entry is rebound then
  // send an unsolicited "getAll" response (ala broadcast).
  //
  // Must be tunable!

  public void setParameter(Object o) {
    this.config = new RootConfig(o);
  }

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais != null) {
      this.agentId = ais.getMessageAddress();
    }
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  public void load() {
    super.load();

    if (logger.isDebugEnabled()) {
      logger.debug("Loading server root authority");
    }

    // configure
    AddressEntry[] tmp = config.entries;
    for (int i = 0; i < tmp.length; i++) {
      AddressEntry ae = tmp[i];
      long ttl = Long.MAX_VALUE; // FIXME forever?
      if (logger.isInfoEnabled()) {
        logger.info("init["+i+" / "+tmp.length+"]: "+ae);
      }
      bootstrap(ae, ttl);
    }

    // create expiration timer
    Scheduled expireRunner =
      new Scheduled() {
        public void run(SchedulableWrapper thread) {
          expireLeases(thread);
        }
      };
    expireThread = SchedulableWrapper.getThread(
      threadService,
      expireRunner,
      "White pages server expiration checker");
    expireThread.schedule(config.checkExpirePeriod);

    // create incoming message handler thread
    Scheduled incomingRunner =
      new Scheduled() {
        public void run(SchedulableWrapper thread) {
          // assert (thread == incomingThread);
          handleIncomingQueries(thread);
        }
      };
    incomingThread = SchedulableWrapper.getThread(
      threadService,
      incomingRunner,
      "White pages server incoming message handler");
    // schedule when messages arrive

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
    messageSwitchService = (MessageSwitchService)
      sb.getService(this, MessageSwitchService.class, null);
    if (messageSwitchService == null) {
      if (logger.isErrorEnabled()) {
        logger.error("Unable to obtain MessageSwitchService");
      }
      return;
    }
    messageSwitchService.addMessageHandler(myMessageHandler);
    if (logger.isInfoEnabled()) {
      logger.info("Registered root authority message handler");
    }
  }

  public void unload() {
    // release services
    if (messageSwitchService != null) {
      //messageSwitchService.removeMessageHandler(myMessageHandler);
      sb.releaseService(
          this, MessageSwitchService.class, messageSwitchService);
      messageSwitchService = null;
    }

    if (agentIdService != null) {
      sb.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }

    super.unload();
  }

  private boolean myHandleMessage(Message m) {
    if (!(m instanceof WhitePagesMessage)) {
      if (logger.isDetailEnabled()) {
        logger.detail("ignore: \t"+m);
      }
      return false;
    }
    WPQuery wpq = (WPQuery) m;

    // add to queue
    synchronized (inQueue) {
      inQueue.add(wpq);
    }
    incomingThread.start();

    return true;
  }

  private void handleIncomingQueries(SchedulableWrapper thread) {
    synchronized (inQueue) {
      if (!inQueue.isEmpty()) {
        runTmp.addAll(inQueue);
        inQueue.clear();
      }
    }
    if (!runTmp.isEmpty()) {
      // receive messages
      for (int i = 0, n = runTmp.size(); i < n; i++) {
        WPQuery wpq = (WPQuery) runTmp.get(i);
        receive(wpq);
      }
      runTmp.clear();
    }

    // run again when we receive messages
  }

  private void receive(WPQuery wpq) {
    if (logger.isDebugEnabled()) {
      logger.debug("Receive "+wpq);
    }
    WPAnswer ret;
    try {
      ret = submit(wpq);
    } catch (Throwable t) {
      if (logger.isErrorEnabled()) {
        logger.error(
            "Unable to process client query: "+wpq, t);
      }
      return;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Send    "+ret);
    }
    messageSwitchService.sendMessage(ret);
  }

  private WPAnswer submit(WPQuery wpq) {
    MessageAddress theSender = wpq.getOriginator();
    Request req = wpq.getRequest();
    Response res = req.createResponse();
    res = submit(res);
    if (res == null || !res.isAvailable()) {
      throw new RuntimeException(
          "Root can't answer: "+wpq);
    }
    Object ret = res.getResult();
    long ttl = res.getTTL();
    return new WPAnswer(
        agentId,   // from me
        theSender, // back to the sender
        req, ret, ttl);
  }

  // this does the real work
  private Response submit(Response res) {
    Request req = res.getRequest();
    Object ret = fetch(req);
    long now = System.currentTimeMillis();
    long ttl = 
      now +
      (ret == null ?
       config.failTTD :
       config.successTTD);
    res.setResult(ret, ttl);
    return res;
  }

  private Object fetch(Request req) {

    // FIXME within lock, if this is the first time
    // then verify that we're the root by looking in
    // the local WP for the ("WP", "alias") entry.

    Object ret;
    if (req instanceof Request.Get) {
      Request.Get rg = (Request.Get) req;
      String n = rg.getName();
      ret = getAll(n);
    } else if (req instanceof Request.GetAll) {
      String n = ((Request.GetAll) req).getName();
      ret = getAll(n);
    } else if (req instanceof Request.List) {
      String suffix = ((Request.List) req).getSuffix();
      ret = list(suffix);
    } else if (req instanceof Request.Bind) {
      Request.Bind rb = (Request.Bind) req;
      AddressEntry ae = rb.getAddressEntry();
      boolean overWrite = rb.isOverWrite();
      boolean renewal = rb.isRenewal();
      ret = bind(ae, overWrite, renewal);
    } else if (req instanceof Request.Unbind) {
      Request.Unbind ru = (Request.Unbind) req;
      AddressEntry ae = ru.getAddressEntry();
      ret = unbind(ae);
    } else {
      ret = "fail"; // FIXME must be non-null
    }
    return ret;
  }

  private void bootstrap(
      AddressEntry ae, long ttl) {
    bind(ae, true, false);
  }

  private Object getAll(String n) {
    Map ret;
    synchronized (lock) {
      Map m = (Map) leases.get(n);
      if (m == null) {
        ret = null;
      } else {
        // build map of entries & remove expired entries
        ret = new HashMap(m.size());
        long now = System.currentTimeMillis();
        for (Iterator iter = m.entrySet().iterator();
            iter.hasNext();
            ) {
          Map.Entry me = (Map.Entry) iter.next();
          Lease l = (Lease) me.getValue();
          if (l.expireTime < now) {
            if (logger.isInfoEnabled()) {
              logger.info("Expire lease "+l);
            }
            iter.remove();
          } else {
            ret.put(l.ae.getType(), l.ae);
            // record min-expire for response ttl?
          }
        }
        if (m.isEmpty()) {
          if (logger.isDetailEnabled()) {
            logger.detail("Expire all leases for "+n);
          }
          leases.remove(n);
          m = null;
          ret = null;
        } else {
          // make unmodifiable
          ret = Collections.unmodifiableMap(ret);
        }
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Returning getAll("+n+")="+ret);
    }
    return ret;
  }

  // FIXME a better layout of the "leases" would simplify this
  // and make it faster...
  private static Set filterNames(Set names, String suffix) {
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
    Set ret = new HashSet(13);
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

  private Object list(String suffix) {
    Set ret;
    synchronized (lock) {
      ret = filterNames(leases.keySet(), suffix);
    }
    return ret;
  }

  private Object bind(
      AddressEntry ae,
      boolean overWrite,
      boolean renewal) {
    String n = ae.getName();
    String type = ae.getType();
    long now = System.currentTimeMillis();
    long expireTime = now + config.expireTTD;
    synchronized (lock) {
      // find lease
      Lease lease;
      Map m = (Map) leases.get(n);
      if (m == null) {
        m = new HashMap(5);
        leases.put(n, m);
        lease = null;
      } else {
        lease = (Lease) m.get(type);
        if (lease != null && lease.expireTime < now) {
          // expired
          if (logger.isInfoEnabled()) {
            logger.info("Expire lease="+lease);
          }
          m.remove(type);
          lease = null;
        }
      }
      if (lease == null) {
        // append
        lease = new Lease(ae, expireTime);
        if (logger.isInfoEnabled()) {
          logger.info("Bound new lease="+lease);
        }
        m.put(type, lease);
      } else {
        // found a match
        if (overWrite) {
          // replace
          if (logger.isInfoEnabled()) {
            logger.info(
                "Bound overwrite oldLease="+lease+
                " newLease="+
                "(lease"+
                " entry="+ae+
                " expireTime="+
                Timestamp.toString(expireTime,now)+
                ")");
          }
          lease.ae = ae;
          lease.expireTime = expireTime;
        } else if (renewal) {
          // must match
          if (ae.equals(lease.ae)) {
            // okay, renew lease
            long oldExpireTime = lease.expireTime;
            lease.expireTime = expireTime;
            if (logger.isInfoEnabled()) {
              logger.info(
                  "Renewed lease oldExpireTime="+
                  Timestamp.toString(oldExpireTime,now)+
                  " newExpireTime="+
                  Timestamp.toString(expireTime,now)+
                  " lease="+lease);
            }
          } else {
            // somebody overwrote it!
            if (logger.isInfoEnabled()) {
              logger.info(
                  "Failing non-matching lease renewal for entry "+
                  ae+", the active lease is "+lease);
            }
            expireTime = -1;
          }
        } else {
          // already bound failure
          //
          // FIXME the failure response will be sent to the
          // named agent, but this will be the WP's bound client
          // instead of the requestor!
          //
          // requires mts to support uri-addresses
          if (logger.isInfoEnabled()) {
            logger.info(
                "Fail lease renewal for "+ae+
                ", the active lease is "+lease);
          }
          expireTime = -1;
        }
      }
    }
    if (expireTime < 0) {
      return Boolean.FALSE;
    }
    return new Long(expireTime);
  }

  private Object unbind(
      AddressEntry ae) {
    String n = ae.getName();
    synchronized (lock) {
      Map m = (Map) leases.get(n);
      if (m != null) {
        String type = ae.getType();
        Lease lease = (Lease) m.remove(type);
        if (lease != null) {
          // remove found entry
          if (logger.isInfoEnabled()) {
            logger.info(
                "Unbinding entry "+ae+" in lease="+lease);
          }
          // make sure that the entries match?
          long now = System.currentTimeMillis();
          if (lease.expireTime < now && 
              !ae.equals(lease.ae)) {
            if (logger.isWarnEnabled()) {
              logger.warn(
                  "Unbinding white pages entry "+ae+
                  " where the lease matches a different"+
                  " entry "+lease.ae+", expires at "+
                  Timestamp.toString(lease.expireTime,now));
            }
            // put the lease back in?
          }
          if (m.isEmpty()) {
            if (logger.isDetailEnabled()) {
              logger.detail("Unbinding all leases for "+n);
            }
            leases.remove(n);
          }
        }
      }
    }
    return Boolean.TRUE;
  }

  protected void expireLeases(SchedulableWrapper thread) {
    // assert (thread == expireThread);
    expireLeases();
    // run me again later
    thread.schedule(config.checkExpirePeriod);
  }

  private void expireLeases() {
    synchronized (lock) {
      long now = System.currentTimeMillis();
      if (logger.isInfoEnabled()) {
        logger.info(
            "Checking lease expiration now="+now+
            " leases["+leases.size()+"]");
      }
      for (Iterator nameIter = leases.values().iterator();
          nameIter.hasNext();
          ) {
        Map m = (Map) nameIter.next();
        for (Iterator typeIter = m.values().iterator();
            typeIter.hasNext();
            ) {
          Lease lease = (Lease) typeIter.next();
          boolean expireNow = (lease.expireTime < now);
          if (expireNow) {
            if (logger.isInfoEnabled()) {
              logger.info("Expire lease "+lease);
            }
            typeIter.remove();
          } else {
            if (logger.isDetailEnabled()) {
              logger.detail("Keep lease "+lease);
            }
          }
        }
        if (m.isEmpty()) {
          nameIter.remove();
        }
      }
    }
  }

  /** config options, soon to be parameters/props */
  private static class RootConfig {
    public final AddressEntry[] entries;
    public final long successTTD;
    public final long failTTD;
    public final long expireTTD;
    public final long checkExpirePeriod = 30*1000;
    public RootConfig(Object o) {
      entries = new AddressEntry[0];
      // FIXME parse!
      successTTD =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.server.successTTD",
              DEFAULT_SUCCESS_TTD));
      failTTD =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.server.failTTD",
              DEFAULT_FAIL_TTD));
      expireTTD =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.server.expireTTD",
              DEFAULT_EXPIRE_TTD));
    }
  }

  private static class Lease {
    public AddressEntry ae;
    public long expireTime;
    public Lease(AddressEntry ae, long expireTime) {
      this.ae = ae;
      this.expireTime = expireTime;
    }
    public String toString() {
      return 
        "(lease"+
        " entry="+ae+
        " expireTime="+
        Timestamp.toString(expireTime)+
        ")";
    }
  }
}

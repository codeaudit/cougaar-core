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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceListener;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageHandler;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.wp.WhitePagesMessage;
import org.cougaar.core.wp.Scheduled;
import org.cougaar.core.wp.SchedulableWrapper;
import org.cougaar.core.wp.server.WPAnswer;
import org.cougaar.core.wp.server.WPQuery;

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
public class RemoteHandler
extends HandlerBase
{
  private RemoteHandlerConfig config;

  private WhitePagesService wps;

  //
  // input (receive from WP server):
  //

  private final Object inLock = new Object();

  private SchedulableWrapper inThread;

  // received messages
  // List<WPAnswer>
  private final List inQueue = new ArrayList();

  // temporary list for use within "checkInQueue()"
  // List<Object>
  private final List inRunTmp = new ArrayList();

  //
  // output (send to WP server):
  //

  private final Object outLock = new Object();

  private SchedulableWrapper outThread;

  private MessageAddress rootAddr;

  // waiting to send
  // List<Request>
  private final List outQueue = new ArrayList();

  // temporary list for use within "checkOutQueue()"
  // List<Object>
  private final List outRunTmp = new ArrayList();

  private MessageSwitchService messageSwitchService;


  //
  // debug queues:
  //

  private SchedulableWrapper debugThread;


  public void setParameter(Object o) {
    this.config = new RemoteHandlerConfig(o);
  }

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  public void load() {
    super.load();

    if (logger.isDebugEnabled()) {
      logger.debug("Loading resolver remote handler");
    }

    // create threads
    Scheduled inRunner =
      new Scheduled() {
        public void run(SchedulableWrapper thread) {
          // assert (thread == inThread);
          checkInQueue();
        }
      };
    inThread = SchedulableWrapper.getThread(
        threadService,
        inRunner,
        "White pages client handle incoming responses");

    Scheduled outRunner =
      new Scheduled() {
        public void run(SchedulableWrapper thread) {
          // assert (thread == outThread);
          checkOutQueue();
        }
      };
    outThread = SchedulableWrapper.getThread(
        threadService,
        outRunner,
        "White pages client handle outgoing requests");

    if (config.debugQueuesPeriod > 0 &&
        logger.isDebugEnabled()) {
      Scheduled debugRunner =
        new Scheduled() {
          public void run(SchedulableWrapper thread) {
            // assert (thread == debugThread);
            debugQueues(thread);
          }
        };
      debugThread = SchedulableWrapper.getThread(
          threadService,
          debugRunner,
          "White pages client handle outgoing requests");
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

    // send root lookup
    Callback myCallback = 
      new Callback() {
        public void execute(Response res) {
          Response.Get gres = (Response.Get) res;
          AddressEntry ae = gres.getAddressEntry();
          foundRoot(ae);
        }
      };
    wps.get("WP", "alias", myCallback);
  }

  private void foundRoot(AddressEntry ae) {
    String rootName = null;
    if (ae != null) {
      String path = ae.getURI().getPath();
      if (path != null && path.length() > 1) {
        rootName = path.substring(1);
      }
      if (logger.isDetailEnabled()) {
        logger.detail(
            "Extracted root name \""+rootName+
            "\" from entry "+ae);
      }
    }
    if (rootName == null) {
      if (logger.isErrorEnabled()) {
        logger.error(
            "Root WP resolver"+
            " (name=\"WP\", type=\"alias\")"+
            " not found!");
      }
      return;
    }
    MessageAddress addr = MessageAddress.getMessageAddress(rootName);
    if (logger.isInfoEnabled()) {
      logger.info("Located root wp server: "+addr);
    }
    synchronized (outLock) {
      rootAddr = addr;
    }
    outThread.start();
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
          return myHandleMessage(m);
        }
      };
    mss.addMessageHandler(myMessageHandler);
    if (logger.isInfoEnabled()) {
      logger.info("Registered resolver message handler");
    }
    synchronized (outLock) {
      this.messageSwitchService = mss;
    }
    outThread.start();
  }

  public void unload() {
    // release services
    if (messageSwitchService != null) {
      //messageSwitchService.removeMessageHandler(myMessageHandler);
      sb.releaseService(
          this, MessageSwitchService.class, messageSwitchService);
      messageSwitchService = null;
    }

    super.unload();
  }

  protected Response mySubmit(Response res) {
    Request req = res.getRequest();
    // queue to run in our thread
    enqueueOut(req);
    // we can't answer it now, but our message receive handler
    // will eventually set the result.
    return null;
  }

  private void enqueueOut(Request req) {
    // queue to run in our thread
    boolean b;
    synchronized (outLock) {
      outQueue.add(req);
      b = (rootAddr != null && messageSwitchService != null);
    }
    if (b) {
      outThread.start();
    }
  }

  private boolean myHandleMessage(Message m) {
    if (!(m instanceof WPAnswer)) {
      return false;
    }
    WPAnswer wpa = (WPAnswer) m;
    // queue to run in our thread
    synchronized (inLock) {
      inQueue.add(wpa);
    }
    inThread.start();
    return true;
  }
  
  private void checkInQueue() {
    synchronized (inLock) {
      if (inQueue.isEmpty()) {
        if (logger.isDetailEnabled()) {
          logger.detail("input queue is empty");
        }
        return;
      }
      inRunTmp.addAll(inQueue);
      inQueue.clear();
    }
    // receive messages
    for (int i = 0, n = inRunTmp.size(); i < n; i++) {
      WPAnswer wpa = (WPAnswer) inRunTmp.get(i);
      receive(wpa);
    }
    inRunTmp.clear();
  }

  private void checkOutQueue() {
    synchronized (outLock) {
      if (messageSwitchService == null) {
        if (logger.isDetailEnabled()) {
          logger.detail("waiting for message switch service");
        }
        return;
      }
      if (rootAddr == null) {
        if (logger.isDetailEnabled()) {
          logger.detail("waiting for root WP address");
        }
        return;
      }
      if (outQueue.isEmpty()) {
        if (logger.isDetailEnabled()) {
          logger.detail("output queue is empty");
        }
        return;
      }
      outRunTmp.addAll(outQueue);
      outQueue.clear();
    }

    for (int i = 0, n = outRunTmp.size(); i < n; i++) {
      Request req = (Request) outRunTmp.get(i);
      send(req);
    }
    outRunTmp.clear();
  }

  private void debugQueues(SchedulableWrapper thread) {
    // assert (thread == debugThread);
    if (logger.isDebugEnabled()) {
      debugQueues();
      // run me again later
      thread.schedule(config.debugQueuesPeriod);
    }
  }

  private void debugQueues() {
    synchronized (inLock) {
      String s = "";
      s += "\n##### debug remote input queue ############################";
      int n = inQueue.size();
      s += "\ninQueue["+n+"]: ";
      for (int i = 0; i < n; i++) {
        WPAnswer wpa = (WPAnswer) inQueue.get(i);
        s += "\n   "+wpa;
      }
      s += "\n###########################################################";
      logger.debug(s);
    }

    synchronized (outQueue) {
      String s = "";
      s += "\n##### debug remote output queue ###########################";
      s += "\nrootAddr="+rootAddr;
      s += "\nmessageSwitchService="+messageSwitchService;
      int n = outQueue.size();
      s += "\noutQueue["+n+"]: ";
      for (int i = 0; i < n; i++) {
        Request req = (Request) outQueue.get(i);
        s += "\n   "+req;
      }
      s += "\n###########################################################";
      logger.debug(s);
    }
  }

  private void receive(WPAnswer wpa) {
    if (logger.isDetailEnabled()) {
      logger.detail("receiving message: "+wpa);
    }
    // extract message contents
    long ttl = wpa.getTTL();
    Request req = wpa.getRequest();
    Object result = wpa.getResult();
    // pass to cache & batch
    hrs.execute(req, result, ttl);
    // we've handled it
  }

  protected void myExecute(Request req, Object result, long ttl) {
    // no-op
  }

  private void send(Request req) {
    // create the message
    WhitePagesMessage wpm = 
      new WPQuery(agentId, rootAddr, req);

    // send the message
    if (logger.isDetailEnabled()) {
      logger.detail("sending message: \t"+wpm);
    }
    messageSwitchService.sendMessage(wpm);
    // it's on its way...
  }

  /** config options, soon to be parameters/props */
  private static class RemoteHandlerConfig {
    public final long debugQueuesPeriod = 30*1000;
    public RemoteHandlerConfig(Object o) {
      // FIXME parse!
    }
  }
}

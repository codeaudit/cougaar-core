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
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.wp.WhitePagesMessage;
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
  private WhitePagesService wps;

  private MessageAddress rootAddr;

  // waiting to send
  // List<Request>
  private final List outQueue = new ArrayList();

  // received messages
  // List<WPAnswer>
  private final List inQueue = new ArrayList();

  private long lastLocate = 0;

  // temporary list for use within "checkQueues()"
  // List<Object>
  private final List runTmp = new ArrayList();

  private final MessageHandler myMessageHandler =
    new MessageHandler() {
      public boolean handleMessage(Message m) {
        return myHandleMessage(m);
      }
    };
  private MessageSwitchService messageSwitchService;

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  public void load() {
    super.load();

    if (logger.isDebugEnabled()) {
      logger.debug("Loading resolver remote handler");
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

    // schedule root lookup
    scheduleRestart(5000);
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
      logger.info("Registered resolver message handler");
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
    synchronized (outQueue) {
      outQueue.add(req);
    }
    restart();
  }

  private boolean myHandleMessage(Message m) {
    if (!(m instanceof WPAnswer)) {
      return false;
    }
    WPAnswer wpa = (WPAnswer) m;
    // queue to run in our thread
    synchronized (inQueue) {
      inQueue.add(wpa);
    }
    restart();
    return true;
  }
  
  // running in our thread
  protected void myRun() {
    checkQueues();
    // we'll run again when the in/out queues are filled
  }

  private void checkQueues() {
    // empty the inQueue
    synchronized (inQueue) {
      if (!inQueue.isEmpty()) {
        runTmp.addAll(inQueue);
        inQueue.clear();
      }
    }
    if (!runTmp.isEmpty()) {
      // receive messages
      for (int i = 0, n = runTmp.size(); i < n; i++) {
        WPAnswer wpa = (WPAnswer) runTmp.get(i);
        receive(wpa);
      }
      runTmp.clear();
    }

    // empty the outQueue
    if (messageSwitchService == null) {
      // reschedule for later...
      if (logger.isDetailEnabled()) {
        logger.detail("waiting for message switch service");
      }
    } else {
      synchronized (outQueue) {
        if (!outQueue.isEmpty()) {
          runTmp.addAll(outQueue);
          outQueue.clear();
        }
      }
    }
    if (!runTmp.isEmpty()) {
      List notSent = null;
      for (int i = 0, n = runTmp.size(); i < n; i++) {
        Request req = (Request) runTmp.get(i);
        if (!send(req)) {
          if (notSent == null) {
            notSent = new ArrayList(n-i);
          }
          notSent.add(req);
        }
      }
      runTmp.clear();

      if (notSent != null) {
        // couldn't send some/all of our requests!
        //
        // this should be rare in practice and only be caused by a
        // configuration or bootstrapping delays
        synchronized (outQueue) {
          outQueue.addAll(0, notSent);
        }
      }
    }
  }

  private void receive(WPAnswer wpa) {
    if (logger.isDetailEnabled()) {
      logger.detail("Receive answer "+wpa);
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

  private boolean send(Request req) {
    if (logger.isDetailEnabled()) {
      logger.detail("send wp request: "+req);
    }

    // select the target
    MessageAddress target = selectTarget(req);
    if (target == null) {
      // reschedule for later...
      return false;
    }

    // create the message
    WhitePagesMessage wpm = 
      new WPQuery(agentId, target, req);

    // send the message
    if (logger.isDetailEnabled()) {
      logger.detail("send: \t"+wpm);
    }
    messageSwitchService.sendMessage(wpm);
    // it's on its way...
    return true;
  }

  private MessageAddress selectTarget(Request req) {
    // make sure we've located the root server
    if (rootAddr != null) {
      return rootAddr;
    }
    long now = System.currentTimeMillis();
    if (lastLocate + 5000 > now) {
      // we just ran
      return null;
    }
    lastLocate = now;

    // find the root wp server
    rootAddr = locateRootServer();
    if (rootAddr == null) {
      // try again later
      scheduleRestart(5000);
      return null;
    }
    return rootAddr;
  }

  private MessageAddress locateRootServer() {
    // find "WP" -> ("alias" -> "name://<agent>")
    String rootName = null;
    AddressEntry rootAE;
    try {
      rootAE = wps.get("WP", "alias", -1);
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error("Failed \"WP\" lookup", e);
      }
      rootAE = null;
    }
    if (rootAE != null) {
      rootName = rootAE.getURI().getHost();
      if (logger.isDetailEnabled()) {
        logger.detail(
            "Extracted root name \""+rootName+
            "\" from entry "+rootAE);
      }
    }
    if (rootName == null) {
      if (logger.isInfoEnabled()) {
        logger.info(
            "Root WP resolver"+
            " (name=\"WP\", type=\"alias\")"+
            " not found, will try again later");
      }
      return null;
    }
    MessageAddress addr = MessageAddress.getMessageAddress(rootName);
    if (logger.isInfoEnabled()) {
      logger.info("Located root wp server: "+addr);
    }
    // let the mts complain if the specific
    //   wp.get(rootName, "-RMI")
    // is not in the bootstrap config.
    return addr;
  }
}

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

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.wp.SchedulableWrapper;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This is a base class for most resolver handlers.
 */
public abstract class HandlerBase
extends GenericStateModelAdapter
implements Component
{
  protected ServiceBroker sb;

  protected LoggingService logger;
  protected MessageAddress agentId;
  private AgentIdentificationService agentIdService;
  protected ThreadService threadService;
  private SchedulableWrapper thread;
  protected HandlerRegistryService hrs;

  private final Handler myHandler = 
    new Handler() {
      public Response submit(Response res) {
        return HandlerBase.this.mySubmit(res);
      }
      public void execute(Request req, Object result, long ttl) {
        HandlerBase.this.myExecute(req, result, ttl);
      }
    };

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
    if (logger.isDebugEnabled()) {
      logger.debug("Loading handler");
    }

    Runnable myRunner =
      new Runnable() {
        public void run() {
          myRun();
        }
      };
    thread = SchedulableWrapper.getThread(
      threadService,
      myRunner,
      "White pages resolver "+getClass().getName());

    super.load();

    // register our handler
    hrs = (HandlerRegistryService)
      sb.getService(
          this, HandlerRegistryService.class, null);
    if (hrs == null) {
      throw new RuntimeException(
          "Unable to obtain HandlerRegistryService");
    }
    hrs.register(myHandler);
  }

  public void unload() {
    super.unload();

    // release services
    if (hrs != null) {
      hrs.unregister(myHandler);
      sb.releaseService(
          this, HandlerRegistryService.class, hrs);
    }

    // halt our thread
    if (thread != null) {
      thread.cancel();
      thread = null;
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
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }
  }

  /** Schedule to run in the future */
  protected final void scheduleRestart(long delay) {
    thread.schedule(delay);
  }

  /** Schedule to run again as soon as possible. */
  protected final void restart() {
    thread.start();
  }

  /** Handle a client-side (outgoing) request */
  protected abstract Response mySubmit(Response res);

  /** Handle a server-side (incoming) result */
  protected abstract void myExecute(
      Request req, Object result, long ttl);

  /** Run in a separate "restart()" thread */
  protected void myRun() {
  }
}

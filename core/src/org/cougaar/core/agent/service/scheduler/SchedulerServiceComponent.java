/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

package org.cougaar.core.agent.service.scheduler;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The SchedulerServiceComponent is a provider class for the Scheduler 
 * service within an agent.
 * <p>
 * This is typically just a wrapper for the ThreadService.
 */
public final class SchedulerServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private LoggingService loggingS;
  private ThreadService threadS;
  private SchedulerServiceProvider schedSP;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    // get the logger
    loggingS = (LoggingService)
      sb.getService(this, LoggingService.class, null);

    // get the thread service
    threadS = (ThreadService)
      sb.getService(this, ThreadService.class, null);

    // get the local agent address
    String agentName = "Anonymous";
    AgentIdentificationService agentIdS = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (agentIdS != null) {
      MessageAddress agentAddr = agentIdS.getMessageAddress();
      if (agentAddr != null) {
        agentName = agentAddr.getAddress();
      }
      sb.releaseService(this, AgentIdentificationService.class, agentIdS);
    }

    // create and advertise our service
    this.schedSP = new SchedulerServiceProvider(threadS, loggingS, agentName);
    sb.addService(SchedulerService.class, schedSP);
  }

  public void suspend() {
    schedSP.suspend();
    super.suspend();
  }

  public void resume() {
    super.resume();
    schedSP.resume();
  }

  public void unload() {
    // revoke our service
    if (schedSP != null) {
      sb.revokeService(SchedulerService.class, schedSP);
      schedSP = null;
    }
    // release services
    if (threadS != null) {
      sb.releaseService(this, ThreadService.class, threadS);
      threadS = null;
    }
    if (loggingS != null) {
      sb.releaseService(this, LoggingService.class, loggingS);
      loggingS = null;
    }
    super.unload();
  }
}

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

package org.cougaar.core.agent;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is loaded early in the agent, to announce
 * the agent Loading/Starting/<i>etc</i> state transitions.
 * @see EndLogger 
 */
public final class BeginLogger 
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private LoggingService log;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    // get our local agent's address
    MessageAddress localAgent = null;
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    // get logging service
    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);

    // prefix with agent name
    String prefix = localAgent+": ";
    log = LoggingServiceWithPrefix.add(log, prefix);

    if (log.isInfoEnabled()) {
      log.info("Loading");
    }
  }
  public void start() {
    super.start();
    if (log.isInfoEnabled()) {
      log.info("Starting");
    }
  }
  public void resume() {
    super.resume();
    if (log.isInfoEnabled()) {
      log.info("Resuming");
    }
  }

  // the component model is "first loaded is last unload", so these
  // are the end-states (i.e. the "*ed" instead of "*ing").

  public void suspend() {
    super.suspend();
    if (log.isInfoEnabled()) {
      log.info("Suspended");
    }
  }
  public void stop() {
    super.stop();
    if (log.isInfoEnabled()) {
      log.info("Stopped");
    }
  }
  public void unload() {
    super.unload();
    if (log.isInfoEnabled()) {
      log.info("Unloaded");
    }
  }
}

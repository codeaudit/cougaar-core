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

package org.cougaar.core.mobility.arch;

import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModel;

/**
 * Received a NACK response fromthe destination node.
 */
public class NackHandler extends AbstractHandler {

  private GenericStateModel model;
  private Throwable throwable;

  public NackHandler(
      MobilitySupport support,
      GenericStateModel model,
      Throwable throwable) {
    super(support);
    this.model = model;
    this.throwable = throwable;
  }

  public void run() {
    nack();
  }

  private void nack() {

    // FIXME race condition between move & agent-add!

    if (log.isInfoEnabled()) {
      log.info(
          "Handling failed move of agent "+id+
          " from "+nodeId+
          " to node "+moveTicket.getDestinationNode());
    }

    // agent is suspended -- let's resume it.

    try {
      resumeAgent();
    } catch (Exception e) {
      // did we lose an agent?!
      // should we kill it and reclaim the memory?
      if (log.isErrorEnabled()) {
        log.error("Unable to resume agent "+id, e);
      }
      return;
    }

    try {
      onFailure(throwable);
    } catch (Exception e) {
      // too late now -- the dispatch failed.
      if (log.isErrorEnabled()) {
        log.error(
            "Notification for \"onFailure\" of agent "+
            id+" failed (ignored)", e);
      }
      return;
    }

    if (log.isInfoEnabled()) {
      log.info("Completed failed move of agent "+id);
    }

  }

  private void resumeAgent() {
    if (log.isInfoEnabled()) {
      log.info("Resume  agent "+id);
    }
    model.resume();
    if (log.isInfoEnabled()) {
      log.info("Resumed agent "+id);
    }
  }

  public String toString() {
    return "Move (nack) of agent "+id;
  }
}

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

import org.cougaar.util.GenericStateModel;

/**
 */
public class AckHandler extends AbstractHandler {

  private GenericStateModel model;

  public AckHandler(
      MobilitySupport support,
      GenericStateModel model) {
    super(support);
    this.model = model;
  }

  public void run() {
    ack();
  }

  /**
   * Received an ACK response from the destination node.
   */
  private void ack() {

    // FIXME race condition between move & agent-add!

    // FIXME do handshake

    if (log.isInfoEnabled()) {
      log.info(
          "Received acknowledgement from node "+
          moveTicket.getDestinationNode()+
          " for move of agent "+id);
    }

    try {

      stopAgent();
      unloadAgent();
      removeAgent();

    } catch (Exception e) {

      if (log.isErrorEnabled()) {
        log.error("Agent removal after move failed", e);
      }

    }

    // fill in success
    //
    // note that this travels from the origin node 
    // to the just-moved agent on the remote destination 
    // node.

    onArrival();

    // agent will be GC'ed now

    if (log.isInfoEnabled()) {
      log.info(
          "Agent "+id+" has successfully moved to node "+
          moveTicket.getDestinationNode()+
          " and has been removed from node "+nodeId);
    }

  }

  private void stopAgent() {
    if (log.isInfoEnabled()) {
      log.info("Stop     agent "+id);
    }
    model.stop();
    if (log.isInfoEnabled()) {
      log.info("Stopped  agent "+id);
    }
  }

  private void unloadAgent() {
    if (log.isInfoEnabled()) {
      log.info("Unload   agent "+id);
    }
    model.unload();
    if (log.isInfoEnabled()) {
      log.info("Unloaded agent "+id);
    }
  }

  protected void removeAgent() {
    if (log.isInfoEnabled()) {
      log.info("Remove   agent "+id);
    }
    super.removeAgent();
    if (log.isInfoEnabled()) {
      log.info("Removed  agent "+id);
    }
  }

  public String toString() {
    return "Move (ack) of agent "+id;
  }
}

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

import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;

/**
 * Received a TRANSFER request on the destination node.
 */
public class ArrivalHandler extends AbstractHandler {

  private StateTuple tuple;

  public ArrivalHandler(
      MobilitySupport support,
      StateTuple tuple) {
    super(support);
    this.tuple = tuple;
  }

  public void run() {
    arrival();
  }

  private void arrival() {

    // FIXME race condition between move & agent-add!

    // FIXME do handshake

    if (log.isInfoEnabled()) {
      log.info(
          "Received request to add agent "+id+
          ", which is moving from node "+
          moveTicket.getOriginNode()+
          " to local node "+nodeId);
    }

    try {

      addAgent(tuple);

    } catch (Exception e) {

      if (log.isErrorEnabled()) {
        log.error(
            "Unable to add moved agent "+id+" to node "+nodeId, 
            e);
      }

      sendNack(e);

      return;
    }

    try {

      sendAck();

    } catch (Exception e) {

      // too late now!

      if (log.isErrorEnabled()) {
        log.error(
            "Unable to send acknowledgement for"+
            " move of agent "+id+" to node "+nodeId, e);
      }

    }

    if (log.isInfoEnabled()) {
      log.info(
          "Sent acknowledgement back to node "+
          moveTicket.getOriginNode()+
          ": agent "+id+" has successfully moved to node "+
          nodeId);
    }

  }

  protected void addAgent(StateTuple tuple) {
    if (log.isInfoEnabled()) {
      log.info("Add   agent "+id);
    }
    super.addAgent(tuple);
    if (log.isInfoEnabled()) {
      log.info("Added agent "+id);
    }
  }

  public String toString() {
    return "Move (arrival) of agent "+id;
  }
}

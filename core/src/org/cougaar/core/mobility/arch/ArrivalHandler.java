/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
import org.cougaar.core.mobility.MobilityException;
import org.cougaar.core.mts.MessageAddress;

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
      log.info("Agent "+id+" arrival from "+sender);
    }

    boolean didAdd = false;

    try {

      checkTicket();

      addAgent(tuple);

      didAdd = true;

      onArrival();

    } catch (Exception e) {

      if (log.isErrorEnabled()) {
        log.error(
            "Notification for \"onArrival\" of agent "+
            id+" failed", e);
      }
      if (didAdd) {
        removeAgent();
        onRemoval();
      }
      sendNack(e);
      return;
    }

    if (log.isInfoEnabled()) {
      log.info("Agent "+id+" added");
    }

    try {

      sendAck();

    } catch (Exception e) {

      // too late now!

      if (log.isErrorEnabled()) {
        log.error("Agent "+id+" ack failed!", e);
      }

    }

    if (log.isInfoEnabled()) {
      log.info("Agent "+id+" ACK sent");
    }

  }

  private void checkTicket() {
    if (log.isDebugEnabled()) {
      log.debug(
          "Check arrival ticket on node "+
          nodeId+" of ticket "+ticket);
    }

    if (sender == null) {
      String msg =
        "Received agent from null sender, ticket is "+
        ticket;
      throw new MobilityException(msg);
    }

    MessageAddress originNode = ticket.getOriginNode();
    if ((originNode != null) &&
        (!(originNode.equals(sender)))) {
      String msg = 
        "Received agent from sender "+sender+
        " that doesn't match origin "+originNode+
        ", ticket is "+
        ticket;
      throw new MobilityException(msg);
    }

    MessageAddress destNode = ticket.getDestinationNode();
    if ((destNode != null) &&
        (!(destNode.equals(nodeId)))) {
      String msg =
        "Received agent at node "+nodeId+
        " that doesn't match the ticket destination "+
        destNode+", ticket is "+ticket;
      throw new MobilityException(msg);
    }
  }

  public String toString() {
    return "Move (arrival) of agent "+id;
  }
}

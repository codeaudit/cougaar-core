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

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.mobility.MobilityException;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mobility.MoveTicket;

/**
 * The agent is already at the ticket's destination.
 * <p>
 * We perform a trivial "no-op" move by telling the
 * agent that it arrived.
 */
public class DispatchNoopHandler extends AbstractHandler {

  public DispatchNoopHandler(MobilitySupport support) {
    super(support);
  }

  public void run() {
    dispatchNoop();
  }

  private void dispatchNoop() {

    try {

      if (log.isInfoEnabled()) {
        log.info("Initiate agent "+id+" \"no-op\" dispatch");
      }

      checkTicket();

      // simply inform the agent
      onDispatch();
      onArrival();

    } catch (Exception e) {

      // the agent should be okay

      if (log.isErrorEnabled()) {
        log.error("Failed agent "+id+" \"no-op\" dispatch", e);
      }
      onFailure(e);
      return;
    }

    if (log.isInfoEnabled()) {
      log.info("Completed agent "+id+" \"no-op\" dispatch");
    }

  }

  private void checkTicket() {

    if (log.isDebugEnabled()) {
      log.debug(
          "Check dispatch ticket on node "+nodeId+
          " of agent "+id+" and ticket "+ticket);
    }

    // check for non-restart
    if (((MoveTicket)ticket).isForceRestart()) {
      throw new MobilityException(
          "Noop dispatch on a force-restart?");
    }

    // check for local
    MessageAddress destNode = ((MoveTicket)ticket).getDestinationNode();
    if ((destNode != null) &&
        (!(destNode.equals(nodeId)))) {
      throw new MobilityException(
          "Noop dispatch on a non-local destination "+destNode);
    }

    // check agent assertion
    MessageAddress mobileAgent = ((MoveTicket)ticket).getMobileAgent();
    if ((mobileAgent != null) &&
        (!(mobileAgent.equals(id)))) {
      throw new MobilityException(
          "Move agent "+id+
          " doesn't match ticket agent "+mobileAgent);
    }

    // check origin assertion
    MessageAddress originNode = ((MoveTicket)ticket).getOriginNode();
    if ((originNode != null) &&
        (!(originNode.equals(nodeId)))) {
      throw new MobilityException(
          "Move origin "+nodeId+" for "+id+
          " doesn't match ticket origin "+originNode);
    }
  }

  public String toString() {
    return "Move (dispatch-noop) of agent "+id;
  }
}

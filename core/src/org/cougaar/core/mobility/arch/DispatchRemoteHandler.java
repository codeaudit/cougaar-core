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
import org.cougaar.core.component.StateObject;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mobility.MobilityException;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.GenericStateModel;

/**
 * The agent should be moved to a remote node.
 */
public class DispatchRemoteHandler extends AbstractHandler {

  private GenericStateModel model;
  private ComponentDescription desc;
  private StateObject stateProvider;

  public DispatchRemoteHandler(
      MobilitySupport support,
      GenericStateModel model,
      ComponentDescription desc,
      StateObject stateProvider) {
    super(support);
    this.model = model;
    this.desc = desc;
    this.stateProvider = stateProvider;
  }

  public void run() {
    dispatchRemote();
  }

  private void dispatchRemote() {

    StateTuple tuple;
    boolean didSuspend = false;
    try {

      if (log.isInfoEnabled()) {
        log.info(
            "Begin remote transfer of agent "+id+" from "+
            nodeId+" to "+ticket.getDestinationNode());
      }

      checkTicket();

      onDispatch();

      model.suspend();
      didSuspend = true;

      Object state = getAgentState();
      tuple = new StateTuple(desc, state);

    } catch (Exception e) {

      // obtaining the state shouldn't mangle the agent, so we'll
      // attempt to resume the agent from its suspension.

      if (log.isErrorEnabled()) {
        log.error(
            "Unable to prepare agent for transfer", e);
      }
      if (didSuspend) {
        model.resume();
      }

      onFailure(e);

      return;
    }

    try {

      sendTransfer(tuple);

    } catch (Exception e) {

      // not sure if the message was sent...
      //
      // attempt to resume the agent from its suspension.

      if (log.isErrorEnabled()) {
        log.error(
            "Failed message delivery for agent transfer", e);
      }

      model.resume();

      onFailure(e);

      return;
    }

    setPendingModel(model);
  }

  private void checkTicket() {

    if (log.isDebugEnabled()) {
      log.debug(
          "Check dispatch ticket on node "+nodeId+
          " of agent "+id+" and ticket "+ticket);
    }

    // check for non-local destination
    MessageAddress destNode = ticket.getDestinationNode();
    if ((destNode == null) ||
        (destNode.equals(nodeId))) {
      throw new InternalError(
          "Remote dispatch on a local destination "+destNode);
    }

    // check agent assertion
    MessageAddress mobileAgent = ticket.getMobileAgent();
    if ((mobileAgent != null) &&
        (!(mobileAgent.equals(id)))) {
      throw new MobilityException(
        "Move agent "+id+
        " doesn't match ticket agent "+mobileAgent);
    }

    // check origin assertion
    MessageAddress originNode = ticket.getOriginNode();
    if ((originNode != null) &&
        (!(originNode.equals(nodeId)))) {
      throw new MobilityException(
        "Move origin "+nodeId+" for "+id+
        " doesn't match ticket origin "+originNode);
    }
  }

  private Object getAgentState() {
    // capture the agent state

    if (log.isDebugEnabled()) {
      log.debug("Agent "+id+" suspended, now capturing state");
    }

    if (stateProvider == null) {
      if (log.isWarnEnabled()) {
        log.warn("Agent "+id+" has no state?");
      }
      return null;
    }

    if (log.isDebugEnabled()) {
      log.debug("Capture state for agent "+id);
    }

    Object state;
    try {
      state = stateProvider.getState();
    } catch (Exception e) {
      throw new MobilityException(
            "Unable to capture state for agent "+id+
            ", will attempt resume", e);
    }

    if (log.isDebugEnabled()) {
      // FIXME maybe not log this -- state may be very verbose!
      log.debug("Agent "+id+" state: "+state);
    }

    return state;
  }

  public String toString() {
    return "Move (dispatch-remote) of agent "+id;
  }
}

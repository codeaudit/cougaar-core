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

package org.cougaar.core.mobility.ldm;

import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;

/**
 * Package-private implementation of MoveAgent.
 * <p>
 * Backwards compatibility for the old MoveAgent API.
 */
class MoveAgentAdapter
extends AgentControlImpl 
implements MoveAgent {

  private Status myStatus;

  public MoveAgentAdapter(
      UID uid,
      UID ownerUID,
      MessageAddress source,
      MessageAddress target,
      Ticket ticket) {
    super(uid, ownerUID, source, target, ticket);
  }

  public MoveAgentAdapter(
      UID uid,
      MessageAddress source,
      MessageAddress target,
      Ticket ticket) {
    super(uid, null, source, target, ticket);
  }

  public Ticket getTicket() {
    return (Ticket) getAbstractTicket();
  }

  public Status getStatus() {
    return myStatus;
  }

  public void setStatus(Status xStatus) {
    if (xStatus != null) {
      setStatus(
          ((xStatus.getCode() == Status.OKAY) ? 
           (MOVED) : 
           FAILURE),
          xStatus.getThrowable());
    }
  }

  public void setStatus(int status, Throwable stack) {
    super.setStatus(status, stack);
    setMyStatus(status, stack);
  }

  public int updateResponse(
      MessageAddress t, Object response) {
    int ret = super.updateResponse(t, response);
    if (ret != Relay.NO_CHANGE) {
      setMyStatus(getStatusCode(), getFailureStackTrace());
    }
    return ret;
  }

  private void setMyStatus(int status, Throwable stack) {
    if (status == NONE) {
      myStatus = null;
    } else if (status == MOVED) {
      myStatus = new Status(
          Status.OKAY, 
          ("Agent arrived at time "+System.currentTimeMillis()), 
          stack);
    } else {
      myStatus = new Status(
          Status.FAILURE, 
          ("Failed at time "+System.currentTimeMillis()),
          stack);
    }
  }

}

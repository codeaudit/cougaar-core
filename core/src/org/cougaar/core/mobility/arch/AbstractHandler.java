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
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModel;

/**
 * Base class for mobility-related handlers.
 */
public abstract class AbstractHandler implements Runnable {

  protected final MobilitySupport support;

  protected final MessageAddress id;
  protected final MessageAddress nodeId;
  protected final MessageAddress sender;
  protected final Ticket ticket;
  protected final LoggingService log;

  public AbstractHandler(MobilitySupport support) {
    this.support = support;
    // save these for easy base-class access
    this.id = support.getId();
    this.nodeId = support.getNodeId();
    this.sender = support.getSender();
    this.ticket = support.getTicket();
    this.log = support.getLog();
  }

  public abstract void run();

  // msg-sender

  protected void sendTransfer(StateTuple tuple) {
    support.sendTransfer(tuple);
  }

  protected void sendAck() {
    support.sendAck();
  }

  protected void sendNack(Throwable throwable) {
    support.sendNack(throwable);
  }

  // model-reg

  protected void setPendingModel(GenericStateModel model) {
    support.setPendingModel(model);
  }

  protected GenericStateModel takePendingModel() {
    return support.takePendingModel();
  }

  // agent-container

  protected void addAgent(StateTuple tuple) {
    support.addAgent(tuple);
  }

  protected void removeAgent() {
    support.removeAgent();
  }

  // detachable mobility-listener

  protected void onDispatch() {
    support.onDispatch();
  }

  protected void onArrival() {
    support.onArrival();
  }

  protected void onFailure(Throwable throwable) {
    support.onFailure(throwable);
  }

  protected void onRemoval() {
    support.onRemoval();
  }

  // to-string

  public String toString() {
    return "Move (?) for agent "+id;
  }
}

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
package org.cougaar.core.mobility.service;

import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mobility.arch.MobilitySupport;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModel;

/**
 * Base class for mobility-support class.
 */
abstract class AbstractMobilitySupport
implements MobilitySupport 
{

  protected final MessageAddress sender;
  protected final MessageAddress id;
  protected final MessageAddress nodeId;
  protected final Ticket ticket;

  protected final LoggingService log;
  protected final MobilityListenerRegistry mlReg;

  public AbstractMobilitySupport(
      MessageAddress sender,
      MessageAddress id,
      MessageAddress nodeId,
      Ticket ticket,
      LoggingService log,
      MobilityListenerRegistry mlReg) {
    this.sender = sender;
    this.id = id;
    this.nodeId = nodeId;
    this.ticket = ticket;
    this.log = log;
    this.mlReg = mlReg;
  }

  // fields

  public LoggingService getLog() {
    return log;
  }

  public MessageAddress getId() {
    return id;
  }

  public MessageAddress getNodeId() {
    return nodeId;
  }

  public MessageAddress getSender() {
    return sender;
  }

  public Ticket getTicket() {
    return ticket;
  }

  // msg-sender

  public void sendTransfer(StateTuple tuple) {
    MessageAddress destNode = ticket.getDestinationNode();
    if (destNode == null) {
      destNode = nodeId;
    }
    TransferMessage tm = 
      new TransferMessage(
          nodeId,
          destNode,
          id,
          ticket,
          tuple);
    sendMessage(tm);
  }

  public void sendAck() {
    AckMessage am = 
      new AckMessage(
          nodeId,
          sender,
          id,
          ticket);
    sendMessage(am);
  }

  public void sendNack(Throwable throwable) {
    NackMessage nm = 
      new NackMessage(
          nodeId,
          sender,
          id,
          ticket,
          throwable);
    sendMessage(nm);
  }

  protected abstract void sendMessage(Message message);

  // model-reg

  public void setPendingModel(GenericStateModel model) {
    PendingEntry pe = new PendingEntry(id, ticket, model);
    // FIXME check for already-contains
    // FIXME also use this to block multi-moves?
    putPendingEntry(pe);
  }

  public GenericStateModel takePendingModel() {
    PendingEntry pe = removePendingEntry();
    if (pe == null) {
      return null;
    }
    // FIXME check id & ticket?
    return pe.getModel();
  }

  protected abstract PendingEntry putPendingEntry(PendingEntry pe);

  protected abstract PendingEntry removePendingEntry();

  // agent-container

  public abstract void addAgent(StateTuple tuple);

  public abstract void removeAgent();

  // detachable mobility-listener

  public void onDispatch() {
    mlReg.onDispatch(id, ticket);
  }

  public void onArrival() {
    mlReg.onArrival(id, ticket);
  }

  public void onFailure(Throwable throwable) {
    mlReg.onFailure(id, ticket, throwable);
  }

  public void onRemoval() {
    mlReg.removeAll(id);
  }

  public String toString() {
    return "Mobility support for agent "+id+" on "+nodeId;
  }

  protected static class PendingEntry {
    private final MessageAddress id;
    private final Ticket ticket;
    private final GenericStateModel model;
    public PendingEntry(
        MessageAddress id,
        Ticket ticket,
        GenericStateModel model) {
      this.id = id;
      this.ticket = ticket;
      this.model = model;
    }
    public MessageAddress getAddress() {
      return id;
    }
    public Ticket getTicket() {
      return ticket;
    }
    public GenericStateModel getModel() {
      return model;
    }
    public String toString() {
      return "pending-entry for "+id;
    }
  }

}

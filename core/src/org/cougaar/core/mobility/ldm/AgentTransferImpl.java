/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.mobility.ldm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;
import java.util.Collections;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceInputStream;
import org.cougaar.core.persist.PersistenceOutputStream;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;

/**
 * Package-private implementation of AgentTransfer.
 * <p>
 * This uses a Relay.
 */
class AgentTransferImpl 
implements AgentTransfer, Relay.Source, Relay.Target, XMLizable, Serializable {

  private static final TransferStatus NO_TRANSFER_STATUS =
    new TransferStatus(NO_STATUS, null);

  private final UID uid;
  private final UID ownerUID;
  private final MessageAddress source;
  private final MessageAddress target;
  private final AbstractTicket ticket;
  // NOTE transient!
  private transient StateTuple state;
  private TransferStatus transferStatus;

  private transient Set _targets;

  public AgentTransferImpl(
      UID uid,
      UID ownerUID,
      MessageAddress source,
      MessageAddress target,
      AbstractTicket ticket,
      StateTuple state) {
    this.uid = uid;
    this.ownerUID = ownerUID;
    this.source = source;
    this.target = target;
    this.ticket = ticket;
    this.state = state;
    if ((uid == null) ||
        (source == null) ||
        (ticket == null) ||
        (state == null)) {
      throw new IllegalArgumentException(
          "null uid/source/target/ticket/state");
    }
    cacheTargets();
    // initial transferStatus:
    this.transferStatus = NO_TRANSFER_STATUS;
  }

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }

  // AgentTransfer:

  public UID getOwnerUID() {
    return ownerUID;
  }

  public MessageAddress getTarget() {
    return target;
  }

  public AbstractTicket getTicket() {
    return ticket;
  }

  public StateTuple getState() {
    return state;
  }

  public int getStatusCode() {
    return transferStatus.getStatusCode();
  }

  public Throwable getFailureStackTrace() {
    return transferStatus.getStack();
  }

  public void setStatus(int statusCode, Throwable stack) {
    TransferStatus newTS = new TransferStatus(statusCode, stack);
    if (!(transferStatus.equals(NO_TRANSFER_STATUS))) {
      throw new IllegalArgumentException(
          "Status already set to "+transferStatus+
          ", can't override with "+newTS);
    }
    transferStatus = newTS;
    // force GC:
    state = null;
  }

  // Relay.Source:

  private void cacheTargets() {
    _targets = 
      ((target != null) ? 
       Collections.singleton(target) :
       Collections.EMPTY_SET);
  }
  public Set getTargets() {
    return _targets;
  }
  public Object getContent() {
    return this;
  }
  public Relay.TargetFactory getTargetFactory() {
    return null;
  }
  public int updateResponse(
      MessageAddress t, Object response) {
    TransferStatus newTS = (TransferStatus) response;
    // assert local-agent == getSource()
    // assert newTS != null
    if (!(transferStatus.equals(newTS))) {
      transferStatus = newTS;
      return Relay.RESPONSE_CHANGE;
    }
    return Relay.NO_CHANGE;
  }

  // Relay.Target:

  public MessageAddress getSource() {
    return source;
  }
  public Object getResponse() {
    return 
      ((transferStatus != NO_TRANSFER_STATUS) ? 
       (transferStatus) : 
       null);
  }
  public int updateContent(Object content, Token token) {
    // currently the content is immutable
    // maybe support "abort" content in the future
    return Relay.NO_CHANGE;
  }

  public org.w3c.dom.Element getXML(org.w3c.dom.Document doc) {
    return XMLize.getPlanObjectXML(this, doc);
  }
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof AgentTransferImpl)) { 
      return false;
    } else {
      UID u = ((AgentTransferImpl) o).uid;
      return uid.equals(u);
    }
  }
  public int hashCode() {
    return uid.hashCode();
  }
  private void writeObject(
      ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    if (stream instanceof PersistenceOutputStream) {
      // don't persist state!
    } else {
      stream.writeObject(state);
    }
  }
  private void readObject(ObjectInputStream stream) 
    throws ClassNotFoundException, IOException {
      stream.defaultReadObject();
      if (stream instanceof PersistenceInputStream) {
      } else {
        state = (StateTuple) stream.readObject();
      }
      cacheTargets();
    }

  public String toString() {
    return "agent transfer of "+ticket;
  }

  private static class TransferStatus implements Serializable {
    public final int statusCode;
    public final Throwable stack;
    public TransferStatus(int statusCode, Throwable stack) {
      this.statusCode = statusCode;
      this.stack = stack;
    }
    public int getStatusCode() { return statusCode; }
    public Throwable getStack() { return stack; }
    public boolean equals(Object o) {
      TransferStatus ts = (TransferStatus) o;
      return 
        ((statusCode == ts.statusCode) && 
         ((stack != null) ?
          (stack.equals(ts.stack)) :
          (ts.stack == null)));
    }
    public String toString() {
      return "status ("+statusCode+") stack("+stack+")";
    }
  }

}

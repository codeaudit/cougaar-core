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
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mobility.MoveTicket;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceInputStream;
import org.cougaar.core.persist.PersistenceOutputStream;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;

/**
 * Package-private implementation of AgentMove.
 * <p>
 * This uses a Relay.
 */
class AgentMoveImpl 
implements AgentMove, Relay.Source, Relay.Target, XMLizable, Serializable {

  private static final MoveStatus NO_MOVE_STATUS =
    new MoveStatus(NO_STATUS, null);

  private final UID uid;
  private final UID ownerUID;
  private final MessageAddress source;
  private final MessageAddress target;
  private final MoveTicket ticket;
  private MoveStatus moveStatus;

  private transient Set _targets;

  public AgentMoveImpl(
      UID uid,
      UID ownerUID,
      MessageAddress source,
      MessageAddress target,
      MoveTicket ticket) {
    this.uid = uid;
    this.ownerUID = ownerUID;
    this.source = source;
    this.target = target;
    this.ticket = ticket;
    if ((uid == null) ||
        (source == null) ||
        (ticket == null)) {
      throw new IllegalArgumentException(
          "null uid/ticket");
    }
    // expecting target to be either moveA or origN
    cacheTargets();
    // initial moveStatus:
    this.moveStatus = NO_MOVE_STATUS;
  }

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }

  // AgentMove:

  public UID getOwnerUID() {
    return ownerUID;
  }

  public MessageAddress getTarget() {
    return target;
  }

  public AbstractTicket getAbstractTicket() {
    return (AbstractTicket)ticket;
  }

  public MoveTicket getTicket() {
    return ticket;
  }
  
  public int getStatusCode() {
    return moveStatus.getStatusCode();
  }

  public String getStatusCodeAsString() {
    int i = getStatusCode();
    switch (i) {
      case NO_STATUS: return "NO_STATUS";
      case SUCCESS_STATUS: return "SUCCESS_STATUS";
      case FAILURE_STATUS: return "FAILURE_STATUS";
      default: return "Unknown ("+i+")";
    }
  }

  public Throwable getFailureStackTrace() {
    return moveStatus.getStack();
  }

  public void setStatus(int statusCode, Throwable stack) {
    MoveStatus newMS = new MoveStatus(statusCode, stack);
    if (!(moveStatus.equals(NO_MOVE_STATUS))) {
      throw new IllegalArgumentException(
          "Status already set to "+moveStatus+
          ", can't override with "+newMS);
    }
    moveStatus = newMS;
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
    return AgentMoveImplFactory.INSTANCE;
  }
  public int updateResponse(
      MessageAddress t, Object response) {
    MoveStatus newMS = (MoveStatus) response;
    // assert local-agent == getSource()
    // assert newMS != null
    if (!(moveStatus.equals(newMS))) {
      moveStatus = newMS;
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
      ((!(moveStatus.equals(NO_MOVE_STATUS))) ?
       (moveStatus) : 
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
    } else if (!(o instanceof AgentMoveImpl)) { 
      return false;
    } else {
      UID u = ((AgentMoveImpl) o).uid;
      return uid.equals(u);
    }
  }
  public int hashCode() {
    return uid.hashCode();
  }
  private void readObject(ObjectInputStream stream) 
    throws ClassNotFoundException, IOException {
      stream.defaultReadObject();
      cacheTargets();
    }

  public String toString() {
    return 
      "Agent move request "+uid+
      " owned by "+ownerUID+
      " and ticket ("+ticket+
      ") with source "+source+
      " and target "+target+
      ", status is "+moveStatus;
  }

  private static class MoveStatus implements Serializable {
    public final int statusCode;
    public final Throwable stack;
    public MoveStatus(int statusCode, Throwable stack) {
      this.statusCode = statusCode;
      this.stack = stack;
    }
    public int getStatusCode() { return statusCode; }
    public Throwable getStack() { return stack; }
    public boolean equals(Object o) {
      MoveStatus ts = (MoveStatus) o;
      return 
        ((ts == this) ||
         ((statusCode == ts.statusCode) && 
          ((stack != null) ?
           (stack.equals(ts.stack)) :
           (ts.stack == null))));
    }
    public String toString() {
      return "status ("+statusCode+") stack("+stack+")";
    }
  }

  /**
   * Simple factory implementation.
   */
  private static class AgentMoveImplFactory 
    implements Relay.TargetFactory, Serializable {

      public static AgentMoveImplFactory INSTANCE = 
        new AgentMoveImplFactory();

      private AgentMoveImplFactory() { }

      public Relay.Target create(
          UID uid, MessageAddress source, Object content,
          Relay.Token token) {
        AgentMoveImpl adi = (AgentMoveImpl) content;
        return new AgentMoveImpl(
            adi.uid, adi.ownerUID, source, null, adi.ticket);
      }

      private Object readResolve() {
        return INSTANCE;
      }
    }
}

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
import org.cougaar.core.mobility.RemoveTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceInputStream;
import org.cougaar.core.persist.PersistenceOutputStream;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;

/**
 * Package-private implementation of AgentRemove.
 * <p>
 * This uses a Relay.
 */
class AgentRemoveImpl 
implements AgentRemove, Relay.Source, Relay.Target, XMLizable, Serializable {

  private static final RemoveStatus NO_REMOVE_STATUS =
    new RemoveStatus(NONE, null);

  private final UID uid;
  private final UID ownerUID;
  private final MessageAddress source;
  private final MessageAddress target;
  private final RemoveTicket removeTicket;
  private RemoveStatus removeStatus;

  private transient Set _targets;

  public AgentRemoveImpl(
      UID uid,
      UID ownerUID,
      MessageAddress source,
      MessageAddress target,
      RemoveTicket removeTicket) {
    this.uid = uid;
    this.ownerUID = ownerUID;
    this.source = source;
    this.target = target;
    this.removeTicket = removeTicket;
    if ((uid == null) ||
        (source == null) ||
        (removeTicket == null)) {
      throw new IllegalArgumentException(
          "null uid/ticket");
    }
    // expecting target to be either removeA or origN
    cacheTargets();
    // initial removeStatus:
    this.removeStatus = NO_REMOVE_STATUS;
  }

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }

  // AgentRemove:

  public UID getOwnerUID() {
    return ownerUID;
  }

  public MessageAddress getTarget() {
    return target;
  }

  public RemoveTicket getRemoveTicket() {
    return removeTicket;
  }

  public int getStatusCode() {
    return removeStatus.getStatusCode();
  }

  public String getStatusCodeAsString() {
    int i = getStatusCode();
    switch (i) {
      case NONE: return "NONE";
      case REMOVED: return "REMOVED";
      case ALREADY_REMOVED: return "ALREADY_REMOVED";
      case FAILURE: return "FAILURE";
      default: return "Unknown ("+i+")";
    }
  }

  public Throwable getFailureStackTrace() {
    return removeStatus.getStack();
  }

  public void setStatus(int statusCode, Throwable stack) {
    RemoveStatus newRS = new RemoveStatus(statusCode, stack);
    if (!(removeStatus.equals(NO_REMOVE_STATUS))) {
      throw new IllegalArgumentException(
          "Status already set to "+removeStatus+
          ", can't override with "+newRS);
    }
    removeStatus = newRS;
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
    return AgentRemoveImplFactory.INSTANCE;
  }
  public int updateResponse(
      MessageAddress t, Object response) {
    RemoveStatus newRS = (RemoveStatus) response;
    // assert local-agent == getSource()
    // assert newAS != null
    if (!(removeStatus.equals(newRS))) {
      removeStatus = newRS;
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
      ((!(removeStatus.equals(NO_REMOVE_STATUS))) ?
       (removeStatus) : 
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
    } else if (!(o instanceof AgentRemoveImpl)) { 
      return false;
    } else {
      UID u = ((AgentRemoveImpl) o).uid;
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
      "Agent remove request "+uid+
      " owned by "+ownerUID+
      " and ticket ("+removeTicket+
      ") with source "+source+
      " and target "+target+
      ", status is "+removeStatus;
  }

  private static class RemoveStatus implements Serializable {
    public final int statusCode;
    public final Throwable stack;
    public RemoveStatus(int statusCode, Throwable stack) {
      this.statusCode = statusCode;
      this.stack = stack;
    }
    public int getStatusCode() { return statusCode; }
    public Throwable getStack() { return stack; }
    public boolean equals(Object o) {
      RemoveStatus rs = (RemoveStatus) o;
      return 
        ((rs == this) ||
         ((statusCode == rs.statusCode) && 
          ((stack != null) ?
           (stack.equals(rs.stack)) :
           (rs.stack == null))));
    }
    public String toString() {
      return "status ("+statusCode+") stack("+stack+")";
    }
  }

  /**
   * Simple factory implementation.
   */
  private static class AgentRemoveImplFactory 
    implements Relay.TargetFactory, Serializable {

      public static AgentRemoveImplFactory INSTANCE = 
        new AgentRemoveImplFactory();

      private AgentRemoveImplFactory() { }

      public Relay.Target create(
          UID uid, MessageAddress source, Object content,
          Relay.Token token) {
        AgentRemoveImpl ari = (AgentRemoveImpl) content;
        return new AgentRemoveImpl(
            ari.uid, ari.ownerUID, source, null, ari.removeTicket);
      }

      private Object readResolve() {
        return INSTANCE;
      }
    }
}

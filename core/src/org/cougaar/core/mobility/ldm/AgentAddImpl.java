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
import org.cougaar.core.mobility.AddTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceInputStream;
import org.cougaar.core.persist.PersistenceOutputStream;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;

/**
 * Package-private implementation of AgentAdd.
 * <p>
 * This uses a Relay.
 */
class AgentAddImpl 
implements AgentAdd, Relay.Source, Relay.Target, XMLizable, Serializable {

  private static final AddStatus NO_ADD_STATUS =
    new AddStatus(NONE, null);

  private final UID uid;
  private final UID ownerUID;
  private final MessageAddress source;
  private final MessageAddress target;
  private final AddTicket addTicket;
  private AddStatus addStatus;

  private transient Set _targets;

  public AgentAddImpl(
      UID uid,
      UID ownerUID,
      MessageAddress source,
      MessageAddress target,
      AddTicket addTicket) {
    this.uid = uid;
    this.ownerUID = ownerUID;
    this.source = source;
    this.target = target;
    this.addTicket = addTicket;
    if ((uid == null) ||
        (source == null) ||
        (addTicket == null)) {
      throw new IllegalArgumentException(
          "null uid/ticket");
    }
    // expecting target to be either addA or origN
    cacheTargets();
    // initial addStatus:
    this.addStatus = NO_ADD_STATUS;
  }

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }

  // AgentAdd:

  public UID getOwnerUID() {
    return ownerUID;
  }

  public MessageAddress getTarget() {
    return target;
  }

  public AddTicket getAddTicket() {
    return addTicket;
  }

  public int getStatusCode() {
    return addStatus.getStatusCode();
  }

  public String getStatusCodeAsString() {
    int i = getStatusCode();
    switch (i) {
    case NONE: return "NONOE";
    case CREATED: return "CREATED";
    case ALREADY_EXISTS: return "ALREADY_EXISTS";	
    case FAILURE: return "FAILURE";
    default: return "Unknown ("+i+")";
    }
  }

  public Throwable getFailureStackTrace() {
    return addStatus.getStack();
  }

  public void setStatus(int statusCode, Throwable stack) {
    AddStatus newAS = new AddStatus(statusCode, stack);
    if (!(addStatus.equals(NO_ADD_STATUS))) {
      throw new IllegalArgumentException(
          "Status already set to "+addStatus+
          ", can't override with "+newAS);
    }
    addStatus = newAS;
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
    return AgentAddImplFactory.INSTANCE;
  }
  public int updateResponse(
      MessageAddress t, Object response) {
    AddStatus newAS = (AddStatus) response;
    // assert local-agent == getSource()
    // assert newAS != null
    if (!(addStatus.equals(newAS))) {
      addStatus = newAS;
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
      ((!(addStatus.equals(NO_ADD_STATUS))) ?
       (addStatus) : 
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
    } else if (!(o instanceof AgentAddImpl)) { 
      return false;
    } else {
      UID u = ((AgentAddImpl) o).uid;
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
      "Agent add request "+uid+
      " owned by "+ownerUID+
      " and ticket ("+addTicket+
      ") with source "+source+
      " and target "+target+
      ", status is "+addStatus;
  }

  private static class AddStatus implements Serializable {
    public final int statusCode;
    public final Throwable stack;
    public AddStatus(int statusCode, Throwable stack) {
      this.statusCode = statusCode;
      this.stack = stack;
    }
    public int getStatusCode() { return statusCode; }
    public Throwable getStack() { return stack; }
    public boolean equals(Object o) {
      AddStatus as = (AddStatus) o;
      return 
        ((as == this) ||
         ((statusCode == as.statusCode) && 
          ((stack != null) ?
           (stack.equals(as.stack)) :
           (as.stack == null))));
    }
    public String toString() {
      return "status ("+statusCode+") stack("+stack+")";
    }
  }

  /**
   * Simple factory implementation.
   */
  private static class AgentAddImplFactory 
    implements Relay.TargetFactory, Serializable {

      public static AgentAddImplFactory INSTANCE = 
        new AgentAddImplFactory();

      private AgentAddImplFactory() { }

      public Relay.Target create(
          UID uid, MessageAddress source, Object content,
          Relay.Token token) {
        AgentAddImpl adi = (AgentAddImpl) content;
        return new AgentAddImpl(
            adi.uid, adi.ownerUID, source, null, adi.addTicket);
      }

      private Object readResolve() {
        return INSTANCE;
      }
    }
}

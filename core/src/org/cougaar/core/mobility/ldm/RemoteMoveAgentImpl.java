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

import java.io.Serializable;
import java.util.Set;
import java.util.Collections;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;

/**
 * Package-private implementation of a third-party
 * move of an agent, such as an external load-balancer.
 * <p>
 * This uses the Relay support to transfer the data
 * between the source agent and mobile agent.
 */
class RemoteMoveAgentImpl 
implements MoveAgent, Relay.Source, Relay.Target, XMLizable, Serializable {

  private final UID uid;
  private final MessageAddress source;
  private final Ticket ticket;
  private Status status;

  private transient Set _targets;

  public RemoteMoveAgentImpl(
      UID uid, MessageAddress source, Ticket ticket) {
    this.uid = uid;
    this.source = source;
    this.ticket = ticket;
    if ((uid == null) ||
        (source == null) ||
        (ticket == null)) {
      throw new IllegalArgumentException("null uid/source/ticket");
    }
    // assert ((ticket.getMobileAgent() != null) &&
    //         (!(source.equals(ticket.getMobileAgent()))))
    cacheTargets();
    // initial status:
    this.status = null;
  }

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }

  // MoveAgent:

  public MessageAddress getSource() {
    return source;
  }

  public Status getStatus() {
    return status;
  }
  public Ticket getTicket() {
    return ticket;
  }
  public void setStatus(Status status) {
    // assert local-agent == ticket.getMobileAgent()
    if (status == null) {
      throw new IllegalArgumentException("null status");
    }
    this.status = status;
  }

  // Relay.Source:

  private void cacheTargets() {
    _targets = Collections.singleton(ticket.getMobileAgent());
  }
  public Set getTargets() {
    return _targets;
  }
  public Object getContent() {
    return getTicket();
  }
  public Relay.TargetFactory getTargetFactory() {
    return RemoteMoveAgentImplFactory.INSTANCE;
  }
  public int updateResponse(
      MessageAddress t, Object response) {
    Status status = (Status) response;
    // assert local-agent == getSource()
    // assert status != null
    if (!(status.equals(this.status))) {
      this.status = status;
      return Relay.RESPONSE_CHANGE;
    }
    return Relay.NO_CHANGE;
  }

  // Relay.Target:

  public Object getResponse() {
    return getStatus();
  }
  public int updateContent(Object content, Token token) {
    // currently the content (uid, source, ticket) is immutable
    return Relay.NO_CHANGE;
  }

  public org.w3c.dom.Element getXML(org.w3c.dom.Document doc) {
    return XMLize.getPlanObjectXML(this, doc);
  }
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof RemoteMoveAgentImpl)) { 
      return false;
    } else {
      UID u = ((RemoteMoveAgentImpl) o).uid;
      return uid.equals(u);
    }
  }
  public int hashCode() {
    return uid.hashCode();
  }
  private void readObject(java.io.ObjectInputStream os) 
    throws ClassNotFoundException, java.io.IOException {
      os.defaultReadObject();
      cacheTargets();
    }
  public String toString() {
    return "move-remote ("+uid+", "+source+", "+ticket+", "+status+")";
  }

  /**
   * Simple factory implementation.
   */
  private static class RemoteMoveAgentImplFactory 
    implements Relay.TargetFactory, Serializable {

      public static RemoteMoveAgentImplFactory INSTANCE = 
        new RemoteMoveAgentImplFactory();

      private RemoteMoveAgentImplFactory() { }

      public Relay.Target create(
          UID uid, MessageAddress source, Object content,
          Relay.Token token) {
        Ticket t = (Ticket) content;
        // assert (source.equals(t.getMobileAgent()))
        return new RemoteMoveAgentImpl(uid, source, t);
      }

      private Object readResolve() {
        return INSTANCE;
      }
    }
}

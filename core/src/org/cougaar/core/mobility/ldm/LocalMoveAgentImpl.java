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
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;

/**
 * Package-private implementation of an agent move
 * for an agent that's moving itself ("a rover").
 */
class LocalMoveAgentImpl 
implements MoveAgent, XMLizable, Serializable 
{

  private final UID uid;
  private final MessageAddress self;
  private final Ticket ticket;
  private Status status;

  public LocalMoveAgentImpl(
      UID uid, MessageAddress self, Ticket ticket) {
    this.uid = uid;
    this.self = self;
    this.ticket = ticket;
    if ((uid == null) ||
        (self == null) ||
        (ticket == null)) {
      throw new IllegalArgumentException("null uid/self/ticket");
    }
    // assert ((ticket.getMobileAgent() == null) ||
    //         (self.equals(ticket.getMobileAgent())))
    // initial status:
    this.status = null;
  }

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }
  public MessageAddress getSource() {
    return self;
  }
  public Ticket getTicket() {
    return ticket;
  }
  public Status getStatus() {
    return status;
  }
  public void setStatus(Status status) {
    this.status = status;
  }
  public org.w3c.dom.Element getXML(org.w3c.dom.Document doc) {
    return XMLize.getPlanObjectXML(this, doc);
  }
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof LocalMoveAgentImpl)) {
      return false;
    } else {
      UID u = ((LocalMoveAgentImpl) o).uid;
      return uid.equals(u);
    }
  }
  public int hashCode() {
    return uid.hashCode();
  }
  public String toString() {
    return "move-local ("+uid+", "+self+", "+ticket+", "+status+")";
  }
}

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
package org.cougaar.core.mobility;

import java.io.Serializable;
import java.util.*;

import org.cougaar.core.mts.MessageAddress;

/**
 * A MobilityListener that buffers the calls ("onArrival", etc) o
 * until the "deliver(..)" method is invoked.
 * <p>
 * This can be used to safely buffer these calls until a plugin 
 * is ready to "execute()".  The plugin can also trigger itself
 * after it adds to the buffer.
 */
public class BufferedMobilityListener 
implements MobilityListener, Serializable
{

  private final MessageAddress addr;
  private final List eventQueue = new ArrayList(5);

  public BufferedMobilityListener(
      MessageAddress addr) {
    this.addr = addr;
    if (addr == null) {
      throw new IllegalArgumentException(
          "Must specify the agent's address");
    }
  }

  public void deliver(MobilityListener listener) {
    deliverEvents(listener);
  }

  // MobilityEvent types
  private static final int DISPATCH = 0;
  private static final int ARRIVAL = 1;
  private static final int FAILURE = 2;

  public MessageAddress getAddress() {
    return addr;
  }

  public void onDispatch(Ticket ticket) {
    addEvent(DISPATCH, ticket, null);
  }

  public void onArrival(Ticket ticket) {
    addEvent(ARRIVAL, ticket, null);
  }

  public void onFailure(Ticket ticket, Throwable t) {
    addEvent(FAILURE, ticket, t);
  }

  private void deliverEvents(MobilityListener listener) {
    List l;
    synchronized (eventQueue) {
      if (listener == null) {
        eventQueue.clear();
        return;
      }
      if (eventQueue.isEmpty()) {
        return;
      }
      l = new ArrayList(eventQueue);
      eventQueue.clear();
    }
    for (int i = 0, n = l.size(); i < n; i++) {
      MobilityEvent me = (MobilityEvent) l.get(i);
      Ticket ticket = me.getTicket();
      switch (me.getType()) {
        case DISPATCH: 
          listener.onDispatch(ticket);
          break;
        case ARRIVAL: 
          listener.onArrival(ticket);
          break;
        case FAILURE: 
          listener.onFailure(ticket, me.getThrowable());
          break;
      }
    }
  }

  private void addEvent(int type, Ticket ticket, Throwable t) {
    synchronized (eventQueue) {
      MobilityEvent me = new MobilityEvent(type, ticket, t);
      eventQueue.add(me);
    }
  }

  private void readObject(java.io.ObjectInputStream ois) 
  throws java.io.IOException, ClassNotFoundException {
    ois.defaultReadObject();
    if (addr == null) {
      throw new java.io.InvalidObjectException("Null agent address");
    }
  }

  public String toString() {
    return "Buffered mobility-listener for agent "+addr;
  }

  private static class MobilityEvent implements Serializable {
    private final int type;
    private final Ticket ticket;
    private final Throwable throwable;
    public MobilityEvent(int type, Ticket ticket, Throwable throwable) {
      this.type = type;
      this.ticket = ticket;
      this.throwable = throwable;
    }
    public int getType() {
      return type;
    }
    public Ticket getTicket() {
      return ticket;
    }
    public Throwable getThrowable() {
      return throwable;
    }
    public String getTypeAsString() {
      switch (type) {
        case DISPATCH: return "dispatch";
        case ARRIVAL:  return "arrival";
        case FAILURE: return "failure";
        default: return "(unknown "+type+")";
      }
    }
    public String toString() {
      return 
        "<move "+type+
        " ticket "+
        ((ticket != null) ? ticket.getIdentifier() : "")+
        " cause "+
        ((throwable != null) ? throwable.getMessage() : "")+
        ">";
    }
  }
}

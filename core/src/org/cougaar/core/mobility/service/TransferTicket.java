/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.persist.PersistenceInputStream;
import org.cougaar.core.persist.PersistenceOutputStream;

/**
 * Package-private ticket to transfer an agent between nodes.
 */
final class TransferTicket 
extends AbstractTicket 
implements Serializable {

  private final MoveTicket moveTicket;
  private final ComponentDescription desc;
  private Object state;

  public TransferTicket(
      MoveTicket moveTicket,
      ComponentDescription desc,
      Object state) {
    this.moveTicket = moveTicket;
    this.desc = desc;
    this.state = state;
  }

  public MoveTicket getMoveTicket() {
    return moveTicket;
  }

  public ComponentDescription getComponentDescription() {
    return desc;
  }

  public Object getState() {
    return state;
  }

  public void clearState() {
    // force GC
    state = null;
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
        state = stream.readObject();
      }
    }

  public int hashCode() {
    return moveTicket.hashCode();
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof TransferTicket)) {
      return false;
    } else {
      TransferTicket t = (TransferTicket) o;
      return moveTicket.equals(t.moveTicket);
    }
  }
  
  public String toString() {
    return "Node-to-Node transfer of "+moveTicket;
  }
}

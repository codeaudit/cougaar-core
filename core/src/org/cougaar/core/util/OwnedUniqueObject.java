/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
package org.cougaar.core.util;

import java.io.IOException;
import java.io.ObjectOutputStream;

import org.cougaar.core.mts.MessageAddress;

/**
 * Simple implementation of UniqueObject for objects that don't manage the UID themselves.
 **/

public abstract class OwnedUniqueObject extends SimpleUniqueObject {
  protected MessageAddress owner;

  private transient Throwable allocationContext;

  protected OwnedUniqueObject() {
    allocationContext = new Throwable("Allocation context");
  }

  public boolean isFrom(MessageAddress where) {
    if (owner == null) {
      ownerError("owner was never set");
    }
    return where.equals(owner);
  }

  public void setOwner(MessageAddress newOwner) {
    allocationContext = new Throwable("Allocation context");
    owner = newOwner;
  }

  public MessageAddress getOwner() {
    if (owner == null) {
      ownerError("owner was never set");
    }
    return owner;
  }

  /** alias for getOwner */
  public MessageAddress getSource() {
    return getOwner();
  }

  private void ownerError(String reason) {
    if (allocationContext != null) {
      allocationContext.printStackTrace();
    } else {
      System.err.println("OwnedUniqueObject deserialized");
    }
    throw new RuntimeException(reason + ": " + this);
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    if (owner == null) {
      ownerError("writeObject with no owner");
    }
    stream.defaultWriteObject();
  }
}


/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.society;

import org.cougaar.core.cluster.ClusterIdentifier;
import java.io.ObjectOutputStream;
import java.io.IOException;

/**
 * Simple implementation of UniqueObject for objects that don't manage the UID themselves.
 **/

public abstract class OwnedUniqueObject extends SimpleUniqueObject {
  protected ClusterIdentifier owner;

  private transient Throwable allocationContext;

  protected OwnedUniqueObject() {
    allocationContext = new Throwable("Allocation context");
  }

  public boolean isFrom(ClusterIdentifier where) {
    if (owner == null) {
      ownerError("owner was never set");
    }
    return where.equals(owner);
  }

  public void setOwner(ClusterIdentifier newOwner) {
    allocationContext = new Throwable("Allocation context");
    owner = newOwner;
  }

  public ClusterIdentifier getOwner() {
    if (owner == null) {
      ownerError("owner was never set");
    }
    return owner;
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


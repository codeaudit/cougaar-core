package org.cougaar.core.society;

import java.io.ObjectOutputStream;
import java.io.IOException;

/**
 * Simple implementation of UniqueObject for objects that don't manage the UID themselves.
 **/

public abstract class SimpleUniqueObject implements UniqueObject {
  /** The UID of the object **/
  protected UID uid;

  /** DEBUGGING
   * @deprecated Should be turned off
   **/
  private transient Throwable allocationContext;

  protected SimpleUniqueObject() {
    allocationContext = new Throwable("Allocation context");
  }

  /**
   * @return the UID of a UniqueObject.  If the object was created
   * correctly (e.g. via a Factory), will be non-null.
   **/
  public UID getUID() {
    if (uid == null) {
      uidError("uid was never set");
    }
    return uid;
  }

  /**
   * Set the UID of a UniqueObject.  This should only be done by
   * an LDM factory.  Will throw a RuntimeException if
   * the UID was already set.
   **/
  public void setUID(UID newUID) {
    if (uid != null && !uid.equals(newUID)) {
      uidError("uid already set");
    }
    allocationContext = new Throwable("setUID context");
    uid = newUID;
  }

  private void uidError(String reason) {
    if (allocationContext != null) {
      allocationContext.printStackTrace();
    } else {
      System.err.println("UniqueObject deserialized");
    }
    throw new RuntimeException(reason + ": " + this);
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    if (uid == null) {
      uidError("writeObject with no uid");
    }
    stream.defaultWriteObject();
  }
}

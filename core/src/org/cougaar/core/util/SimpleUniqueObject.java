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
   * a domain factory.  Will throw a RuntimeException if
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

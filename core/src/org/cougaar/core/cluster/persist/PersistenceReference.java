/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster.persist;

import java.io.*;

/**
 * Objects of this class replace objects that have been previously
 * persisted. During rehydration this objects are resolved into the
 * real objects. It is merely an Integer.
 */

public class PersistenceReference implements Serializable {
  private int id;
  public PersistenceReference(int id) {
    this.id = id;
  }
  public int intValue() {
    return id;
  }
  public String toString() {
    return "" + id;
  }
}

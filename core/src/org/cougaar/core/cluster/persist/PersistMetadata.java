/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster.persist;

import java.io.Serializable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

import org.cougaar.core.cluster.persist.PersistenceState;

/**
 * Metadata object which is persisted.  Contains state which cannot be easily
 * derived.
 **/
public class PersistMetadata implements Serializable
{
  public PersistMetadata() {}

  private PersistenceState uidServerState = null;
  public PersistenceState getUIDServerState() { return uidServerState; }
  public void setUIDServerState(PersistenceState state) { uidServerState = state; }
}

/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

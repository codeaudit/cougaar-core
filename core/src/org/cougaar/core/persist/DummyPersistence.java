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
 
package org.cougaar.core.persist;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import org.cougaar.core.agent.ClusterContext;
import org.cougaar.core.blackboard.Envelope;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.PersistenceEnvelope;
import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.core.service.DataProtectionKeyEnvelope;
import org.cougaar.core.service.DataProtectionKey;

/**
 * This persistence class provides implementation stubs that do
 * nothing. Running with this Persistence implementation incurs most
 * of the work of doing persistence, but discards the results. This is
 * only used for performance testing.
 **/
public class DummyPersistence
  extends PersistencePluginAdapter
  implements PersistencePlugin
{

  public void init(PersistencePluginSupport pps, String name, String[] params)
    throws PersistenceException
  {
    init(pps, name);
  }

  public SequenceNumbers[] readSequenceNumbers(String suffix) {
    return new SequenceNumbers[0];
  }

  public void writeSequenceNumbers(SequenceNumbers sequenceNumbers) {
  }

  public void cleanupOldDeltas(SequenceNumbers cleanupNumbers) {
  }

  public OutputStream openOutputStream(int deltaNumber, boolean full)
    throws IOException
  {
    return null;                // Null means don't write output
  }

  public void finishOutputStream(SequenceNumbers retain,
                                 boolean full)
  {
  }

  public void abortOutputStream(SequenceNumbers retain) {
  }

  public InputStream openInputStream(int deltaNumber) throws IOException {
    throw new IOException("No dummy input");
  }

  public void finishInputStream(int deltaNumber) {
  }

  public void deleteOldPersistence() {
  }

  public void storeDataProtectionKey(int deltaNumber, DataProtectionKey key)
    throws IOException
  {
  }

  public DataProtectionKey retrieveDataProtectionKey(int deltaNumber)
    throws IOException
  {
    return null;
  }
}

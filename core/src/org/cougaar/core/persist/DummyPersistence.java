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

import java.io.*;
import java.util.*;

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

  public ObjectOutputStream openObjectOutputStream(int deltaNumber, boolean full)
    throws IOException
  {
    return new ObjectOutputStream(new ByteArrayOutputStream());
  }

  public void closeObjectOutputStream(SequenceNumbers retain,
                                      ObjectOutputStream currentOutput,
                                      boolean full)
  {
    try {
      currentOutput.close();
    }
    catch (IOException e) {
      pps.getLoggingService().error("Exception closing output stream", e);
    }
  }

  public void abortObjectOutputStream(SequenceNumbers retain, ObjectOutputStream currentOutput) {
    closeObjectOutputStream(retain, currentOutput, false);
  }

  public ObjectInputStream openObjectInputStream(int deltaNumber) throws IOException {
    throw new IOException("No dummy input");
  }

  public void closeObjectInputStream(int deltaNumber, ObjectInputStream currentInput) {
    try {
      currentInput.close();
    }
    catch (IOException e) {
      pps.getLoggingService().error("Exception closing input stream", e);
    }
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

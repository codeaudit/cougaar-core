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

import java.io.*;
import java.util.*;

import org.cougaar.core.cluster.ClusterContext;
import org.cougaar.core.cluster.Envelope;
import org.cougaar.core.cluster.EnvelopeTuple;
import org.cougaar.core.cluster.PersistenceEnvelope;
import org.cougaar.core.cluster.Subscriber;
import org.cougaar.domain.planning.ldm.plan.Plan;

/**
 * This persistence class provides implementation stubs that do
 * nothing. Running with this Persistence implementation incurs most
 * of the work of doing persistence, but discards the results. This is
 * only used for performance testing.
 **/
public class DummyPersistence extends BasePersistence implements Persistence {

  public static Persistence find(final ClusterContext clusterContext)
    throws PersistenceException {
    return BasePersistence.findOrCreate(clusterContext, new PersistenceCreator() {
      public BasePersistence create() throws PersistenceException {
	return new DummyPersistence(clusterContext);
      }
    });
  }

  private DummyPersistence(ClusterContext clusterContext) throws PersistenceException {
    super(clusterContext);
    String clusterName = clusterContext.getClusterIdentifier().getAddress();
  }

  protected SequenceNumbers readSequenceNumbers(String suffix) {
    return null;
  }

  protected void writeSequenceNumbers(SequenceNumbers sequenceNumbers) {
  }

  protected void cleanupOldDeltas(SequenceNumbers cleanupNumbers) {
  }

  protected ObjectOutputStream openObjectOutputStream(int deltaNumber, boolean full)
    throws IOException
  {
    return new ObjectOutputStream(new ByteArrayOutputStream());
  }

  protected void closeObjectOutputStream(SequenceNumbers retain,
                                         ObjectOutputStream currentOutput,
                                         boolean full)
  {
    try {
      currentOutput.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected void abortObjectOutputStream(SequenceNumbers retain, ObjectOutputStream currentOutput) {
    closeObjectOutputStream(retain, currentOutput, false);
  }

  protected ObjectInputStream openObjectInputStream(int deltaNumber) throws IOException {
    throw new IOException("No dummy input");
  }

  protected void closeObjectInputStream(int deltaNumber, ObjectInputStream currentInput) {
    try {
      currentInput.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected void deleteOldPersistence() {
  }

  protected PrintWriter getHistoryWriter(int deltaNumber, String prefix) throws IOException {
    return null;
  }

  public java.sql.Connection getDatabaseConnection(Object locker) {
    throw new UnsupportedOperationException("DummyPersistence.getDatabaseConnection not supported");
  }

  public void releaseDatabaseConnection(Object locker) {
    throw new UnsupportedOperationException("DummyPersistence.releaseDatabaseConnection not supported");
  }
}

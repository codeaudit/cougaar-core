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

  protected SequenceNumbers readSequenceNumbers() {
    return null;
  }

  protected void writeSequenceNumbers(SequenceNumbers sequenceNumbers) {
  }

  protected void cleanupOldDeltas(SequenceNumbers cleanupNumbers) {
  }

  protected ObjectOutputStream openObjectOutputStream(int deltaNumber) throws IOException {
    return new ObjectOutputStream(new ByteArrayOutputStream());
  }

  protected void closeObjectOutputStream(SequenceNumbers retain, ObjectOutputStream currentOutput) {
    try {
      currentOutput.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected void abortObjectOutputStream(SequenceNumbers retain, ObjectOutputStream currentOutput) {
    closeObjectOutputStream(retain, currentOutput);
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
}

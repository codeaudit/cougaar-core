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

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.*;

import org.cougaar.core.cluster.ClusterContext;
import org.cougaar.core.cluster.Envelope;
import org.cougaar.core.cluster.EnvelopeTuple;
import org.cougaar.core.cluster.PersistenceEnvelope;
import org.cougaar.core.cluster.Subscriber;
import org.cougaar.domain.planning.ldm.plan.Plan;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * This persistence class saves plan objects in files. It saves and
 * restores persistence deltas in files.
 */
public class FilePersistence extends BasePersistence implements Persistence {
  static File persistenceRoot;

  static {
    File installDirectory = new File(System.getProperty("org.cougaar.install.path", "/tmp"));
    persistenceRoot = new File(installDirectory, System.getProperty("org.cougaar.core.cluster.persistence.path", "P"));
    if (!persistenceRoot.isDirectory()) {
      if (!persistenceRoot.mkdirs()) {
	System.err.println("Not a directory: " + persistenceRoot);
	System.exit(-1);
      }
    }
  }

  public static Persistence find(final ClusterContext clusterContext)
    throws PersistenceException {
    return BasePersistence.findOrCreate(clusterContext, new PersistenceCreator() {
      public BasePersistence create() throws PersistenceException {
	return new FilePersistence(clusterContext);
      }
    });
  }

  private static class SafeObjectOutputFile extends ObjectOutputStream {
    private FileOutputStream fileOutputStream;

    public SafeObjectOutputFile(FileOutputStream stream) throws IOException {
      super(stream);
      fileOutputStream = stream;
    }

    public void close() throws IOException {
      flush();
      fileOutputStream.flush();
      fileOutputStream.getFD().sync();
      super.close();
    }
  }

  private File persistenceDirectory;

  private File sequenceFile;
  private File newSequenceFile;

  private FilePersistence(ClusterContext clusterContext) throws PersistenceException {
    super(clusterContext);
    String clusterName = clusterContext.getClusterIdentifier().getAddress();
    persistenceDirectory = new File(persistenceRoot, clusterName);
    if (!persistenceDirectory.isDirectory()) {
      if (!persistenceDirectory.mkdirs()) {
	throw new PersistenceException("Not a directory: " + persistenceDirectory);
      }
    }
  }

  private File getSequenceFile() {
    if (sequenceFile == null) {
      sequenceFile = new File(persistenceDirectory, "sequence");
    }
    return sequenceFile;
  }

  private File getNewSequenceFile() {
    if (newSequenceFile == null) {
      newSequenceFile = new File(persistenceDirectory, "newSequence");
    }
    return newSequenceFile;
  }

  protected SequenceNumbers readSequenceNumbers() {
    if (!getSequenceFile().exists() && getNewSequenceFile().exists()) {
      getNewSequenceFile().renameTo(getSequenceFile());
    }
    if (getSequenceFile().exists()) {
      print("Reading " + getSequenceFile());
      try {
	DataInputStream sequenceStream = new DataInputStream(new FileInputStream(getSequenceFile()));
	try {
	  int first = sequenceStream.readInt();
	  int last = sequenceStream.readInt();
	  return new SequenceNumbers(first, last);
	}
	finally {
	  sequenceStream.close();
	}
      }
      catch (IOException e) {
	e.printStackTrace();
      }
    }
    return null;
  }

  private void writeSequenceNumbers(SequenceNumbers sequenceNumbers) {
    try {
      FileOutputStream fileStream = new FileOutputStream(getNewSequenceFile());
      DataOutputStream sequenceStream = new DataOutputStream(fileStream);
      try {
	sequenceStream.writeInt(sequenceNumbers.first);
	sequenceStream.writeInt(sequenceNumbers.current);
      }
      finally {
	sequenceStream.flush();
        fileStream.getFD().sync();
        sequenceStream.close();
	getSequenceFile().delete();
	if (!getNewSequenceFile().renameTo(getSequenceFile())) {
	  System.err.println("Failed to rename " + getNewSequenceFile() + " to " + getSequenceFile());
	}
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void cleanupOldDeltas(SequenceNumbers cleanupNumbers) {
    for (int deltaNumber = cleanupNumbers.first; deltaNumber < cleanupNumbers.current; deltaNumber++) {
      File deltaFile = getDeltaFile(deltaNumber);
      if (!deltaFile.delete()) {
	System.err.println("Failed to delete " + deltaFile);
      }
    }
  }

  protected ObjectOutputStream openObjectOutputStream(int deltaNumber) throws IOException {
    File deltaFile = getDeltaFile(deltaNumber);
    print("Persist to " + deltaFile);
    return new SafeObjectOutputFile(new FileOutputStream(deltaFile));
  }

  protected void closeObjectOutputStream(SequenceNumbers retainNumbers,
                                         ObjectOutputStream currentOutput)
  {
    try {
      currentOutput.close();
      writeSequenceNumbers(retainNumbers);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected void abortObjectOutputStream(SequenceNumbers retainNumbers,
                                         ObjectOutputStream currentOutput)
  {
    try {
      currentOutput.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    getDeltaFile(retainNumbers.current).delete();
  }

  protected ObjectInputStream openObjectInputStream(int deltaNumber) throws IOException {
    File deltaFile = getDeltaFile(deltaNumber);
    print("rehydrate " + deltaFile);
    return new ObjectInputStream(new FileInputStream(deltaFile));
  }

  protected void closeObjectInputStream(int deltaNumber, ObjectInputStream currentInput) {
    try {
      currentInput.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected PrintWriter getHistoryWriter(int deltaNumber, String prefix) throws IOException {
    String seq = "" + (100000+deltaNumber);
    File historyFile = new File(persistenceDirectory, prefix + seq.substring(1));
    return new PrintWriter(new FileWriter(historyFile));
  }

  protected void deleteOldPersistence() {
    File[] files = persistenceDirectory.listFiles();
    for (int i = 0; i < files.length; i++) {
      files[i].delete();
    }
  }

  private File getDeltaFile(int sequence) {
    String seq = "" + (100000+sequence);
    return new File(persistenceDirectory, "delta_" + seq.substring(1));
  }
}

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
 * @property org.cougaar.install.path Used by FilePersistence as the parent directory for persistence
 * snapshots when org.cougaar.core.cluster.persistence.path is a relative pathname.
 * @property org.cougaar.core.cluster.persistence.path Specifies the directory in which persistence
 * snapshots should be saved.  If this is a relative path, it the base will be the value or org.cougaar.install.path.
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

  private File getSequenceFile(String suffix) {
    return new File(persistenceDirectory, "sequence" + suffix);
  }

  private File getNewSequenceFile(String suffix) {
    return new File(persistenceDirectory, "newSequence" + suffix);
  }

  protected SequenceNumbers readSequenceNumbers(String suffix) {
    File sequenceFile = getSequenceFile(suffix);
    File newSequenceFile = getNewSequenceFile(suffix);
    if (!sequenceFile.exists() && newSequenceFile.exists()) {
      newSequenceFile.renameTo(sequenceFile);
    }
    if (sequenceFile.exists()) {
      print("Reading " + sequenceFile);
      try {
	DataInputStream sequenceStream = new DataInputStream(new FileInputStream(sequenceFile));
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

  private void writeSequenceNumbers(SequenceNumbers sequenceNumbers, String suffix) {
    try {
      File sequenceFile = getSequenceFile(suffix);
      File newSequenceFile = getNewSequenceFile(suffix);
      FileOutputStream fileStream = new FileOutputStream(newSequenceFile);
      DataOutputStream sequenceStream = new DataOutputStream(fileStream);
      try {
	sequenceStream.writeInt(sequenceNumbers.first);
	sequenceStream.writeInt(sequenceNumbers.current);
      }
      finally {
	sequenceStream.flush();
        fileStream.getFD().sync();
        sequenceStream.close();
	sequenceFile.delete();
	if (!newSequenceFile.renameTo(sequenceFile)) {
	  System.err.println("Failed to rename " + newSequenceFile + " to " + sequenceFile);
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

  protected ObjectOutputStream openObjectOutputStream(int deltaNumber, boolean full) throws IOException {
    File deltaFile = getDeltaFile(deltaNumber);
    print("Persist to " + deltaFile);
    return new SafeObjectOutputFile(new FileOutputStream(deltaFile));
  }

  protected void closeObjectOutputStream(SequenceNumbers retainNumbers,
                                         ObjectOutputStream currentOutput,
                                         boolean full)
  {
    try {
      currentOutput.close();
      writeSequenceNumbers(retainNumbers, "");
      if (full) writeSequenceNumbers(retainNumbers, formatDeltaNumber(retainNumbers.first));
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
    File historyFile = new File(persistenceDirectory, prefix + formatDeltaNumber(deltaNumber));
    return new PrintWriter(new FileWriter(historyFile));
  }

  protected void deleteOldPersistence() {
    File[] files = persistenceDirectory.listFiles();
    for (int i = 0; i < files.length; i++) {
      files[i].delete();
    }
  }

  private File getDeltaFile(int sequence) {
    return new File(persistenceDirectory, "delta" + formatDeltaNumber(sequence));
  }

  public java.sql.Connection getDatabaseConnection(Object locker) {
    throw new UnsupportedOperationException("FilePersistence.getDatabaseConnection not supported");
  }

  public void releaseDatabaseConnection(Object locker) {
    throw new UnsupportedOperationException("FilePersistence.releaseDatabaseConnection not supported");
  }
}


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

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import org.cougaar.core.adaptivity.OMCRange;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.agent.ClusterContext;
import org.cougaar.core.blackboard.Envelope;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.PersistenceEnvelope;
import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.DataProtectionKeyEnvelope;
import org.cougaar.core.service.DataProtectionKey;
import org.cougaar.planning.ldm.plan.Plan;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * This persistence plugin saves and restores plan objects in
 * files. There is one optional parameter naming the persistence
 * root directory. If the parameter is omitted, the persistence root
 * is specified by system properties.
 * @property org.cougaar.install.path Used by FilePersistence as the
 * parent directory for persistence snapshots when there is no
 * directory specified in configuration parameters and
 * org.cougaar.core.persistence.path is a relative pathname. This
 * property is not used if the plugin is configured with a specific
 * parameter specifying the location of the persistence root.
 * @property org.cougaar.core.persistence.path Specifies the directory
 * in which persistence snapshots should be saved. If this is a
 * relative path, it the base will be the value or
 * org.cougaar.install.path. This property is not used if the plugin
 * is configured with a specific parameter specifying the location of
 * the persistence root.
 **/
public class FilePersistence
  extends PersistencePluginAdapter
  implements PersistencePlugin
{
  /**
   * Wrap a FileOutputStream to prove safe close semantics. Explicitly
   * sync the file descriptor on close() to insure the file has been
   * completely written to the disk.
   **/
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

  private static File getDefaultPersistenceRoot() {
    String installPath = System.getProperty("org.cougaar.install.path", "/tmp");
    File workspaceDirectory =
      new File(System.getProperty("org.cougaar.workspace", installPath + "/workspace"));
    return new File(workspaceDirectory,
                    System.getProperty("org.cougaar.core.persistence.path", "P"));
  }

  private File persistenceDirectory;

  public void init(PersistencePluginSupport pps, String name, String[] params)
    throws PersistenceException
  {
    init(pps, name);
    File persistenceRoot;
    if (params.length < 1) {
      persistenceRoot = getDefaultPersistenceRoot();
    } else {
      persistenceRoot = new File(params[0]);
    }
    if (!persistenceRoot.isDirectory()) {
      if (!persistenceRoot.mkdirs()) {
	pps.getLoggingService().fatal("Not a directory: " + persistenceRoot);
        throw new PersistenceException("Persistence root unavailable");
      }
    }
    String clusterName = pps.getClusterIdentifier().getAddress();
    persistenceDirectory = new File(persistenceRoot, clusterName);
    if (!persistenceDirectory.isDirectory()) {
      if (!persistenceDirectory.mkdirs()) {
        String msg = "FilePersistence(" + name + ") not a directory: " + persistenceDirectory;
	pps.getLoggingService().fatal(msg);
	throw new PersistenceException(msg);
      }
    }
  }

  private File getSequenceFile(String suffix) {
    return new File(persistenceDirectory, "sequence" + suffix);
  }

  private File getNewSequenceFile(String suffix) {
    return new File(persistenceDirectory, "newSequence" + suffix);
  }

  public SequenceNumbers[] readSequenceNumbers(final String suffix) {
    LoggingService ls = pps.getLoggingService();
    FilenameFilter filter;
    if (suffix.equals("")) {
      filter = new FilenameFilter() {
        public boolean accept(File dir, String path) {
          return (path.startsWith("newSequence") || path.startsWith("sequence"));
        }
      };
    } else {
      filter = new FilenameFilter() {
        public boolean accept(File dir, String path) {
          return (path.endsWith(suffix)
                  && (path.startsWith("newSequence") || path.startsWith("sequence")));
        }
      };
    }
    String[] names = persistenceDirectory.list(filter);
    List result = new ArrayList(names.length);
    File sequenceFile;
    for (int i = 0; i < names.length; i++) {
      if (names[i].startsWith("newSequence")) {
        File newSequenceFile = new File(names[i]);
        sequenceFile = new File("sequence" + names[i].substring("newSequence".length()));
        newSequenceFile.renameTo(sequenceFile);
      } else {
        sequenceFile = new File(persistenceDirectory, names[i]);
      }
      if (ls.isDebugEnabled()) {
        ls.debug("Reading " + sequenceFile);
      }
      try {
	DataInputStream sequenceStream = new DataInputStream(new FileInputStream(sequenceFile));
	try {
	  int first = sequenceStream.readInt();
	  int last = sequenceStream.readInt();
          long timestamp = sequenceFile.lastModified();
	  result.add(new SequenceNumbers(first, last, timestamp));
	}
	finally {
	  sequenceStream.close();
	}
      }
      catch (IOException e) {
	ls.error("Error reading " + sequenceFile, e);
      }
    }
    return (SequenceNumbers[]) result.toArray(new SequenceNumbers[result.size()]);
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
	  pps.getLoggingService().error("Failed to rename " + newSequenceFile + " to " + sequenceFile);
	}
      }
    }
    catch (Exception e) {
      pps.getLoggingService().error("Exception writing sequenceFile", e);
    }
  }

  public void cleanupOldDeltas(SequenceNumbers cleanupNumbers) {
    for (int deltaNumber = cleanupNumbers.first; deltaNumber < cleanupNumbers.current; deltaNumber++) {
      File deltaFile = getDeltaFile(deltaNumber);
      if (!deltaFile.delete()) {
	pps.getLoggingService().error("Failed to delete " + deltaFile);
      }
    }
  }

  public ObjectOutputStream openObjectOutputStream(int deltaNumber, boolean full) throws IOException {
    File deltaFile = getDeltaFile(deltaNumber);
    LoggingService ls = pps.getLoggingService();
    if (ls.isDebugEnabled()) {
      ls.debug("Persist to " + deltaFile);
    }
    return new SafeObjectOutputFile(new FileOutputStream(deltaFile));
  }

  public void closeObjectOutputStream(SequenceNumbers retainNumbers,
                                         ObjectOutputStream currentOutput,
                                         boolean full)
  {
    try {
      currentOutput.close();
      writeSequenceNumbers(retainNumbers, "");
      if (full) writeSequenceNumbers(retainNumbers,
                                     BasePersistence.formatDeltaNumber(retainNumbers.first));
    }
    catch (IOException e) {
      pps.getLoggingService().error("Exception closing persistence output stream", e);
    }
  }

  public void abortObjectOutputStream(SequenceNumbers retainNumbers,
                                      ObjectOutputStream currentOutput)
  {
    try {
      currentOutput.close();
    }
    catch (IOException e) {
      pps.getLoggingService().error("Exception aborting persistence output stream", e);
    }
    getDeltaFile(retainNumbers.current).delete();
  }

  public ObjectInputStream openObjectInputStream(int deltaNumber) throws IOException {
    File deltaFile = getDeltaFile(deltaNumber);
    LoggingService ls = pps.getLoggingService();
    if (ls.isDebugEnabled()) {
      ls.debug("rehydrate " + deltaFile);
    }
    return new ObjectInputStream(new FileInputStream(deltaFile));
  }

  public void closeObjectInputStream(int deltaNumber, ObjectInputStream currentInput) {
    try {
      currentInput.close();
    }
    catch (IOException e) {
      pps.getLoggingService().error("Exception closing persistence input stream", e);
    }
  }

  public void deleteOldPersistence() {
    File[] files = persistenceDirectory.listFiles();
    for (int i = 0; i < files.length; i++) {
      files[i].delete();
    }
  }

  public void storeDataProtectionKey(int deltaNumber, DataProtectionKey key)
    throws IOException
  {
    File file = getEncryptedKeyFile(deltaNumber);
    ObjectOutputStream ois = new ObjectOutputStream(new FileOutputStream(file));
    try {
      ois.writeObject(key);
    } finally {
      ois.close();
    }
  }

  public DataProtectionKey retrieveDataProtectionKey(int deltaNumber)
    throws IOException
  {
    File file = getEncryptedKeyFile(deltaNumber);
    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
    try {
      return (DataProtectionKey) ois.readObject();
    } catch (ClassNotFoundException cnfe) {
      IOException ioe = new IOException("Read DataProtectionKey failed");
      ioe.initCause(cnfe);
      throw ioe;
    } finally {
      ois.close();
    }
  }

  private File getDeltaFile(int sequence) {
    return new File(persistenceDirectory,
                    "delta" + BasePersistence.formatDeltaNumber(sequence));
  }

  private File getEncryptedKeyFile(int sequence) {
    return new File(persistenceDirectory,
                    "key" + BasePersistence.formatDeltaNumber(sequence));
  }
}

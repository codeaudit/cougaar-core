
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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.*;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.DataProtectionKeyEnvelope;
import org.cougaar.core.service.DataProtectionKey;

/**
 * This persistence plugin abstract base class saves and restores plan
 * objects in files. The actual opening of the input and output
 * streams remains abstract.
 *
 * <P>There is one optional parameter naming the persistence root
 * directory. If the parameter is omitted, the persistence root is
 * specified by system properties.
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
public abstract class FilePersistenceBase
  extends PersistencePluginAdapter
  implements PersistencePlugin
{
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
      String path = params[0];
      persistenceRoot = new File(path);
    }
    pps.getLoggingService().fatal("Persistence directory: " + persistenceRoot);
    persistenceRoot.mkdirs();
    if (!persistenceRoot.isDirectory()) {
      pps.getLoggingService().fatal("Not a directory: " + persistenceRoot);
      throw new PersistenceException("Persistence root unavailable");
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

  protected abstract InputStream openFileInputStream(File file) throws FileNotFoundException;

  protected abstract OutputStream openFileOutputStream(File file) throws FileNotFoundException;

  protected abstract boolean rename(File from, File to);

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
        rename(newSequenceFile, sequenceFile);
      } else {
        sequenceFile = new File(persistenceDirectory, names[i]);
      }
      if (ls.isDebugEnabled()) {
        ls.debug("Reading " + sequenceFile);
      }
      try {
	DataInputStream sequenceStream = new DataInputStream(openFileInputStream(sequenceFile));
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
      DataOutputStream sequenceStream =
        new DataOutputStream(openFileOutputStream(newSequenceFile));
      try {
	sequenceStream.writeInt(sequenceNumbers.first);
	sequenceStream.writeInt(sequenceNumbers.current);
      }
      finally {
        sequenceStream.close();
	sequenceFile.delete();
	if (!rename(newSequenceFile, sequenceFile)) {
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
    return new ObjectOutputStream(openFileOutputStream(deltaFile));
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
    return new ObjectInputStream(openFileInputStream(deltaFile));
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
    ObjectOutputStream ois = new ObjectOutputStream(openFileOutputStream(file));
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
    ObjectInputStream ois = new ObjectInputStream(openFileInputStream(file));
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

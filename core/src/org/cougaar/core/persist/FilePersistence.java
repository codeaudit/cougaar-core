/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
  extends FilePersistenceBase
  implements PersistencePlugin
{
  /**
   * Wrap a FileOutputStream to prove safe close semantics. Explicitly
   * sync the file descriptor on close() to insure the file has been
   * completely written to the disk.
   **/
  private static class SafeFileOutputStream extends OutputStream {
    private FileOutputStream fileOutputStream;

    public SafeFileOutputStream(File file) throws FileNotFoundException {
      fileOutputStream = new FileOutputStream(file);
    }

    public void write(int b) throws IOException {
      fileOutputStream.write(b);
    }

    public void write(byte[] b)  throws IOException {
      fileOutputStream.write(b, 0, b.length);
    }

    public void write(byte[] b, int offset, int nbytes) throws IOException {
      fileOutputStream.write(b, offset, nbytes);
    }

    public void flush() throws IOException {
      fileOutputStream.flush();
    }

    public void close() throws IOException {
      fileOutputStream.flush();
      fileOutputStream.getFD().sync();
      fileOutputStream.close();
    }
  }

  protected OutputStream openFileOutputStream(File file) throws FileNotFoundException {
    return new SafeFileOutputStream(file);
  }

  protected InputStream openFileInputStream(File file) throws FileNotFoundException {
    return new FileInputStream(file);
  }

  protected boolean rename(File from, File to) {
    return from.renameTo(to);
  }
}

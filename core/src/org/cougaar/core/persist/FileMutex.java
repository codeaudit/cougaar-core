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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class FileMutex {
  private static final SimpleDateFormat uniqueNameFormat;
  static {
    uniqueNameFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    uniqueNameFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  private File uniqueFile;
  private File commonFile;
  private long timeout;

  public FileMutex(File directory, String commonName, long timeout) {
    this.timeout = timeout;
    commonFile = new File(directory, commonName);
    uniqueFile = new File(directory, uniqueNameFormat.format(new Date()));
  }

  public void lock() throws IOException {
    long endTime = System.currentTimeMillis() + timeout;
    new FileOutputStream(uniqueFile).close(); // No content needed
    while (!uniqueFile.renameTo(commonFile)) {
      if (System.currentTimeMillis() > endTime) {
        unlock();               // Unlock the mutex for the
                                // (apparently dead) other instance
        continue;               // Rename should now work
      }
      try {
        Thread.sleep(5000);     // Wait for the other instance to
                                // unlock
      } catch (InterruptedException ie) {
      }
    }
  }

  public void unlock() throws IOException {
    commonFile.delete();
  }

  public static void main(String[] args) {
    String TMP = "/tmp";
    String SEQ = TMP + "/seq";
    String PREFIX = TMP + "/";
    String COMMON = "filemutexlock";
    String SUFFIX = args[0];
    DecimalFormat format = new DecimalFormat("00000");
    FileMutex fm = new FileMutex(new File(TMP), COMMON, 30000L);
    try {
      for (int i = 0; i < 1000; i++) {
        fm.lock();
        int seq;
        try {
          DataInputStream r = new DataInputStream(new FileInputStream(SEQ));
          try {
            seq = r.readInt();
          } finally {
            r.close();
          }
        } catch (IOException ioe) {
          seq = 0;
        }
        String fileName = PREFIX + format.format(seq) + SUFFIX;
        new FileOutputStream(fileName).close();
        DataOutputStream o = new DataOutputStream(new FileOutputStream(SEQ));
        seq++;
        o.writeInt(seq);
        o.close();
        fm.unlock();
        Thread.sleep(100);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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

package org.cougaar.core.node.service.jvmdump;

/**
 * Package-private JNI implementation.
 */
class JniStackDump {

  private static final String LIBRARY_NAME = "jvmdump";

  private static boolean needLibrary = true;
  private static boolean haveLibrary = false;

  /**
   * Load library if necessary, check for loading errors and 
   * disable the dump if the library can't be loaded.
   * @return true if the libary is available.
   **/
  private static boolean checkLibrary() {
    if (needLibrary) {
      try {
        System.loadLibrary(LIBRARY_NAME);
        haveLibrary = true;
      } catch (UnsatisfiedLinkError e) {
        // missing library
      }
      needLibrary = false;
    }
    return haveLibrary;
  }

  // native method declaration
  private static native boolean jvmdump();

  /**
   * Request that the JVM dump its stack to std-out.
   *
   * @return true if the stack was dumped
   */
  public static synchronized boolean dumpStack() {
    return checkLibrary() && jvmdump();
  }

  public static void main(String args[]) {
    System.out.println("Dump JVM stack: ");
    boolean ret = dumpStack();
    System.out.println("response: "+ret);
    if (!ret) {
      try {
        System.loadLibrary(LIBRARY_NAME);
      } catch (UnsatisfiedLinkError e) {
        // print library error
        e.printStackTrace();
      }
    }
  }
}

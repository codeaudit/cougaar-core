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
package org.cougaar.util;


/**
*   Imports
**/
import java.io.*;
import java.net.*;
import java.util.Vector;
import java.util.Stack;
import org.w3c.dom.Document;

/**
 * ConfigFileFinder is a convenient class of static methods which 
 * simply dispatch to the singleton default instance of 
 * ConfigFinder.getInstance().  This provides backwards compatability
 * for old code as well as the convenience.  However, new code should
 * use the ConfigFinder instance provided by the Cluster for forward 
 * compatability.
 * @deprecated ALP6.4: Use cluster.getConfigFinder()
 **/
public class ConfigFileFinder {
  /**
   * Locate an actual file in the config path. This will skip over
   * elements of org.cougaar.config.path that are not file: urls.
   * @deprecated ALP6.4: Use cluster.getConfigFinder().locateFile();
   **/
  public static File locateFile(String aFilename) {
    return ConfigFinder.getInstance().locateFile(aFilename);
  }

  /**
   * Opens an InputStream to access the named file. The file is sought
   * in all the places specified in configPath.
   * @throws IOException if the resource cannot be found.
   * @deprecated ALP6.4: Use cluster.getConfigFinder().open();
   **/
  public static InputStream open(String aURL) throws IOException {
    return ConfigFinder.getInstance().open(aURL);
  }

  /**
   * @deprecated ALP6.4: Use cluster.getConfigFinder().openZip();
   */
  public static InputStream openZip(String aURL, String aZIP) throws IOException {
    return ConfigFinder.getInstance().openZip(aURL, aZIP);
  }

  /**
   * @deprecated ALP6.4: Use cluster.getConfigFinder().parseXMLConfigFile();  
   */
  public static Document parseXMLConfigFile(String xmlfile) throws IOException {
    return ConfigFinder.getInstance().parseXMLConfigFile(xmlfile);
  }
}


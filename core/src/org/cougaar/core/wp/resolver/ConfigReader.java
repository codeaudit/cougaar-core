/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp.resolver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

/**
 * This utility class reads the bootstrap configuration for later
 * use in the client's cache and server.
 * <p>
 * This class reads the lines in the file "alpreg.ini", the system
 * property "-Dorg.cougaar.name.server=<i>VALUE</i>" which is
 * parsed as the line "WP=<i>VALUE</i>", and all system properties
 * that match "-Dorg.cougaar.name.server.<i>NAME</i>=<i>VALUE</i>
 * which are parsed as lines "<i>NAME</i>=<i>VALUE</i>.  Lines
 * starting with "#", "[", or "//" are ignored.
 * <p>
 * Typically either the "alpreg.ini" file or system properties
 * will specify a single "host:port" of a node that's running the
 * white pages server, e.g.:<pre>
 *   -Dorg.cougaar.name.server=foo.com:1234
 * </pre>  This will create a local white pages cache hint for the
 * "WP" entry and activates the local RMI bootstrap resolver:<pre>
 *   {@link org.cougaar.core.wp.resolver.rmi.RMIBootstrapLookup}
 * </pre> to contact the RMI registry on "foo.com:1324" and find
 * the RMI stub.  If this is the local host then the RMI bootstrap
 * resolver watches for local MTS binds and advertises the local
 * node as the white pages server.  An alias named "WP" will be added
 * to the local white pages cache, which tells the local white pages
 * client resolver where to find the white pages.
 * <p>
 * Another useful line format is:<pre>
 *   <i>NAME</i>=<i>AGENT</i>@<i>HOST</i>:<i>PORT</i>
 * </pre> which tells the RMI bootstrap resolver to look for a
 * specific agent on the host instead of finding the first that
 * raced to bind in the RMI registry.
 * <p>
 * The name "WP" and names starting with "WP-" have special meaning
 * to the ClientTransport, since these identify white pages servers.
 * See {@link ClientTransport} for additional details.
 * 
 * @see #parse detailed parsing notes
 */
public class ConfigReader {

  private static final String FILENAME = "alpreg.ini";
  private static final String SERVER_PROP = "org.cougaar.name.server";
  private static final String PROP_PREFIX = SERVER_PROP+".";

  private static final Object lock = new Object();

  private static List entries;

  // these are only used if "entries" is null
  private static Logger logger; 
  private static String host;
  private static int port;

  public static List listEntries() {
    synchronized (lock) {
      ensureLoad();
      return entries;
    }
  }

  private static void ensureLoad() {
    if (entries == null) {
      entries = new ArrayList();
      readConfig();
      entries = 
        (entries.isEmpty() ?
         Collections.EMPTY_LIST :
         Collections.unmodifiableList(entries));
    }
  }

  private static void readConfig() {
    logger = LoggerFactory.getInstance().createLogger(
        ConfigReader.class);
    try {
      read_file(FILENAME);
      read_props();
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error("Failed readConfig", e);
      }
    }
    if (logger.isInfoEnabled()) {
      logger.info("Read config: "+entries);
    }
    logger = null;
  }

  private static void add(String line) {
    AddressEntry ae = parse(line);
    if (ae == null) {
      return;
    }
    // find matching entry (if any)
    int overwrite = -1;
    String name = ae.getName();
    String type = ae.getType();
    for (int i = 0, n = entries.size(); i < n; i++) {
      AddressEntry ai = (AddressEntry) entries.get(i);
      if (name.equals(ai.getName()) &&
          type.equals(ai.getType())) {
        overwrite = i;
        break;
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Adding "+ae+
          (0 <= overwrite ?
           ", which overwrites "+entries.get(overwrite) :
           ""));
    }
    if (0 <= overwrite) {
      entries.remove(overwrite);
    }
    entries.add(ae);
  }

  /**
   * Parse a line of input into an AddressEntry.
   * <p>
   * Lines starting with "#", "[", or "//" are ignored.
   * <p>
   * The standard line formats are:<pre>
   *   1) <i>NAME</i>=<i>HOST</i>:<i>PORT</i>
   *   2) <i>NAME</i>=<i>AGENT</i>@<i>HOST</i>:<i>PORT</i>
   * </pre> which turn into these white pages hints:<pre>
   *   1&gt; (name=<i>NAME</i> type=-RMI_REG uri=rmi://<i>HOST</i>:<i>PORT</i>/*)
   *   2&gt; (name=<i>NAME</i> type=-RMI_REG uri=rmi://<i>HOST</i>:<i>PORT</i>/<i>AGENT</i>)
   * </pre> plus these backwards-compatible lines:<pre>
   *   3) address=<i>HOST</i>
   *   4) port=<i>PORT</i>
   * </pre> which are combined to form the equivalent of:<pre>
   *   3+4) WP=<i>HOST</i>:<i>PORT</i>
   * </pre><p>
   * An additional line format is:<pre>
   *   5) <i>NAME</i>=<i>AGENT</i>@
   * </pre> which is parsed as:<pre>
   *   5&gt; (name=<i>NAME</i> type=alias uri=name:///<i>AGENT</i>)
   * </pre>.
   * <p>
   * Lastly, this format is supported:<pre>
   *   6) <i>NAME</i>=<i>TYPE</i>[:,]<i>SCHEME</i>://<i>URI_INFO</i>
   * </pre> which is parsed as:<pre>
   *   6&gt; (name=<i>NAME</i> type=<i>TYPE</i> uri=<i>SCHEME</i>://<i>URI_INFO</i>)
   * </pre>.  This allows the user to specify a full AddressEntry.
   * <p>
   * For backwards compatibility, the following names are
   * reserved:<pre>
   *    address, port, hostname, alias, lpsport</pre>
   * Also, if the line ends in ":5555", then this is trimmed off,
   * since this is the old PSP server port.
   */
  private static AddressEntry parse(String line) {
    if (line.startsWith("#") ||
        line.startsWith("[") ||
        line.startsWith("//")) {
      return null;
    }
    int index = line.indexOf('=');
    if (index <= 0) {
      return null;
    }
    String name = line.substring(0, index).trim();
    String value = line.substring(index + 1).trim();
    if ("address".equals(name) ||
        "hostname".equals(name)) {
      // save for later
      host = value;
      return null;
    }
    if ("port".equals(name)) {
      // save for later
      port = Integer.parseInt(value);
      return null;
    } 
    if ("alias".equals(name) ||
        "lpsport".equals(name)) {
      // ignore
      return null;
    }
    // trim off the old psp server port
    if (value.endsWith(":5555")) {
      value = value.substring(0, value.length()-5); 
    }
    // parse
    int atIdx = value.indexOf('@');
    int comIdx = value.indexOf(',');
    int colIdx = value.indexOf(':');
    int col2Idx;
    if (0 <= comIdx && comIdx < colIdx) {
      col2Idx = colIdx;
      colIdx = comIdx;
    } else {
      col2Idx =
        (colIdx < 0 ?
         -1 :
         value.indexOf(':', colIdx+1));
    }
    String type;
    String suri;
    if (0 < col2Idx) {
      type = value.substring(0, colIdx);
      suri = value.substring(colIdx+1);
    } else if (
        0 < atIdx &&
        atIdx == value.length()-1) {
      type = "alias";
      suri = "name:///"+value.substring(0, atIdx);
    } else {
      type = "-RMI_REG";
      String agent;
      String hostport;
      if (atIdx < 0) {
        agent = "*";
        hostport = value;
      } else {
        agent = value.substring(0, atIdx);
        hostport = value.substring(atIdx+1);
      }
      suri = "rmi://"+hostport+"/"+agent;
    }
    URI uri = URI.create(suri);
    AddressEntry ae = AddressEntry.getAddressEntry(
        name, type, uri);
    return ae;
  }

  private static void read_file(String filename) {
    try {
      InputStream fs = ConfigFinder.getInstance().open(filename);
      if (fs != null) {
        BufferedReader in = 
          new BufferedReader(new InputStreamReader(fs));
        String line = null;
        while ((line = in.readLine()) != null) {
          add(line);
        }
        in.close();
        if (host != null && 0 < port) {
          // backwards compatibility!
          add("WP="+host+":"+port);
        }
        host = null;
        port = -1;
      }
    } catch (FileNotFoundException fnfe) {
    } catch(Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error("Failed read of "+filename, e);
      }
    }
  }

  private static void read_props() {
    String server = System.getProperty(SERVER_PROP);
    if (server != null) {
      add("WP="+server);
    }

    Properties props =
      SystemProperties.getSystemPropertiesWithPrefix(
          PROP_PREFIX);
    for (Enumeration en = props.propertyNames();
        en.hasMoreElements();
        ) {
      String key = (String) en.nextElement();
      String name = key.substring(PROP_PREFIX.length());
      String value = props.getProperty(key);
      String line = name+"="+value;
      add(line);
    }

    if (host != null && 0 < port) {
      // backwards compatibility!
      add("WP="+host+":"+port);
    }
    host = null;
    port = -1;
  }
}

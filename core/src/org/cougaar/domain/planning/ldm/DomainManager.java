/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm;

import java.util.*;
import java.io.*;

import org.cougaar.util.ConfigFinder;

/**
 * Manage a per-VM set of Domain instances.
 *
 * The set of domains loaded is based on the System properties
 * starting with org.cougaar.domain.  For example, the "foo" domain would
 * be initialized on demand from the value of the system property
 * "org.cougaar.domain.foo=com.FooCo.org.cougaar.FooDomain"
 *
 * Also will attempt to load domains from a config file "LDMDomains.ini".
 * This file has lines of the form (matching the above example):
 *   foo=com.FooCo.org.cougaar.FooDomain
 **/

public final class DomainManager
{
  private final static String PREFIX = "org.cougaar.domain.";
  private final static int PREFIXLENGTH = PREFIX.length();

  private final static boolean verbose = "true".equals(System.getProperty("org.cougaar.verbose","false"));

  /** Should not be constructed **/
  private DomainManager() {}

  /** a map of domainName to Domain Instance **/
  private static final HashMap domains = new HashMap(11); // small is fine

  /** have the domains been initialized? **/
  private static boolean isInitialized = false;

  /** Find a domain instance by name.
   * Looks in System property "org.cougaar.domain.domainName" for the 
   * class to instantiate.  As a special case, the "root" domain
   * always maps to "org.cougaar.domain.planning.ldm.RootDomain"
   **/
  public static Domain find(Object key) {
    synchronized (domains) {
      return (Domain) domains.get(key);
    }
  }

  /** Set up a Domain from the argument strings.
   * @param domainName the name to register the domain under.
   * @param className the name of the class to instantiate as the domain.
   **/
  private static void setup(String domainName, String className) {
    if (domains.get(domainName) != null) {
      System.err.println("Domain \""+domainName+"\" multiply defined!");
      return;
    }

    // we do not synchronize because it is only called from initialize()
    // which is synchronized...
    try {
      Class domainClass = Class.forName(className);
      Domain d = (Domain) domainClass.newInstance();
      
      d.initialize();         // initialize the new domain instance

      domains.put(domainName.intern(), d);
      //System.err.println("Loaded Domain \""+domainName+"\" as "+d);
      keylist = null;
      if (verbose) System.err.println("Initialized LDM Domain \""+domainName+"\".");
    } catch (Exception e) {
      System.err.println("Could not construct Domain \""+domainName+"\"");
      //e.printStackTrace();
    }
  }

  private static void initializeFromProperties() {
    Properties props = System.getProperties();
    for (Enumeration names = props.propertyNames(); names.hasMoreElements(); ) {
      String key = (String) names.nextElement();
      if (key.startsWith(PREFIX)) {
        String name = key.substring(PREFIXLENGTH);
        String value = props.getProperty(key);
        setup(name, value);
      }
    }
  }
  
  private static void initializeFromConfigFiles() {
    try {
      InputStream in = org.cougaar.util.ConfigFinder.getInstance().open("LDMDomains.ini");
      InputStreamReader isr = new InputStreamReader(in);
      BufferedReader br = new BufferedReader(isr);

      String line;
      int lc = 0;
      for (line = br.readLine(); line != null; line=br.readLine()) {
        lc++;
        line = line.trim();
        if (line.length() == 0) continue;
        char c;
        if ( (c = line.charAt(0)) == ';' || c == '#' ) {
          continue;
        }
        int l = line.indexOf('=');
        if (l == -1) {
          System.err.println("LDMDomains.ini syntax error: line "+lc);
          continue;
        }
        String name = line.substring(0,l).trim();
        String val = line.substring(l+1).trim();
        if (name.length()==0 || val.length()==0) {
          System.err.println("LDMDomains.ini syntax error: line "+lc);
          continue;
        }
        setup(name, val);
      }
    } catch (Exception ex) {
      if (! (ex instanceof FileNotFoundException)) {
        System.err.println("LDMDomains.ini exception: "+ex);
        ex.printStackTrace();
      }
    }
  }

  /** initialize the domain set from system properties **/
  public static void initialize() {
    synchronized (domains) {
      if (isInitialized) return;
      isInitialized = true;

      setup("root", "org.cougaar.domain.planning.ldm.RootDomain"); // setup the root domain
      initializeFromProperties();
      initializeFromConfigFiles();
    }
  }

  public static Collection values() {
    initialize();
    return domains.values();
  }

  private static Collection keylist = null;

  public static Collection keySet() {
    synchronized (domains) {
      if (keylist != null) return keylist;

      initialize();
      ArrayList l = new ArrayList(11);
      for (Iterator i = domains.keySet().iterator(); i.hasNext(); ) {
        l.add(i.next());
      }
      keylist = l;
      return l;
    }
  }
}
      

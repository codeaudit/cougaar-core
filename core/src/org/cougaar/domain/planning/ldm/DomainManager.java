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

package org.cougaar.domain.planning.ldm;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;

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

  /** a map of domainName aliases to primary domain name.
   * Access is guarded by synchronizing on <em>domains</em>
   **/
  private static final HashMap aliases = new HashMap(11);

  /** have the domains been initialized? **/
  private static boolean isInitialized = false;

  /** Find a domain instance by name.
   * Looks in System property "org.cougaar.domain.domainName" for the 
   * class to instantiate.  As a special case, the "root" domain
   * always maps to "org.cougaar.domain.planning.ldm.RootDomain"
   **/

  public static Domain find(Object key) {
    synchronized (domains) {
      Domain d = (Domain) domains.get(key);
      if (d != null) return d;

      String primary = (String) aliases.get(key);
      if (primary != null) {
        System.err.println("Warning: use of domain alias \""+key+"\" for \""+primary+"\".");
        return (Domain) domains.get(primary);
      } 
      return null;
    }
  }

  /** Set up a Domain from the argument strings.
   * Must be called in a context where domains is synchronized.
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

      domainName = domainName.intern();
      domains.put(domainName, d);
      //System.err.println("Loaded Domain \""+domainName+"\" as "+d);

      // look for Collection d.getAliases();
      try {
        Method m = domainClass.getMethod("getAliases", null);
        Object r = m.invoke(d, null);
        if (r != null && r instanceof Collection) {
          for (Iterator i = ((Collection)r).iterator(); i.hasNext(); ) {
            String alias = (String) i.next(); // thrown classcastexception if bad
            if (aliases.get(alias) != null) {
              System.err.println("Warning: Domain "+domainClass+" attempting to re-alias "+alias);
            } else {
              aliases.put(alias, domainName);
            }
          }
        }
      } catch (NoSuchMethodException nsme) {
        // ok - no aliases
      } catch (Exception othere) {
        System.err.println("Warning: domain analysis for aliases raised: "+othere);
      }

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
        // domain names have no extra "." characters, so we can 
        // use -D arguments to control domain-related facilities.
        if (name.indexOf('.')<0) {
          String value = props.getProperty(key);
          setup(name, value);
        }
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
      

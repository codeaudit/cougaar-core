/*
 * <copyright>
 *  Copyright 1997-2001 Networks Associates Technology, Inc.
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
 * Created on September 12, 2001, 10:55 AM
 */

package org.cougaar.core.security.bootstrap;

import java.util.Properties;
import java.lang.SecurityManager;
import java.util.Enumeration;
import java.security.PrivilegedAction;
import java.security.AccessController;

/** This utility class allows to retrieve system properties without
    requiring write access (which would be a security vulnerability).
    Only the bootstrapper has the permission to call System.getProperties().
    Warning: in fact, no class should need the entire set of properties.
    Classes should only use specific properties that they need. 
    Several properties would create a security threat if they could be read. */
public class SystemProperties {

  private static boolean debug = false;

  /* Make a copy of all system properties. This gives read access to
     system properties while preventing arbitrary code to write these
     properties. This is in fact a private method because allowing read-access
     to system properties creates a security threat. */
  private static Properties getSystemProperties() {
    Properties props;
    // Make a copy of the properties.
    props = new Properties(System.getProperties());

    System.out.println("system class path:" + System.getProperty("java.class.path"));
    props.setProperty("java.class.path", "foo");
    System.out.println("props class path:" + props.getProperty("java.class.path"));
    System.out.println("system class path after:" + System.getProperty("java.class.path"));
    return props;
  }

  /** Returns standard properies that can be read by anyone.
  */
  public static Properties getStandardSystemProperties() {
    String[] propname = {
      "java.version",
      "java.vendor",
      "java.vendor.url",
      "java.class.version",
      "os.name",
      "os.version",
      "os.arch",
      "file.separator",
      "path.separator",
      "line.separator",
      "java.specification.version",
      "java.specification.vendor",
      "java.specification.name",
      "java.vm.specification.version",
      "java.vm.specification.vendor",
      "java.vm.specification.name",
      "java.vm.version",
      "java.vm.vendor",
      "java.vm.name"
    };

    Properties props = new Properties();
    for (int i = 0 ; i < propname.length ; i++) {
      // Make a copy of the system properties.
      props.setProperty(propname[i], System.getProperty(propname[i]));
    }
    return props;
  }

  public static Properties getSystemPropertiesWithPrefix(String prefix) {
    Properties props = new Properties();
    if (debug) {
      System.out.println("getSystemPropertiesWithPrefix: " + prefix);
    }

    /*
      // disable to allow it to function with NAI SecurityManager 
    Enumeration names = (Enumeration) AccessController.doPrivileged(new PrivilegedAction() {
	public Object run() {
	  Enumeration n = System.getProperties().propertyNames();
	  return n;
	}
      });
    */
    Enumeration names = System.getProperties().propertyNames();

    while (names.hasMoreElements()) {
      String key = (String) names.nextElement();
      if (key.startsWith(prefix)) {
	if (debug) {
	  System.out.println("Trying to read property: " + key);
	}

	try {
	  // Make sure this property can be read. Check against the security policy.
	  // The following line will throw a security exception if the thread
	  // does not have the permission to read that property.
	  System.getProperty(key);

	  props.setProperty(key, System.getProperty(key));
	} catch (SecurityException e) {
	  // Don't add the property if we cannot read it.
	  if (debug) {
	    System.out.println("Not allowed to read property: " + key);
	  }
	}
      }
    }
    //props.list(System.out);
    return props;
  } 
}

/*
 * <copyright>
 *  Copyright 2000-2001 BBNT Solutions, LLC
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

package org.cougaar.core.servlet;

import java.util.*;
import org.cougaar.core.service.NamingService;

/**
 * Utility class to lookup all agent names.
 */
public abstract class NSUtil {

  private NSUtil() {}

  public static List getAllEncodedAgentNames(
      NamingService ns) {
    return getAllEncodedAgentNames(ns, new ArrayList());
  }

  public static List getAllEncodedAgentNames(
      NamingService ns, List toList) {
    toList.clear();

    // FIXME replace with WhitePagesService!!!
    try {
      javax.naming.directory.DirContext d = 
        ns.getRootContext(); 
      d = (javax.naming.directory.DirContext) d.lookup("WP");
      javax.naming.NamingEnumeration en = d.listBindings("");
      Set set = new HashSet();
      while (en.hasMoreElements()) {
        javax.naming.Binding binding =  
          (javax.naming.Binding) en.nextElement();
        org.cougaar.core.service.wp.AddressEntry ae = 
          (org.cougaar.core.service.wp.AddressEntry) 
          binding.getObject();
        String name = java.net.URLEncoder.encode(ae.getName());
        set.add(name);
      }
      toList.addAll(set);
      Collections.sort(toList);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }

    return toList;
  }
}

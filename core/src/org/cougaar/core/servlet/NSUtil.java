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

    // FIXME abstract away this naming-service use!
    //
    // here we find all Servlet-server names, which
    // (for now) are the encoded Agent names.  this is 
    // equal-to or a subset-of the list of all Agent names.
    //
    // should add a "ServerQueryService" to provide
    // this lookup, plus a flag in this registry
    // to distinguish between Agent and other (future)
    // root names (e.g. Node-level Servlets, etc).
    // additionally should consider caching...
    //
    // for now this is tolerable:
    try {
      javax.naming.directory.DirContext d = 
        ns.getRootContext(); 
      d = (javax.naming.directory.DirContext) 
        d.lookup("WEBSERVERS");
      javax.naming.NamingEnumeration en = d.listBindings("");
      while (en.hasMoreElements()) {
        javax.naming.Binding binding =  
          (javax.naming.Binding) en.nextElement();
        //
        // This strangeness keeps us from having a dependency on 
        // the "webserver" package, which is compiled after core.jar
        //
        Object o = binding.getObject();
        java.lang.reflect.Method m = o.getClass().getMethod("getName", new Class[0]);
        toList.add(m.invoke(o, new Object[0]));
	/*
        // This is what the code above should be....
        org.cougaar.lib.web.arch.root.GlobalEntry entry = 
          (org.cougaar.lib.web.arch.root.GlobalEntry)binding.getObject();
        toList.add(entry.getName());
	*/
        
      }
      Collections.sort(toList);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }

    return toList;
  }
}

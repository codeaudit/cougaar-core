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

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.NamingService;

/**
 * This is a generic API, provided by "SimpleServletComponent",
 * that allows a Servlet to access COUGAAR Services.
 * <p>
 * The SimpleServletComponent class passes an instance of 
 * SimpleServletSupport to a Servlet's constructor.  This API
 * abstracts away the Component and Service details from the
 * Servlet.
 */
public class SimpleServletSupportImpl
implements SimpleServletSupport 
{
  protected String path;
  protected ClusterIdentifier agentId;
  protected BlackboardService blackboard;
  protected NamingService ns;

  protected String encAgentName;

  public SimpleServletSupportImpl(
      String path,
      ClusterIdentifier agentId,
      BlackboardService blackboard,
      NamingService ns) {
    this.path = path;
    this.agentId = agentId;
    this.blackboard = blackboard;
    this.ns = ns;
    // cache:
    encAgentName = encodeAgentName(agentId.getAddress());
  }

  public String getPath() {
    return path;
  }

  public Collection queryBlackboard(UnaryPredicate pred) {
    // standard stuff here:
    Collection col;
    try {
      blackboard.openTransaction();
      col = blackboard.query(pred);
    } finally {
      blackboard.closeTransaction(false);
    }
    return col;
  }

  public String getEncodedAgentName() {
    return encAgentName;
  }

  public ClusterIdentifier getAgentIdentifier() {
    return agentId;
  }

  public List getAllEncodedAgentNames() {
    return getAllEncodedAgentNames(new ArrayList());
  }

  public List getAllEncodedAgentNames(List toList) {
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
      javax.naming.NamingEnumeration en = d.list("");
      while (en.hasMoreElements()) {
        javax.naming.NameClassPair ncp = 
          (javax.naming.NameClassPair) en.nextElement();
        toList.add(ncp.getName());
      }
      Collections.sort(toList);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }

    return toList;
  }

  // maybe add a "getAllAgentIdentifiers()"

  public String encodeAgentName(String name) {
    return URLEncoder.encode(name);
  }

  // etc to match "SimpleServletSupport"
}

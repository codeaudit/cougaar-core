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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.util.UnaryPredicate;

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
  protected MessageAddress agentId;
  protected BlackboardQueryService blackboardQuery;
  protected NamingService ns;
  protected LoggingService log;

  protected String encAgentName;

  public SimpleServletSupportImpl(
      String path,
      MessageAddress agentId,
      BlackboardQueryService blackboardQuery,
      NamingService ns) {
    this(path, agentId, blackboardQuery, ns, null);
  }

  public SimpleServletSupportImpl(
      String path,
      MessageAddress agentId,
      BlackboardQueryService blackboardQuery,
      NamingService ns,
      LoggingService log) {
    this.path = path;
    this.agentId = agentId;
    this.blackboardQuery = blackboardQuery;
    this.ns = ns;
    this.log = 
      ((log != null) ? log :  LoggingService.NULL);
    // cache:
    encAgentName = encodeAgentName(agentId.getAddress());
  }

  public String getPath() {
    return path;
  }

  public Collection queryBlackboard(UnaryPredicate pred) {
    return blackboardQuery.query(pred);
  }

  public String getEncodedAgentName() {
    return encAgentName;
  }

  public MessageAddress getAgentIdentifier() {
    return agentId;
  }

  public LoggingService getLog() {
    return log;
  }

  public List getAllEncodedAgentNames() {
    return NSUtil.getAllEncodedAgentNames(ns);
  }

  public List getAllEncodedAgentNames(List toList) {
    return NSUtil.getAllEncodedAgentNames(ns, toList);
  }

  // maybe add a "getAllAgentIdentifiers()"

  public String encodeAgentName(String name) {
    try {
      return URLEncoder.encode(name, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      // should never happen
      throw new RuntimeException("Unable to encode to UTF-8?");
    }
  }

  // etc to match "SimpleServletSupport"
}

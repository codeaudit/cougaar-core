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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.*;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.GenericStateModelAdapter;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.component.*;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.servlet.ServletService;

import javax.servlet.Servlet;

/**
 * Component that loads a <code>Servlet</code> and provides
 * the <code>SimpleServletSupport</code>.
 * <p>
 * Usage in an ".ini" file is:<pre>
 *   ...
 *   plugin = &lt;this-class&gt;(&lt;servlet-class&gt;, &lt;path&gt;)
 *   ...</pre><br>
 * where<pre>
 *   &lt;this-class&gt;
 *      is "org.cougaar.core.servlet.SimpleServletComponent"
 *   &lt;servlet-class&gt;
 *      is the class name of a Servlet.
 *      The servlet must have a zero-argument constructor.
 *      If the Servlet has a
 *        public void setSimpleServletSupport(SimpleServletSupport support)
 *      method then a SimpleServletSupport is passed, which provides
 *      <i>limited</i> access to Cougaar internals.
 *   &lt;path&gt;
 *      is the path for the Servlet, such as "/test".
 * </pre><br>
 * <p>
 *
 * Most of this code is "reflection-glue" to:<ul>
 *   <li>capture the (classname, path) parameters</li>
 *   <li>construct the class instance</li>
 *   <li>examine the class's method(s)</li>
 *   <li>setup and create a <code>SimpleServletSupportImpl</code>
 *       instance</li>
 * </ul>
 * Most subclass developers have the classname and path hard-coded,
 * so they should consider not extending SimpleServletComponent and
 * instead use <code>BaseServletComponent</code>.  The additional
 * benefit is that subclasses of BaseServletComponent have full
 * access to the ServiceBroker.
 *
 * @see SimpleServletSupport
 */
public class SimpleServletComponent
extends BaseServletComponent
{

  /**
   * Servlet classname from "setParameter(..)".
   */
  protected String classname;

  /**
   * Servlet path from "setParameter(..)".
   */
  protected String path;

  /**
   * Agent identifier for the Agent that loaded this Component.
   */
  protected ClusterIdentifier agentId;

  // servlet that we'll load
  protected Servlet servlet;

  //
  // Services for our SimpleServletSupport use
  //
  protected BlackboardQueryService blackboardQuery;
  protected NamingService ns;
  protected LoggingService log;


  /**
   * Standard constructor.
   */
  public SimpleServletComponent() {
    super();
  }

  /**
   * Save our binding info during initialization.
   */
  public void setBindingSite(BindingSite bindingSite) {
    super.setBindingSite(bindingSite);

    // kludge until a "AgentIdentificationService" is created
    if (bindingSite instanceof org.cougaar.core.plugin.PluginBindingSite) {
      org.cougaar.core.plugin.PluginBindingSite pbs =
        (org.cougaar.core.plugin.PluginBindingSite) bindingSite;
      this.agentId = pbs.getAgentIdentifier();
    } else {
      throw new RuntimeException(
          "Unable to get AgentId from bindingSite: "+bindingSite);
    }
  }

  /**
   * Save our Servlet's configurable path, for example
   * "/test".
   * <p>
   * This is only set during initialization and is constant
   * for the lifetime of the Servlet.
   */
  public void setParameter(Object o) {
    // expecting a List of [String, String]
    if (!(o instanceof List)) {
      throw new IllegalArgumentException(
        "Expecting a List parameter, not : "+
        ((o != null) ? o.getClass().getName() : "null"));
    }
    List l = (List)o;
    if (l.size() != 2) {
      throw new IllegalArgumentException(
          "Expecting a List with two elements,"+
          " \"classname\" and \"path\", not "+l.size());
    }
    Object o1 = l.get(0);
    Object o2 = l.get(1);
    if ((!(o1 instanceof String)) ||
        (!(o2 instanceof String))) {
      throw new IllegalArgumentException(
          "Expecting two Strings, not ("+o1+", "+o2+")");
    }

    // save the servlet classname and path
    this.classname = (String) o1;
    this.path = (String) o2;
  }

  protected String getPath() {
    return path;
  }

  protected Servlet createServlet() {
    // load the servlet class
    Class cl;
    try {
      cl = Class.forName(classname);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to load Servlet class: "+classname);
    }
    if (!(Servlet.class.isAssignableFrom(cl))) {
      throw new IllegalArgumentException(
          "Class \""+classname+"\" does not implement \""+
          Servlet.class.getName()+"\"");
    }

    // create a zero-arg instance
    try {
      this.servlet = (Servlet) cl.newInstance();
    } catch (Exception e) {
      // check for bug 1073 (deprecated servlet constructor)
      boolean hasDeprecatedConstructor = false;
      try {
        Constructor cons = cl.getConstructor(
            new Class[]{SimpleServletSupport.class});
        hasDeprecatedConstructor = true;
      } catch (Exception e2) {
        // ignore
      }
      if (hasDeprecatedConstructor) {
        // throw a specific "bug 1073" exception
        throw new RuntimeException(
            "Unsupported servlet constructor method \""+
            classname+"(SimpleServletSupport ..)\";"+
            " see bug 1073");
      }
      // throw the general "no-constructor" exception
      throw new RuntimeException(
          "Unable to create Servlet instance: ", e);
    }

    // check for the support requirement
    Method supportMethod;
    try {
      supportMethod = cl.getMethod(
          "setSimpleServletSupport",
          new Class[]{SimpleServletSupport.class});
    } catch (NoSuchMethodException e) {
      // simple non-cougaar-aware servlet
      return servlet;
    }

    // create the support
    SimpleServletSupport support;
    try {
      support = createSimpleServletSupport(servlet);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to create Servlet support", e);
    }

    // set the support
    try {
      supportMethod.invoke(servlet, new Object[]{support});
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to set Servlet support", e);
    }

    return servlet;
  }

  /**
   * Obtain services for the servlet, using the servlet as
   * the requestor.
   */
  protected SimpleServletSupport createSimpleServletSupport(
      Servlet servlet) {
    // the agentId is known from "setBindingSite(..)"

    // get the blackboard query service
    blackboardQuery = (BlackboardQueryService)
      serviceBroker.getService(
          servlet,
          BlackboardQueryService.class,
          null);
    if (blackboardQuery == null) {
      throw new RuntimeException(
          "Unable to obtain blackboard query service");
    }

    // get the naming service (for "listAgentNames")
    ns = (NamingService)
      serviceBroker.getService(
          servlet,
          NamingService.class,
          null);
    if (ns == null) {
      throw new RuntimeException(
          "Unable to obtain naming service");
    }

    // get the logging service (for "getLogger")
    log = (LoggingService)
      serviceBroker.getService(
          servlet,
          LoggingService.class,
          null);
    if (log == null) {
      // continue loading -- let the "support" use a null-logger.
    }

    // create a new "SimpleServletSupport" instance
    return
      new SimpleServletSupportImpl(
        path, agentId, blackboardQuery, ns, log);
  }

  public void unload() {
    // release all services
    super.unload();

    if (servlet != null) {
      if (log != null) {
        serviceBroker.releaseService(
            servlet, LoggingService.class, log);
        log = null;
      }
      if (ns != null) {
        serviceBroker.releaseService(
            servlet, NamingService.class, ns);
        ns = null;
      }
      if (blackboardQuery != null) {
        serviceBroker.releaseService(
            servlet, BlackboardQueryService.class, blackboardQuery);
        blackboardQuery = null;
      }
      servlet = null;
    }
  }

  public String toString() {
    return classname+"("+path+")";
  }
}

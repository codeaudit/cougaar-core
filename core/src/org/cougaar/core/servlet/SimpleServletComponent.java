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

import org.cougaar.core.blackboard.*;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.component.*;
import org.cougaar.core.service.BlackboardService;
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
 * <p>
 * Bug 1073 allows for a single constructor that accepts a
 * SimpleServletSupport, which skips the two phase<ol>
 *   <li>construct with the zero-arg constructor</li>
 *   <li>call the setSimpleServletSupport(support) method</li>
 * </ol> This usage is <i>deprecated</i>, primarily due to security 
 * concerns.  See the bug report for details.
 *
 * @see SimpleServletSupport
 */
public class SimpleServletComponent
  extends BaseServletComponent
  implements BlackboardClient
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
  protected BlackboardService blackboard;
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

    // old-style constructor (deprecated!)
    //
    // see bug 1073
    try {
      Constructor cons = cl.getConstructor(
          new Class[]{SimpleServletSupport.class});
      /*
      System.err.println(
          "Warning: Support for the \""+
          classname+"(SimpleServletSupport)"+
          "\" constructor has been deprecated; see bug 1073");
          */
      SimpleServletSupport support;
      try {
         support = createSimpleServletSupport();
      } catch (Exception e) {
        throw new RuntimeException(
            "Unable to create Servlet support: "+
            e.getMessage());
      }
      Servlet ret;
      try {
        ret = (Servlet) cons.newInstance(new Object[]{support});
      } catch (Exception e) {
        throw new RuntimeException(
            "Unable to create Servlet instance: "+
            e.getMessage());
      }
      return ret;
    } catch (Exception e) {
      // good
    }

    // create a zero-arg instance
    try {
      this.servlet = (Servlet) cl.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to create Servlet instance: "+
          e.getMessage());
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
          "Unable to create Servlet support: "+
          e.getMessage());
    }

    // set the support
    try {
      supportMethod.invoke(servlet, new Object[]{support});
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to set Servlet support: "+
          e.getMessage());
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

    // get the blackboard service (for "query")
    // FIXME the servlet wants the service (bug 1073)!!!
    // The BlackboardClient API is awkward; maybe suggest
    //   a simplistic BlackboardQueryService that doesn't
    //   require the requestor to implement a special API???
    blackboard = (BlackboardService)
      serviceBroker.getService(
          this, // FIXME (bug 1073)
          BlackboardService.class,
          null);
    if (blackboard == null) {
      throw new RuntimeException(
          "Unable to obtain blackboard service");
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
        path, agentId, blackboard, ns, log);
  }

  /**
   * @deprecated see bug 1073
   */
  protected SimpleServletSupport createSimpleServletSupport() {
    // just like "createSimpleServletSupport(servlet)", but
    // passes "this" as the requestor

    blackboard = (BlackboardService)
      serviceBroker.getService(this, BlackboardService.class, null);
    if (blackboard == null) {
      throw new RuntimeException(
          "Unable to obtain blackboard service");
    }
    ns = (NamingService)
      serviceBroker.getService(
          this, NamingService.class, null);
    if (ns == null) {
      throw new RuntimeException(
          "Unable to obtain naming service");
    }
    log = (LoggingService)
      serviceBroker.getService(
          this, LoggingService.class, null);
    if (log == null) {
      // continue loading -- let the "support" use a null-logger.
    }
    return
      new SimpleServletSupportImpl(
        path, agentId, blackboard, ns, log);
  }

  public void unload() {
    // release all services
    super.unload();

    // old-style
    if (servlet == null) {
      if (log != null) {
        serviceBroker.releaseService(
            this, LoggingService.class, log);
        log = null;
      }
      if (ns != null) {
        serviceBroker.releaseService(
            this, NamingService.class, ns);
        ns = null;
      }
      if (blackboard != null) {
        serviceBroker.releaseService(
            this, BlackboardService.class, blackboard);
        blackboard = null;
      }
      return;
    }

    // new-style
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
      if (blackboard != null) {
        serviceBroker.releaseService(
            servlet, BlackboardService.class, blackboard);
        blackboard = null;
      }
      servlet = null;
    }
  }

  public String toString() {
    return classname+"("+path+")";
  }

  // odd BlackboardClient method:
  public String getBlackboardClientName() {
    return toString();
  }

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }

  // unused BlackboardClient method:
  public boolean triggerEvent(Object event) {
    // if we had Subscriptions we'd need to implement this.
    //
    // see "ComponentPlugin" for details.
    throw new UnsupportedOperationException(
        this+" only supports Blackboard queries, but received "+
        "a \"trigger\" event: "+event);
  }
}

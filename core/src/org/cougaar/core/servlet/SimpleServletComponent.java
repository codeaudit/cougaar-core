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
import org.cougaar.util.GenericStateModelAdapter;

import org.cougaar.core.blackboard.*;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.component.*;
import org.cougaar.core.naming.NamingService;
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
 *   &lt;this-class&gt; is "org.cougaar.core.servlet.SimpleServletComponent"
 *   &lt;servlet-class&gt; is the class name of a Servlet.  If the
 *      Servlet has a "public classname(SimpleServletSupport support)"
 *      constructor then a SimpleServletSupport is passed.
 *   &lt;path&gt; is the path for the Servlet, such as "/test".
 * </pre><br>
 * <p>
 * Eventually this class may be integrated into a 
 * <code>BinderFactory</code> to allow the simpler:<pre>
 *    plugin = &lt;servlet-class&gt;(&lt;path&gt;)
 * </pre><br>
 *
 * @see SimpleServletSupport
 */
public class SimpleServletComponent
  extends GenericStateModelAdapter
  implements Component, BlackboardClient
{

  /**
   * Servlet classname from "setParameter(..)".
   */
  private String classname;

  /**
   * Servlet path from "setParameter(..)".
   */
  private String path;

  /**
   * Instance of <tt>classname</tt>.
   */
  private Servlet servlet;

  /**
   * Service broker from "setBindingSite(..)".
   */
  private ServiceBroker serviceBroker;

  /**
   * To launch our Servlet we need to obtain the Servlet
   * registration service.
   */
  private ServletService servletService;

  /**
   * Agent identifier for the Agent that loaded this Component.
   */
  private ClusterIdentifier agentId;

  //
  // Services for our SimpleServletSupport use
  //
  private BlackboardService blackboard;
  private NamingService ns;


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
    this.serviceBroker = bindingSite.getServiceBroker();

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
   * "/test/FOO".
   *
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

  public void load() {
    super.load();
    
    // first get the servlet service
    servletService = (ServletService)
      serviceBroker.getService(
		    this,
		    ServletService.class,
		    null);
    if (servletService == null) {
      throw new IllegalStateException(
          "Unable to obtain servlet service");
    }

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
    
    // find the appropriate constructor
    boolean needsSupport;
    java.lang.reflect.Constructor cons;
    try {
      cons = cl.getConstructor(
          new Class[]{SimpleServletSupport.class});
      needsSupport = true;
    } catch (Exception e) {
      try {
        cons = cl.getConstructor(new Class[]{});
        needsSupport = false;
      } catch (Exception e2) {
        throw new RuntimeException(
            "Servlet \""+classname+"\" lacks both a \""+
            classname+"(SimpleServletSupport)\" and \""+
            classname+"()\" constructors");
      }
    }

    // obtain the services required for support
    SimpleServletSupport support;
    if (needsSupport) {
      // get the blackboard service (for "query")
      blackboard = (BlackboardService)
        serviceBroker.getService(
            this, 
            BlackboardService.class,
            null);
      if (blackboard == null) {
        throw new RuntimeException(
            "Unable to obtain blackboard service");
      }

      // get the naming service (for "listAgentNames")
      ns = (NamingService)
        serviceBroker.getService(
            this, 
            NamingService.class,
            null);
      if (ns == null) {
        throw new RuntimeException(
            "Unable to obtain naming service");
      }

      support = new SimpleServletSupportImpl();
    } else {
      support = null;
    }

    // create the servlet instance
    try {
      Object[] args = 
          ((needsSupport) ? 
           (new Object[]{support}) :
           (new Object[] {}));
      this.servlet = (Servlet) cons.newInstance(args);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to create Servlet instance: "+
          e.getMessage());
    }

    // register the servlet
    try {
      servletService.register(path, servlet);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to register servlet with path \""+
          path+"\"");
    }
  }

  public void unload() {
    // release all services
    if (ns != null) {
      serviceBroker.releaseService(
          this, NamingService.class, ns);
    }
    if (blackboard != null) {
      serviceBroker.releaseService(
          this, BlackboardService.class, blackboard);
    }
    if (servletService != null) {
      serviceBroker.releaseService(
        this, ServletService.class, servletService);
    }
    super.unload();
  }

  public String toString() {
    return 
      ((servlet != null) ? 
       servlet.getClass().getName() : 
       "null") +
      "("+path+")";
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




  /**
   * This Component's "hook" that allows it's Servlet to
   * access COUGAAR Services.
   */
  private class SimpleServletSupportImpl
  implements SimpleServletSupport 
  {

    private String agentName;
    private String encAgentName;

    private SimpleServletSupportImpl() {
      // cache these:
      agentName = agentId.getAddress();
      encAgentName = encodeAgentName(agentName);
    }

    public String getPath() {
      return path;
    }

    public String encodeAgentName(String name) {
      return URLEncoder.encode(name);
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

    public String getAgentName() {
      return agentName;
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
      } catch (Exception e) {
        throw new RuntimeException(e.getMessage());
      }

      return toList;
    }

    public List getAllAgentNames() {
      return getAllAgentNames(new ArrayList());
    }

    public List getAllAgentNames(List toList) {
      throw new UnsupportedOperationException(
          "Not implemented yet --"+
          " see \"getAllEncodedAgentNames()\"");
    }

    // etc to match "SimpleServletSupport"

    public String toString() {
      return 
        "Support for "+
        SimpleServletComponent.this.toString();
    }
  }

}


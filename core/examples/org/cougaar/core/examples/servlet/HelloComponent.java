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
package org.cougaar.core.examples.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.util.GenericStateModelAdapter;

import org.cougaar.core.component.*;
import org.cougaar.core.service.ServletService;

/**
 * Example Component that creates a "hello world!" Servlet and
 * registers for all "/hello" HTTP/HTTPS requests.
 * <p>
 * This is the simplest possible Servlet that shows the full
 * <code>ServletService</code> request code.
 *
 * @see ServletService
 */
public class HelloComponent
  extends GenericStateModelAdapter
  implements Component
{

  /**
   * This is the path for my Servlet, relative to the
   * Agent's URLEncoded name.
   * <p>
   * For example, on Agent "X" the URI request path
   * will be "/$X/hello".
   */
  private final String myPath = "/hello";

  /**
   * This is my Servlet, which is the "HelloServlet" inner class
   * that is defined at the end of this file.
   */
  private final Servlet myServlet = new HelloServlet();

  /**
   * Save our service broker during initialization.
   */
  private ServiceBroker serviceBroker;

  /**
   * To launch our Servlet we will use the 
   * "<code>ServletService</code>" Servlet Registration Service,
   * which is obtained from the <tt>serviceBroker</tt>.
   * <p>
   * This is used during "load()" and "unload()".
   */
  private ServletService servletService;

  /**
   * Constructor.
   */
  public HelloComponent() {
    super();
  }

  /**
   * If our Component had a parameter we could capture it here.
   * <p>
   * For example, the <tt>myPath</tt> could be passed.
   */
  public void setParameter(Object o) {
    // ignore; likely an empty Vector
  }

  /**
   * Save our ServiceBroker during initialization.
   * <p>
   * This method is called when this class is created.
   */
  public void setBindingSite(BindingSite bs) {
    this.serviceBroker = bs.getServiceBroker();
  }

  /**
   * When this class is created the "load()" method will
   * be called, at which time we'll register our Servlet.
   */
  public void load() {
    super.load();
    
    // get the servlet service
    servletService = (ServletService)
      serviceBroker.getService(
		    this,
		    ServletService.class,
		    null);
    if (servletService == null) {
      throw new RuntimeException(
          "Unable to obtain ServletService");
    }

    // register our servlet
    try {
      servletService.register(myPath, myServlet);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to register servlet with path \""+
          myPath+"\": "+e.getMessage());
    }
  }

  /**
   * When our class is unloaded we must release our service.
   * <p>
   * This will automatically unregister our Servlet.
   */
  public void unload() {
    // release our servlet service
    if (servletService != null) {
      serviceBroker.releaseService(
        this, ServletService.class, servletService);
    }
    super.unload();
  }

  /**
   * Pretty to-String for debugging.
   */
  public String toString() {
    return getClass().getName()+"("+myPath+")";
  }

  /**
   * Here is our inner class that will handle all HTTP and
   * HTTPS service requests for our <tt>myPath</tt>.
   */
  private class HelloServlet
  extends HttpServlet 
  {
    public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {
      // generate our response.
      //
      // here we simply generate an HTML page with the
      // "hello world!" string.
      //
      // we can also access fields of HelloServlet to create a 
      // more complex Servlet (e.g. access blackboard).  Note
      // that multiple service requests can occur at the same 
      // time, so be careful about synchronization.
      PrintWriter out = res.getWriter();
      out.print(
          "<HTML><BODY>\n"+
          "<h1>hello world!</h1>\n"+
          "</BODY></HTML>\n");
    }
  }
}

/*
 * <copyright>
 *  Copyright 2000-2003 BBNT Solutions, LLC
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

/**
 * This is a very simple "stand-alone" Servlet, with no Cougaar 
 * dependencies, that just prints "Hello world!" to the client.
 * <p>
 * This is just like the "tutorial" Servlets you would find
 * online, such as:<pre>
 *   <a href="http://java.sun.com/docs/books/tutorial/servlets">
 *   http://java.sun.com/docs/books/tutorial/servlets</a>
 * </pre>
 * This Servlet is unable to ask about Cougaar internals, such
 * as a query into the Blackboard.  Other classes in this package
 * will provide such Cougaar-internal examples.
 * <p>
 * This Servlet can be loaded with the Cougaar "servlet-loader" plugin
 * by adding this line to the ".INI" file of any agent:<pre>
 *    plugin = org.cougaar.core.servlet.SimpleServletComponent(org.cougaar.core.examples.servlet.HelloServlet, /hello)
 * </pre>
 * The above line tells the loader to associate the path "/hello" 
 * with this class.  If this line was added to agent X's ".INI", 
 * then this Servlet will be invoked when a browser requests:<pre>
 *    <a href="http://localhost:8800/$X/hello">
 *    http://localhost:8800/$X/hello</a>
 * </pre>
 * <p>
 * The above "localhost:8800" assumes that machine "localhost" 
 * is running a node (any node in the society, not just agent X's 
 * node), and that the node is configured to run the webserver on
 * port 8800.  See the "webserver/doc/install.html" for details.
 * <p>
 * The output to the browser should look like:<pre>
 *   Hello world!
 * </pre>
 */
public class HelloServlet extends HttpServlet {
  public void doGet(
      HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.print("Hello world!");
  }
}

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

import org.cougaar.core.servlet.SimpleServletSupport;

/**
 * This Servlet is very much like the "HelloServlet", except
 * that the constructor accepts a "SimpleServletSupport" API
 * from the "servlet-loader" plugin.
 * <p>
 * Follow the instructions in "HelloServlet" for loading this
 * Servlet into an agent.
 * <p>
 * The "SimpleServletSupport" API provides a couple <i>simple</i>
 * methods for the Servlet to call:
 * <ul>
 *   <li>get the agent's name</li>
 *   <li>query the blackboard</li>
 *   <li>get the names of all the agents in the society</li>
 * </ul>
 * See the javadocs for details.  This API is sufficient for 
 * many simplistic Servlets, such as the "/tasks" Servlet.
 * <p>
 * The output to the browser should look like:<pre>
 *   Hello world from agent X!
 * </pre>
 */
public class HelloAgentServlet extends HttpServlet {
  private SimpleServletSupport support;
  public HelloAgentServlet(SimpleServletSupport support) {
    this.support = support;
  }
  public void doGet(
      HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.print(
        "Hello world from agent "+
        support.getEncodedAgentName()+
        "!");
  }
}

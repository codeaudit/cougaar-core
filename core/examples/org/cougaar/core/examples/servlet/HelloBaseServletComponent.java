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

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.servlet.BaseServletComponent;

/**
 * This is an example of a full-fledged Component that uses
 * the "BaseServletComponent" base-class to configure
 * its path and Servlet.
 * <p>
 * Unlike the "Hello*Servlet" examples, this Component loads itself
 * with this ".INI" line:<pre>
 *    plugin = org.cougaar.core.examples.servlet.HelloBaseServletComponent
 * </pre>
 * The path is hard-coded to "/hello", so in agent X this would respond
 * to "http://localhost:8800/$X/hello" requests.
 * <p>
 * See "BaseServletComponent" for javadoc details.
 * <p>
 * Unlike the simple "HelloServlet", this class has access to 
 * the BindingSite and ServiceBroker of the BaseServletComponent.
 * This is illustrated in "HelloLogServletComponent" by getting
 * the LoggingService.
 * <p>
 * The output to the browser should look like:<pre>
 *   Hello world from self-loaded servlet!
 * </pre>
 */
public class HelloBaseServletComponent extends BaseServletComponent {

  protected String getPath() {
    return "/hello";
  }

  protected Servlet createServlet() {
    return new HttpServlet() {
      public void doGet(
          HttpServletRequest req,
          HttpServletResponse res) throws IOException {
        PrintWriter out = res.getWriter();
        out.println("Hello world from self-loaded servlet!");
      }
    };
  }

}

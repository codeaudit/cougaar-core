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

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.servlet.ServletService;

/**
 * Example subclass of "BaseServletComponent" that illustrates
 * the use of the ServiceBroker to aquire a Service (in this
 * case, the LoggingService).
 */
public class LogServletComponent extends BaseServletComponent {

  private LoggingService log;

  protected String getPath() {
    return "/log";
  }

  protected Servlet createServlet() {
    // get the logging service
    log = (LoggingService)
      serviceBroker.getService(
          this,
          LoggingService.class,
          null);
    if (log == null) {
      throw new RuntimeException("no log?!");
    }

    // We could inline "MyServlet" here as an anonymous
    // inner-class (like HelloBaseServletComponent does). Instead, 
    // we'll move it to a simple inner-class, which will make the 
    // code a little easier to read.
    return new MyServlet();
  }

  public void unload() {
    super.unload();
    // release the logging service
    if (log != null) {
      serviceBroker.releaseService(
        this, ServletService.class, servletService);
      log = null;
    }
  }

  private class MyServlet extends HttpServlet {
    public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {
      PrintWriter out = res.getWriter();
      out.println("Hello from log-servlet!");
      log.log(log.DEBUG, "my log debug message");
    }
  }
}

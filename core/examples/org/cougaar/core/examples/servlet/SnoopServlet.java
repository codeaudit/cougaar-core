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
import java.util.Enumeration;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Another Cougaar-independent Servlet that calls all the 
 * various HttpServletRequest methods and prints the results 
 * back to the client -- load this and learn the Servlet API 
 * by example.
 * <p>
 * See the "HelloServlet" javadocs for loading details.
 * <p>
 * Note: This Servlet lists all sorts of HttpServletRequest 
 * properties (sessions, cookies, etc) that are rarely 
 * needed by most Cougaar Servlets.  Simply skim the
 * results and find what you need...
 * <p>
 * Based upon the "SnoopServlet" provided
 * in Tomcat 3.3:<pre>
 *  ($TOMCAT_HOME/webapps/examples/WEB-INF/classes/SnoopServlet.java)
 * </pre> See "http://jakarta.apache.org/tomcat/" for the 
 * original (nearly-identical) source.
 */
public class SnoopServlet extends HttpServlet {

  public void doPut(
      HttpServletRequest request, 
      HttpServletResponse response) throws ServletException, IOException {
      doGet(request, response);
    }

  public void doPost(
      HttpServletRequest request, 
      HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  public void doGet(
      HttpServletRequest request, 
      HttpServletResponse response) throws ServletException, IOException {
    PrintWriter out = response.getWriter();
    response.setContentType("text/plain");

    out.println("Snoop Servlet");
    out.println();
    out.println("Servlet init parameters:");
    Enumeration e = getInitParameterNames();
    while (e.hasMoreElements()) {
      String key = (String)e.nextElement();
      String value = getInitParameter(key);
      out.println("   " + key + " = " + value); 
    }
    out.println();

    out.println("Context init parameters:");
    ServletContext context = getServletContext();
    Enumeration en = context.getInitParameterNames();
    while (en.hasMoreElements()) {
      String key = (String)en.nextElement();
      Object value = context.getInitParameter(key);
      out.println("   " + key + " = " + value);
    }
    out.println();

    out.println("Context attributes:");
    en = context.getAttributeNames();
    while (en.hasMoreElements()) {
      String key = (String)en.nextElement();
      Object value = context.getAttribute(key);
      out.println("   " + key + " = " + value);
    }
    out.println();

    out.println("Request attributes:");
    e = request.getAttributeNames();
    while (e.hasMoreElements()) {
      String key = (String)e.nextElement();
      Object value = request.getAttribute(key);
      out.println("   " + key + " = " + value);
    }
    out.println();
    out.println("Servlet Name: " + getServletName());
    out.println("Protocol: " + request.getProtocol());
    out.println("Scheme: " + request.getScheme());
    out.println("Server Name: " + request.getServerName());
    out.println("Server Port: " + request.getServerPort());
    out.println("Server Info: " + context.getServerInfo());
    out.println("Remote Addr: " + request.getRemoteAddr());
    out.println("Remote Host: " + request.getRemoteHost());
    out.println("Character Encoding: " + request.getCharacterEncoding());
    out.println("Content Length: " + request.getContentLength());
    out.println("Content Type: "+ request.getContentType());
    out.println("Locale: "+ request.getLocale());
    out.println("Default Response Buffer: "+ response.getBufferSize());
    out.println();
    out.println("Parameter names in this request:");
    e = request.getParameterNames();
    while (e.hasMoreElements()) {
      String key = (String)e.nextElement();
      String[] values = request.getParameterValues(key);
      out.print("   " + key + " = ");
      for(int i = 0; i < values.length; i++) {
        out.print(values[i] + " ");
      }
      out.println();
    }
    out.println();
    out.println("Headers in this request:");
    e = request.getHeaderNames();
    while (e.hasMoreElements()) {
      String key = (String)e.nextElement();
      String value = request.getHeader(key);
      out.println("   " + key + ": " + value);
    }
    out.println();  
    out.println("Cookies in this request:");
    Cookie[] cookies = request.getCookies();
    for (int i = 0; i < cookies.length; i++) {
      Cookie cookie = cookies[i];
      out.println("   " + cookie.getName() + " = " + cookie.getValue());
    }
    out.println();

    out.println("Request Is Secure: " + request.isSecure());
    out.println("Auth Type: " + request.getAuthType());
    out.println("HTTP Method: " + request.getMethod());
    out.println("Remote User: " + request.getRemoteUser());
    out.println("Request URI: " + request.getRequestURI());
    out.println("Context Path: " + request.getContextPath());
    out.println("Servlet Path: " + request.getServletPath());
    out.println("Path Info: " + request.getPathInfo());
    out.println("Path Trans: " + request.getPathTranslated());
    out.println("Query String: " + request.getQueryString());

    out.println();
    HttpSession session = request.getSession();
    out.println("Requested Session Id: " +
        request.getRequestedSessionId());
    out.println("Current Session Id: " + session.getId());
    out.println("Session Created Time: " + session.getCreationTime());
    out.println("Session Last Accessed Time: " +
        session.getLastAccessedTime());
    out.println("Session Max Inactive Interval Seconds: " +
        session.getMaxInactiveInterval());
    out.println();
    out.println("Session values: ");
    Enumeration names = session.getAttributeNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      out.println("   " + name + " = " + session.getAttribute(name));
    }

    out.println();
    int len = request.getContentLength();
    out.println("Body["+len+"]");
    if (len > 0) {
      out.println("--------------------------");
      java.io.InputStream in = request.getInputStream();
      byte[] buf = new byte[512];
      while (true) {
        int l = in.read(buf, 0, 512);
        if (l < 0) {
          break;
        }
        String s = new String(buf, 0, l);
        out.print(s);
      }
      out.println("--------------------------");
    }
  }
}


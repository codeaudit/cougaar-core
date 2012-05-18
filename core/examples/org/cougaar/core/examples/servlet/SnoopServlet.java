/*
 * <copyright>
 *  
 *  Copyright 2000-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.examples.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * A Cougaar-independent Servlet that calls all the various
 * HttpServletRequest methods and prints the results back to the
 * client, which is ideal for learning the Servlet API by example.
 * <p>
 * Unlike the <code>HelloServlet</code>, which extends
 * "ComponentServlet", this class extends the more basic
 * non-component "HttpServlet" class.  A "wrapping" component
 * must be used to load this servlet and register it with the
 * agent's ServletService:
 * <pre> 
 *    &lt;component
 *      class="org.cougaar.core.servlet.SimpleServletComponent"&gt;
 *      &lt;argument&gt;org.cougaar.core.examples.servlet.SnoopServlet&lt;/argument&gt;
 *      &lt;argument&gt;/snoop&lt;/argument&gt;
 *    &lt;/component&gt;
 * </pre> 
 * <p>
 * This Servlet lists all sorts of HttpServletRequest properties
 * (sessions, cookies, etc) that are rarely needed by most Cougaar
 * Servlets.  Simply skim the results and find what you need...
 * <p>
 * Based upon the "SnoopServlet" provided in Tomcat 3.3:<pre>
 *  ($TOMCAT_HOME/webapps/examples/WEB-INF/classes/SnoopServlet.java)
 * </pre> See "http://jakarta.apache.org/tomcat/" for the original
 * (nearly-identical) source.
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
    int ncookies = (cookies == null ? 0 : cookies.length);
    for (int i = 0; i < ncookies; i++) {
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


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

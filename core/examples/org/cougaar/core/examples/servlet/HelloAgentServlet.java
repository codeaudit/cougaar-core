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
  public void setSimpleServletSupport(SimpleServletSupport support) {
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

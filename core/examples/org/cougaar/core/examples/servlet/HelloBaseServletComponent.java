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

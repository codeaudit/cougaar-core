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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cougaar.core.servlet.ComponentServlet;

/**
 * This servlet illustrates an HTML form and URL-parameters passing.
 * <p> 
 * To load this servlet, add the following to any agent's XML
 * configuration:<pre> 
 *    &lt;component
 *      class="org.cougaar.core.examples.servlet.ColorServlet"&gt;
 *      &lt;argument&gt;/color&lt;/argument&gt;
 *    &lt;/component&gt;
 * </pre>
 */
public class ColorServlet extends ComponentServlet {

  public void doGet(
      HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    // use a "worker" class, since multiple requests
    // could happen at the same time (which would mangle
    // our variables).
    Worker w = new Worker(request, response);
    w.execute();
  }

  private static class Worker {
    // from the "doGet(..)":
    private HttpServletRequest request;
    private HttpServletResponse response;

    // from URL-params:
    //   (this could be named "firstColor", but here
    //    we'll name it "c1" to show that these are
    //    separate concepts).
    private static final String FIRST_COLOR_PARAM = "c1";
    private String firstColor;
    
    // ditto for the "secondColor":
    private static final String SECOND_COLOR_PARAM = "c2";
    private String secondColor;

    // built-in colors for "secondColor":
    private static final String[] COLORS = new String[] {
      "red",
      "orange",
      "yellow",
      "green",
      "blue",
      "indigo",
      "violet",
    };

    public Worker(
        HttpServletRequest request,
        HttpServletResponse response) {
      this.request = request;
      this.response = response;
    }

    public void execute() throws IOException {
      parseParams();
      writeResponse();
    }

    private void parseParams() {
      firstColor = getParameter(FIRST_COLOR_PARAM, "black");
      secondColor = getParameter(SECOND_COLOR_PARAM, COLORS[0]);
    }

    private String getParameter(String name, String defaultValue) {
      String value = request.getParameter(name);
      if (value != null) {
        value = value.trim();
        if (value.length() == 0) {
          value = null;
        }
      }
      if (value == null) {
        value = defaultValue;
      }
      return value;
    }

    private void writeResponse() throws IOException {
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      // begin page:
      out.print(
          "<html><body>\n"+
          "<h2>Example color-change page</h2><br>\n");
      // begin form:
      //   (always use "GET", not "POST"!)
      out.print("<form method=\"GET\" action=\"");
      // form sends back to this servlet:
      out.print(request.getRequestURI());
      // show first color, let the user change it:
      out.print(
          "\">\n"+
          "First color is "+
          "<font size=+2 color=\""+
          firstColor+
          "\">"+
          firstColor+
          "</font> (<tt>"+
          firstColor+
          "</tt>), change to: "+
          "<input name=\""+
          FIRST_COLOR_PARAM+
          "\" type=\"text\" size=\"20\" value=\""+
          firstColor+
          "\"><br>\n");
      // just for kicks, lets use a drop-down list here:
      out.print(
          "Second color is "+
          "<font size=+2 color=\""+
          secondColor+
          "\">"+
          secondColor+
          "</font> (<tt>"+
          secondColor+
          "</tt>), change to: "+
          "<select name=\""+
          SECOND_COLOR_PARAM+
          "\">\n");
      for (int i = 0; i < COLORS.length; i++) {
        String ci = COLORS[i];
        out.print("<option");
        if (ci.equals(secondColor)) {
          out.print(" selected");
        }
        out.print(">"+ci+"</option>\n");
      }
      out.print("</select><br>\n");
      // add form button:
      out.print(
          "<input type=\"submit\" value=\"change colors!\"><br>\n");
      // end form:
      out.print("</form>\n");
      // end page:
      out.print("</body></html>");
    }
  }
}

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
import java.util.Enumeration;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This simple servlet illustrates an HTML form and URL-parameters
 * passing.
 */
public class ColorServlet extends HttpServlet {

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

    private void parseParams() throws IOException {
      // set default values
      firstColor = "black";
      secondColor = COLORS[0];
      // get "name=value" parameters
      for (Enumeration en = request.getParameterNames();
           en.hasMoreElements();
          ) {
        String name = (String) en.nextElement();
        String values[] = request.getParameterValues(name);
        int nvalues = ((values != null) ? values.length : 0);
        if (nvalues > 0) {
          // use last value:
          String value = values[nvalues - 1];
          // save param
          if (name.equals(FIRST_COLOR_PARAM)) {
            firstColor = value;
          } else if (name.equals(SECOND_COLOR_PARAM)) {
            secondColor = value;
          } else {
            // ignore unknown param
          }
        }
      }
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

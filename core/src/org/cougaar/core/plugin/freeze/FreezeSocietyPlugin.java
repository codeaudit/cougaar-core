/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

package org.cougaar.core.plugin.freeze;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ServletService;
import org.cougaar.core.service.TopologyReaderService;

/**
 * This plugin initiates a society-wide freeze for assessment
 **/

public class FreezeSocietyPlugin extends FreezeSourcePlugin {
  private ServletService servletService = null;
  protected String status = "Running";

  public void setupSubscriptions() {
    super.setupSubscriptions();
    try {
      servletService = (ServletService)
        getServiceBroker().getService(this, ServletService.class, null);
      servletService.register("/freezeControl", new FreezeControlServlet());
    } catch (Exception e) {
      logger.error("Failed to register freezeControl servlet", e);
    }
  }

  protected synchronized void setUnfrozenAgents(Set unfrozenAgents) {
    if (logger.isDebugEnabled()) logger.debug("unfrozen" + unfrozenAgents);
    if (unfrozenAgents.isEmpty()) {
      status = "Frozen";
    } else {
      status = "Freezing " + unfrozenAgents;
    }
  }

  protected Set getTargetNames() {
    return topologyReaderService.getAll(TopologyReaderService.NODE);
  }

  private class FreezeControlServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      doPostOrGet(request, response, false);
    }
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
      doPostOrGet(request, response, true);
    }
    protected void doPostOrGet(HttpServletRequest request, HttpServletResponse response, boolean doUpdate)
      throws IOException
    {
      String submit = request.getParameter("submit");
      boolean freeze = "Freeze".equals(submit);
      boolean thaw = "Thaw".equals(submit);
      String tstatus = status;
      if (freeze || thaw) {
        blackboard.openTransaction();
        synchronized (FreezeSocietyPlugin.this) {
          if (freeze) {
            freeze();
            status = "Freezing...";
          } else if (thaw) {
            thaw();
            status = "Running";
          }
          tstatus = status;
        }
        blackboard.closeTransactionDontReset();
      }
      response.setContentType("text/html");
      if (tstatus.startsWith("Freezing")) {
        response.setHeader("Refresh", "1");
      }
      PrintWriter out = response.getWriter();
      out.println("<html>\n  <head>\n    <title>Society Freeze Control</title>\n  </head>");
      out.println("  <body>\n    <h1>Society Freeze Control</h1>"
                  + "    <form action=\"freezeControl\" method=\"post\">");
      out.println(tstatus + "<br>");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Freeze\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Thaw\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Refresh\">");
      out.println("    </form>\n  </body>\n</html>");
    }
  }
}
      

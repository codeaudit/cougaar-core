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

package org.cougaar.core.node.service.jvmdump;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Date;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.JvmStackDumpService;
import org.cougaar.core.servlet.BaseServletComponent;

/**
 * Trivial servlet that registers with the path "/jvmdump"
 * and allows the client to invoke the "dumpStack()" method.
 * <p>
 * The servlet requires a "?action=Dump" URL parameter.
 * If not provided, the user is prompted in an HTML form.
 */
public class JvmStackDumpServlet extends BaseServletComponent {

  private String hostId;
  private MessageAddress nodeId;
  private MessageAddress agentId;

  private JvmStackDumpService jsds;

  protected String getPath() {
    return "/jvmdump";
  }

  public void load() {
    // get the jvm stack dump service
    jsds = (JvmStackDumpService) serviceBroker.getService(
        this, JvmStackDumpService.class, null);
    // get the host name
    try {
      hostId = InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      hostId = null;
    }
    // get the agent id
    AgentIdentificationService ais = (AgentIdentificationService)
      serviceBroker.getService(
          this, AgentIdentificationService.class, null);
    if (ais != null) {
      agentId = ais.getMessageAddress();
      serviceBroker.releaseService(
          this, AgentIdentificationService.class, ais);
    }
    // get the node id
    NodeIdentificationService nis = (NodeIdentificationService)
      serviceBroker.getService(
          this, NodeIdentificationService.class, null);
    if (nis != null) {
      nodeId = nis.getMessageAddress();
      serviceBroker.releaseService(
          this, NodeIdentificationService.class, nis);
    }
    super.load();
  }

  public void unload() {
    super.unload();
    if (jsds != null) {
      serviceBroker.releaseService(
          this, JvmStackDumpService.class, jsds);
      jsds = null;
    }
  }

  protected Servlet createServlet() {
    return new JvmStackDumpServletImpl();
  }

  private class JvmStackDumpServletImpl extends HttpServlet {

    public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {

      // check for the "?action=Dump" parameter
      if (!"Dump".equals(req.getParameter("action"))) {
        String osName = System.getProperty("os.name");
        boolean isWindows = 
          (osName != null && osName.indexOf("Windows") >= 0);

        res.setContentType("text/html");
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter out = res.getWriter();
        startHtml(out);
        out.println(
            "Request a JVM stack dump to be sent to"+
            " standard-out.  This is equivalent to a "+
            (isWindows ? 
             "\"CTRL-BREAK\" on Windows" :
             "\"CTRL-\\\" on Unix")+
            ".<p>\n"+
            "Confirm by pressing the \"Dump\" button below:\n");
        printForm(out, req);
        out.println("</body></html>");
        return;
      }

      // check for service existence
      if (jsds == null) {
        res.setContentType("text/html");
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        PrintWriter out = res.getWriter();
        startHtml(out);
        out.println(
            "<b>ERROR:</b><p>\n"+
            "The JvmStackDumpService is not available in this"+
            " servlet's service broker.\n"+
            "</body></html>");
        return;
      }

      boolean didIt = jsds.dumpStack();

      // check the response
      if (!didIt) {
        String osName = System.getProperty("os.name");
        boolean isWindows = 
          (osName != null && osName.indexOf("Windows") >= 0);

        res.setContentType("text/html");
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        PrintWriter out = res.getWriter();
        startHtml(out);
        out.println(
            "<b>ERROR:</b><p>\n"+
            "The JvmStackDumpService's \"dumpStack()\" method"+
            " returned false.<p>\n"+
            "This indicates a possible JNI library path problem"+
            " in this node's "+
            (isWindows ? "%PATH%" : "$LD_LIBRARY_PATH")+
            ".<p>\n"+
            "For further information, please see the core"+
            " javadocs for "+
            "\"org.cougaar.core.node.service.jvmdump\".\n"+
            "</body></html>");
        return;
      }

      // success!
      res.setContentType("text/html");
      PrintWriter out = res.getWriter();
      startHtml(out);
      out.println(
          "<b>SUCCESS:</b><p>\n"+
          "The JVM stack has been successfully dumped to"+
          " standard-out.<p>\n"+
          "Invoke another stack dump?<p>");
        printForm(out, req);
        out.println("</body></html>");
    }

    private void startHtml(PrintWriter out) {
      out.println(
          "<html><head><title>"+
          nodeId+" JVM stack dump</title></head>"+
          "<body>\n"+
          "<h2>JVM stack dump servlet</h2><p>\n"+
          "<table>"+
          "<tr><td rowspan=5>&nbsp;&nbsp;&nbsp;</td></tr>"+
          "<tr><td><i>Host:</i></td><td>"+hostId+"</td></tr>"+
          "<tr><td><i>Node:</i></td><td>"+nodeId+"</td></tr>"+
          "<tr><td><i>Agent:</i></td><td>"+agentId+"</td></tr>"+
          "<tr><td><i>Date:</i></td><td>"+(new Date())+"</td></tr>"+
          "</table><p>");
    }

    private void printForm(
        PrintWriter out, HttpServletRequest req) {
      out.println(
          "<form method=\"GET\" action=\""+
          req.getRequestURI()+
          "\">\n"+
          "<input type=\"submit\" name=\"action\""+
          " value=\"Dump\"><br>\n"+
          "</form>\n");
    }
  }

}

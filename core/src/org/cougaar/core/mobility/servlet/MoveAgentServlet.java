/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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

package org.cougaar.core.mobility.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.cougaar.core.agent.AgentIdentificationService;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.BlackboardClient;

import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mobility.ldm.MoveAgent;
import org.cougaar.core.mobility.ldm.MobilityFactory;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.node.NodeIdentifier;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.util.UnaryPredicate;

/**
 * Servlet that allows the client to add a "MoveAgent"
 * object to the blackboard.
 * <p>
 * The path of the servlet is "/move".
 * <p>
 * The URL parameters to this servlet are:
 * <ul><p>
 *   <li>mobileAgent=STRING</li>
 *   <li>originNode=STRING</li>
 *   <li>destNode=STRING</li>
 *   <li>isForceRestart=BOOLEAN</li>
 * </ul>
 * <p>
 * Note the <b>SECURITY</b> issues!
 */
public class MoveAgentServlet
extends BaseServletComponent
implements BlackboardClient
{
  protected ClusterIdentifier agentId;
  protected NodeIdentifier nodeId;

  protected DomainService domain;
  protected NodeIdentificationService nodeIdService;
  protected BlackboardService blackboard;

  protected MobilityFactory mobilityFactory;

  protected static final UnaryPredicate MOVE_AGENT_PRED =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof MoveAgent);
      }
    };


  protected String getPath() {
    return "/move";
  }

  protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
  }

  public void setNodeIdentificationService(NodeIdentificationService nodeIdService) {
    this.nodeIdService = nodeIdService;
    this.nodeId = nodeIdService.getNodeIdentifier();
  }

  public void setBlackboardService(BlackboardService blackboard) {
    this.blackboard = blackboard;
  }

  public void setDomainService(DomainService domain) {
    this.domain = domain;
    mobilityFactory = 
      (MobilityFactory) domain.getFactory("mobility");
  }

  // aquire services:
  public void load() {
    // FIXME need AgentIdentificationService
    org.cougaar.core.plugin.PluginBindingSite pbs =
      (org.cougaar.core.plugin.PluginBindingSite) bindingSite;
    this.agentId = pbs.getAgentIdentifier();

    super.load();
  }

  // release services:
  public void unload() {
    super.unload();
    if (blackboard != null) {
      serviceBroker.releaseService(
          this, BlackboardService.class, blackboard);
      blackboard = null;
    }
    if (domain != null) {
      serviceBroker.releaseService(
          this, DomainService.class, domain);
      domain = null;
    }
    if (nodeIdService != null) {
      serviceBroker.releaseService(
          this, NodeIdentificationService.class, nodeIdService);
      nodeIdService = null;
    }
    // release agentIdService
  }

  protected Collection queryMoveAgents() {
    Collection ret = null;
    try {
      blackboard.openTransaction();
      ret = blackboard.query(MOVE_AGENT_PRED);
    } finally {
      blackboard.closeTransaction(false);
    }
    return ret;
  }

  protected void addMoveAgent(
      String mobileAgent,
      String originNode,
      String destNode,
      boolean isForceRestart) {
    if (mobilityFactory == null) {
      throw new RuntimeException(
          "Mobility factory (and domain) not enabled");
    }
    MessageAddress mobileAgentAddr = null;
    MessageAddress originNodeAddr = null;
    MessageAddress destNodeAddr = null;
    if (mobileAgent != null) {
      mobileAgentAddr = new ClusterIdentifier(mobileAgent);
    }
    if (originNode != null) {
      originNodeAddr = new MessageAddress(originNode);
    }
    if (destNode != null) {
      destNodeAddr = new MessageAddress(destNode);
    }
    Object ticketId =
      mobilityFactory.createTicketIdentifier();
    Ticket ticket = 
      new Ticket(
          ticketId,
          mobileAgentAddr,
          originNodeAddr,
          destNodeAddr,
          isForceRestart);
    MoveAgent ma = mobilityFactory.createMoveAgent(ticket);
    try {
      blackboard.openTransaction();
      blackboard.publishAdd(ma);
    } finally {
      blackboard.closeTransaction(false);
    }
  }

  /**
   * Servlet to handle requests.
   */
  private class MyServlet extends HttpServlet {

    public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {
      MyWorker mw = new MyWorker(req, res);
      mw.execute();
    }

    private class MyWorker {

      // from the "doGet(..)":
      private HttpServletRequest request;
      private HttpServletResponse response;

      // from the URL-params:
      //    (see the class-level javadocs for details)

      public static final String REFRESH_PARAM = "Refresh";
      public boolean isRefresh;

      public static final String MOVE_PARAM = "Move";
      public boolean isMove;

      public static final String MOBILE_AGENT_PARAM = "mobileAgent";
      public String mobileAgent;

      public static final String ORIGIN_NODE_PARAM = "originNode";
      public String originNode;

      public static final String DEST_NODE_PARAM = "destNode";
      public String destNode;

      public static final String IS_FORCE_RESTART_PARAM = "isForceRestart";
      public boolean isForceRestart;
      
      // worker constructor:
      public MyWorker(
          HttpServletRequest request,
          HttpServletResponse response) {
        this.request = request;
        this.response = response;
      }

      // handle a request:
      public void execute() throws IOException {
        parseParams();
        writeResponse();
      }

      private void parseParams() throws IOException {
        // get "name=value" parameters
        for (Enumeration en = request.getParameterNames();
            en.hasMoreElements();
            ) {
          // extract (name, value)
          String name = (String) en.nextElement();
          if (name == null) {
            continue;
          }
          String values[] = request.getParameterValues(name);
          int nvalues = ((values != null) ? values.length : 0);
          if (nvalues <= 0) {
            continue;
          }
          String value = values[nvalues - 1];
          if ((value == null) ||
              (value.length() <= 0)) {
            continue;
          }
          value = URLDecoder.decode(value, "UTF-8");

          // save parameters
          if (name.equals(MOVE_PARAM)) {
            isMove = "true".equalsIgnoreCase(value);
          } else if (name.equals(REFRESH_PARAM)) {
            isRefresh = "true".equalsIgnoreCase(value);
          } else if (name.equals(MOBILE_AGENT_PARAM)) {
            mobileAgent = value;
          } else if (name.equals(DEST_NODE_PARAM)) {
            destNode = value;
          } else if (name.equals(ORIGIN_NODE_PARAM)) {
            originNode = value;
          } else if (name.equals(IS_FORCE_RESTART_PARAM)) {
            if (value.equalsIgnoreCase("true")) {
              isForceRestart = true;
            } else if (value.equalsIgnoreCase("false")) {
              isForceRestart = false;
            }
          } else {
          }
        }
      }

      private void writeResponse() throws IOException {
        if (isMove) {
          try {
            addMoveAgent(
                mobileAgent,
                originNode,
                destNode,
                isForceRestart);
          } catch (Exception e) {
            writeFailure(e);
            return;
          }
          writeSuccess();
        } else {
          writeUsage();
        }
      }

      private void writeUsage() throws IOException {
        String msg = agentId+" move-agent";
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.print("<html><head><title>");
        out.print(msg);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<h2>");
        out.print(msg);
        out.print("</h2>\n");
        writeForm(out);
        out.print(
            "</body></html>\n");
        out.close();
      }

      private void writeFailure(Exception e) throws IOException {
        // select response message
        String msg = "Failed "+agentId+" move-agent";
        response.setContentType("text/html");
        // build up response
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(baos);
        out.print("<html><head><title>");
        out.print(msg);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<center><h1>");
        out.print(msg);
        out.print("</h1></center>"+
            "<p><pre>\n");
        e.printStackTrace(out);
        out.print(
            "\n</pre><p>"+
            "<h2>Please double-check these parameters:</h2>\n");
        writeForm(out);
        out.print(
            "</body></html>\n");
        out.close();
        // send error code
        response.sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            new String(baos.toByteArray()));
      }

      private void writeSuccess() throws IOException {
        String msg = agentId+" move-agent";
        // write response
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.print("<html><head><title>");
        out.print(msg);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<center><h1>");
        out.print(msg);
        out.print("</h1></center><p>\n");
        writeForm(out);
        out.print("</body></html>\n");
        out.close();
      }

      private void writeForm(
          PrintWriter out) throws IOException {
        // begin form
        out.print("<form method=\"GET\" action=\"");
        out.print(request.getRequestURI());
        out.print("\">\n");
        // show the current time
        out.print("<i>Time: ");
        out.print(new Date());
        out.print("</i><p>");
        out.print(
            "<h2>"+
            "Local MoveAgent Objects:</h2><p>");
        // show current MoveAgent objects
        Collection c = queryMoveAgents();
        int n = ((c != null) ? c.size() : 0);
        if (n == 0) {
          out.print("<i>none</i>");
        } else {
          out.print("<table>\n");
          Iterator iter = c.iterator();
          for (int i = 0; i < n; i++) {
            MoveAgent ma = (MoveAgent) iter.next();
            out.print("<tr><td>");
            out.print(ma.getUID());
            out.print("</td><td>");
            out.print(ma.getTicket());
            out.print("</td><td>");
            out.print(ma.getStatus());
            out.print("</td></tr>\n");
          }
          out.print("</table>\n");
        }
        if (mobileAgent != null) {
          out.print(
            "<input type=\"hidden\" name=\""+
            MOBILE_AGENT_PARAM+
            "\" value=\"");
          out.print(mobileAgent);
          out.print("\">");
        }
        if (originNode != null) {
          out.print(
              "<input type=\"hidden\" name=\""+
              ORIGIN_NODE_PARAM+
              "\" value=\"");
          out.print(originNode);
          out.print("\">");
        }
        if (destNode != null) {
          out.print(
            "<input type=\"hidden\" name=\""+
            DEST_NODE_PARAM+
            "\" value=\"");
          out.print(destNode);
          out.print("\">");
        }
        out.print(
            "<input type=\"hidden\" name=\""+
            IS_FORCE_RESTART_PARAM+
            "\" value=\"");
        out.print(isForceRestart);
        out.print("\">\n");
        out.print(
            "<p><input type=\"hidden\" name=\""+
            REFRESH_PARAM+
            "\" value=\"true\">");
        out.print(
            "<p><input type=\"submit\" value=\""+
            REFRESH_PARAM+
            "\">"+
            "</form>\n");

        // begin form
        out.print("<form method=\"GET\" action=\"");
        out.print(request.getRequestURI());
        out.print("\">\n");
        // allow user to submit a new MoveAgent request
        out.print(
            "<p>"+
            "<h2>Create a new agent-movement request:</h2>\n");
        out.print(
            "<table>\n"+
            "<tr><td>"+
            "Mobile Agent"+
            "</td><td>\n"+
            "<input type=\"text\" name=\""+
            MOBILE_AGENT_PARAM+
            "\" size=70");
        if (mobileAgent != null) {
          out.print(" value=\"");
          out.print(mobileAgent);
          out.print("\"");
        }
        out.print(
            ">"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Origin Node"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            ORIGIN_NODE_PARAM+
            "\" size=70");
        if (originNode != null) {
          out.print(" value=\"");
          out.print(originNode);
          out.print("\"");
        }
        out.print(
            ">"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Destination Node"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            DEST_NODE_PARAM+
            "\" size=70");
        if (destNode != null) {
          out.print(" value=\"");
          out.print(destNode);
          out.print("\"");
        }
        out.print(
            ">"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Force Restart"+
            "</td><td>"+
            "<select name=\""+
            IS_FORCE_RESTART_PARAM+
            "\">"+
            "<option value=\"true\"");
        if (isForceRestart) {
          out.print(" selected");
        }
        out.print(
            ">true</option>"+
            "<option value=\"false\"");
        if (!(isForceRestart)) {
          out.print(" selected");
        }
        out.print(
            ">false</option>"+
            "</select>\n"+
            "</td></tr>\n"+
            "<tr><td colwidth=2>"+
            "<input type=\"hidden\" name=\""+
            MOVE_PARAM+
            "\" value=\"true\">"+
            "<input type=\"submit\" value=\""+
            MOVE_PARAM+
            "\">"+
            "</td></tr>\n"+
            "</table>\n"+
            "</form>\n");
      }
    }
  }


  // odd BlackboardClient method:
  public String getBlackboardClientName() {
    return toString();
  }

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }

  // unused BlackboardClient method:
  public boolean triggerEvent(Object event) {
    // if we had Subscriptions we'd need to implement this.
    //
    // see "ComponentPlugin" for details.
    throw new UnsupportedOperationException(
        this+" only supports Blackboard queries, but received "+
        "a \"trigger\" event: "+event);
  }

  public String toString() {
    return "\""+getPath()+"\" servlet";
  }
}

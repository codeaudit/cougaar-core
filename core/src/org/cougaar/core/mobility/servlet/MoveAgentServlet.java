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
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

/**
 * Servlet that allows the client to add a "MoveAgent"
 * object to the blackboard.
 * <p>
 * The path of the servlet is "/move".
 * <p>
 * The URL parameters to this servlet are:
 * <ul><p>
 *   <li><tt>action=STRING</tt><br>
 *       Option action selection, where the default is "Refresh".
 *       <p>
 *       "Refresh" displays the current Scripts.
 *       <p>
 *       "Remove" removes the Script with the UID specified by
 *       the required "removeUID" parameter.
 *       <p>
 *       "Add" creates a new Script.  Most of the parameters
 *       below are used to support "Add".
 *       </li><p>
 *   <li><tt>removeUID=String</tt><br>
 *       If the action is "Remove", this is the UID of the script
 *       to be removed.  Any running processes are killed.
 *       </li><p>
 *   <li><tt>mobileAgent=STRING</tt><br>
 *       Option agent to move.  Defaults to this servlet's agent.
 *       </li><p>
 *   <li><tt>originNode=STRING</tt><br>
 *       Option origin node for the mobile agent.  Defaults to 
 *       wherever the agent happens to be at the time of the submit.
 *       If set, the move will assert the agent starting node 
 *       location.
 *       </li><p>
 *   <li><tt>destNode=STRING</tt><br>
 *       Option destination node for the mobile agent.  Defaults 
 *       to wherever the agent happens to be at the time of
 *       the submit.
 *       </li><p>
 *   <li><tt>isForceRestart=BOOLEAN</tt><br>
 *       Only applies when the destNode is not specified or
 *       matches the current agent location.  If true, the agent
 *       will undergo most of the move work, even though it's
 *       already at the specified destination node.
 *       </li><p>
 * </ul>
 * <p>
 * Note the <b>SECURITY</b> issues of moving agents!
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
      blackboard.closeTransactionDontReset();
    }
    return ret;
  }

  protected MoveAgent queryMoveAgent(final UID uid) {
    if (uid == null) {
      throw new IllegalArgumentException("null uid");
    }
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        return 
          ((o instanceof MoveAgent) &&
           (uid.equals(((MoveAgent) o).getUID())));
      }
    };
    MoveAgent ret = null;
    try {
      blackboard.openTransaction();
      Collection c = blackboard.query(pred);
      if ((c != null) && (c.size() >= 1)) {
        ret = (MoveAgent) c.iterator().next();
      }
    } finally {
      blackboard.closeTransactionDontReset();
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
      blackboard.closeTransactionDontReset();
    }
  }

  protected void removeMoveAgent(MoveAgent ma) {
    try {
      blackboard.openTransaction();
      blackboard.publishRemove(ma);
    } finally {
      blackboard.closeTransaction();
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

      // action:

      public static final String ACTION_PARAM = "action";
      public static final String ADD_VALUE = "Add";
      public static final String REMOVE_VALUE = "Remove";
      public static final String REFRESH_VALUE = "Refresh";
      public String action;

      // remove uid:
      
      public static final String REMOVE_UID_PARAM = "removeUID";
      public String removeUID;

      // ticket options:

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

        // action:
        action = request.getParameter(ACTION_PARAM);

        // remove param:
        removeUID = request.getParameter(REMOVE_UID_PARAM);
        if ((removeUID != null) && (removeUID.length() == 0)) {
          removeUID = null;
        }

        // ticket options:

        mobileAgent = request.getParameter(MOBILE_AGENT_PARAM);
        if ((mobileAgent != null) && (mobileAgent.length() == 0)) {
          mobileAgent = null;
        }

        destNode = request.getParameter(DEST_NODE_PARAM);
        if ((destNode != null) && (destNode.length() == 0)) {
          destNode = null;
        }

        originNode = request.getParameter(ORIGIN_NODE_PARAM);
        if ((originNode != null) && (originNode.length() == 0)) {
          originNode = null;
        }

        isForceRestart = "true".equalsIgnoreCase(
            request.getParameter(IS_FORCE_RESTART_PARAM));
      }

      private void writeResponse() throws IOException {
        if (ADD_VALUE.equals(action)) {
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
        } else if (REMOVE_VALUE.equals(action)) {
          try {
            UID uid = UID.toUID(removeUID);
            MoveAgent ma = queryMoveAgent(uid);
            if (ma != null) {
              removeMoveAgent(ma);
            }
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
          out.print(
              "<table border=1 cellpadding=1\n"+
              " cellspacing=1 width=95%\n"+
              " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
              "<tr>"+
              "<td>\n"+
              "<font size=+1 color=mediumblue><b>UID</b></font>"+
              "</td>"+
              "<td>"+
              "<font size=+1 color=mediumblue><b>Ticket</b></font>"+
              "</td>"+
              "<td>"+
              "<font size=+1 color=mediumblue><b>Status</b></font>"+
              "</td>"+
              "</tr>\n");
          Iterator iter = c.iterator();
          for (int i = 0; i < n; i++) {
            MoveAgent ma = (MoveAgent) iter.next();
            out.print("<tr><td>");
            out.print(ma.getUID());
            out.print("</td><td>");
            out.print(ma.getTicket());
            out.print("</td><td bgcolor=\"");
            MoveAgent.Status status = ma.getStatus();
            if (status == null) {
              out.print(
                  "#FFFFBB\">"+ // yellow
                  "In progress");
            } else {
              out.print(
                  (status.getCode() == MoveAgent.Status.OKAY) ?
                  "#BBFFBB\">" : // green
                  "#FFBBBB\">"); // red
              out.print(status);
            }
            out.print("</td></tr>\n");
          }
          out.print("</table>\n");
        }
        out.print(
            "<p><input type=\"submit\" name=\""+
            ACTION_PARAM+
            "\" value=\""+
            REFRESH_VALUE+
            "\">\n");

        // allow user to remove an existing MoveAgent
        out.print(
            "<p><hr><p>"+
            "<h2>Remove an existing move request:</h2>\n");
        if (n > 0) {
          out.print(
              "<select name=\""+
              REMOVE_UID_PARAM+
              "\">");
          Iterator iter = c.iterator();
          for (int i = 0; i < n; i++) {
            MoveAgent ma = (MoveAgent) iter.next();
            UID uid = ma.getUID();
            out.print("<option value=\"");
            out.print(uid);
            out.print("\">");
            out.print(uid);
            out.print("</option>");
          }
          out.print(
              "</select>"+
              "<input type=\"submit\" name=\""+
              ACTION_PARAM+
              "\" value=\""+
              REMOVE_VALUE+
              "\">\n");
        } else {
          out.print("<i>none</i>");
        }

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
            "<input type=\"submit\" name=\""+
            ACTION_PARAM+
            "\" value=\""+
            ADD_VALUE+
            "\">"+
            "<input type=\"reset\">"+
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

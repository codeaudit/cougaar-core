/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.examples.mobility.step;

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
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.examples.mobility.ldm.*;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

/**
 * Servlet that allows the client to add, remove, and view
 * mobility test "Step" objects.check for
 * <p>
 * The path of the servlet is "/move/step".
 * <p>
 * The URL parameters to this servlet are:
 * <ul><p>
 *   <li><tt>action=STRING</tt><br>
 *       Option action selection, where the default is "Refresh".
 *       <p>
 *       "Refresh" displays the current Steps and their status.
 *       <p>
 *       "Remove" removes the Step with the UID specified by
 *       the required "removeUID" parameter.
 *       <p>
 *       "Add" creates a new Step.  Most of the parameters
 *       below are used to support "Add".
 *       </li><p>
 *   <li><tt>actorAgent=STRING</tt><br>
 *       Optional name of the agent to perform the step.  Defaults
 *       to this servlet's agent.
 *       <li><p>
 *   <li><tt>pauseTime=STRING</tt><br>
 *       Optional time in milliseconds to pause before starting the
 *       step.  A value of "0" indicates no pause, which is the 
 *       default.  If the value starts with a "+" then the pause 
 *       time is relative to the current time (e.g. "+5000" for 
 *       5 seconds after the submit).  Other values are absolute 
 *       times in milliseconds (e.g. "1023309610110").
 *       </li><p>
 *   <li><tt>timeoutTime=STRING</tt><br>
 *       Optional time in milliseconds for the time limit of
 *       the step.  Supports the same "+" and time options as
 *       the "pauseTime" parameter.
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
public class StepManagerServlet
extends BaseServletComponent
implements BlackboardClient
{

  protected static final UnaryPredicate STEP_PRED =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Step);
      }
    };

  protected MessageAddress agentId;
  protected MessageAddress nodeId;

  protected BlackboardService blackboard;
  protected DomainService domain;
  protected LoggingService log;
  protected MobilityFactory mobilityFactory;
  protected MobilityTestFactory mobilityTestFactory;

  protected String getPath() {
    return "/move/step";
  }

  protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
  }

  // aquire services:
  public void load() {

    // get the log
    log = (LoggingService)
      serviceBroker.getService(
          this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    // get the agentId
    AgentIdentificationService agentIdService = 
      (AgentIdentificationService)
      serviceBroker.getService(
          this,
          AgentIdentificationService.class,
          null);
    if (agentIdService == null) {
      throw new RuntimeException(
          "Unable to obtain agent-id service");
    }
    this.agentId = agentIdService.getMessageAddress();
    serviceBroker.releaseService(
        this, AgentIdentificationService.class, agentIdService);
    if (agentId == null) {
      throw new RuntimeException(
          "Unable to obtain agent id");
    }

    // get the nodeId
    NodeIdentificationService nodeIdService = 
      (NodeIdentificationService)
      serviceBroker.getService(
          this,
          NodeIdentificationService.class,
          null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to obtain node-id service");
    }
    this.nodeId = nodeIdService.getMessageAddress();
    serviceBroker.releaseService(
        this, NodeIdentificationService.class, nodeIdService);
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node id");
    }

    // get the mobility domain
    this.domain = (DomainService)
      serviceBroker.getService(
          this,
          DomainService.class,
          null);
    if (domain == null) {
      throw new RuntimeException(
          "Unable to obtain domain service");
    }

    // get the mobility factory (for ticket creation)
    this.mobilityFactory = 
      (MobilityFactory) domain.getFactory("mobility");
    if (mobilityFactory == null) {
      throw new RuntimeException(
          "Mobility factory (and domain \"mobility\")"+
          " not enabled");
    }

    // get the mobility test factory (for step creation)
    this.mobilityTestFactory = 
      (MobilityTestFactory) domain.getFactory("mobilityTest");
    if (mobilityTestFactory == null) {
      throw new RuntimeException(
          "Mobility test factory (and domain \"mobilityTest\")"+
          " not enabled");
    }


    // get the blackboard
    this.blackboard = (BlackboardService)
      serviceBroker.getService(
          this,
          BlackboardService.class,
          null);
    if (blackboard == null) {
      throw new RuntimeException(
          "Unable to obtain blackboard service");
    }

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
      mobilityFactory = null;
      mobilityTestFactory = null;
    }
    if ((log != null) && (log != LoggingService.NULL)) {
      serviceBroker.releaseService(
          this, LoggingService.class, log);
      log = LoggingService.NULL;
    }
  }

  protected Collection querySteps() {
    Collection ret = null;
    try {
      blackboard.openTransaction();
      ret = blackboard.query(STEP_PRED);
    } finally {
      blackboard.closeTransactionDontReset();
    }
    return ret;
  }

  protected Step queryStep(final UID uid) {
    if (uid == null) {
      throw new IllegalArgumentException("null uid");
    }
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        return 
          ((o instanceof Step) &&
           (uid.equals(((Step) o).getUID())));
      }
    };
    Step ret = null;
    try {
      blackboard.openTransaction();
      Collection c = blackboard.query(pred);
      if ((c != null) && (c.size() >= 1)) {
        ret = (Step) c.iterator().next();
      }
    } finally {
      blackboard.closeTransactionDontReset();
    }
    return ret;
  }

  protected Ticket createTicket(
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
      mobileAgentAddr = 
        MessageAddress.getMessageAddress(mobileAgent);
    }
    if (originNode != null) {
      originNodeAddr = MessageAddress.getMessageAddress(originNode);
    }
    if (destNode != null) {
      destNodeAddr = MessageAddress.getMessageAddress(destNode);
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
    return ticket;
  }

  protected StepOptions createStepOptions(
      String actorAgent,
      Ticket ticket,
      long pauseTime,
      long timeoutTime) {
    MessageAddress actorAgentAddr = 
      ((actorAgent != null) ? 
       (MessageAddress.getMessageAddress(actorAgent)) :
       (agentId));
    StepOptions so = 
      new StepOptions(
          null,
          agentId, // source
          actorAgentAddr,
          ticket,
          pauseTime,
          timeoutTime);
    return so;
  }

  protected Step createStep(StepOptions options) {
    Step step = mobilityTestFactory.createStep(options);
    if (log.isInfoEnabled()) {
      log.info("Created new step: "+step);
    }
    return step;
  }

  protected void addStep(Step step) {
    try {
      blackboard.openTransaction();
      blackboard.publishAdd(step);
    } finally {
      blackboard.closeTransactionDontReset();
    }
  }

  protected void removeStep(Step step) {
    try {
      blackboard.openTransaction();
      blackboard.publishRemove(step);
    } finally {
      blackboard.closeTransactionDontReset();
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

      // actions:

      public static final String ACTION_PARAM = "action";
      public static final String ADD_VALUE = "Add";
      public static final String REMOVE_VALUE = "Remove";
      public static final String REFRESH_VALUE = "Refresh";
      public String action;

      // remove uid:
      
      public static final String REMOVE_UID_PARAM = "removeUID";
      public String removeUID;

      // step options:

      public static final String ACTOR_AGENT_PARAM = "actorAgent";
      public String actorAgent;

      public static final String PAUSE_TIME_PARAM = "pauseTime";
      public long pauseTime;

      public static final String TIMEOUT_TIME_PARAM = "timeoutTime";
      public long timeoutTime;

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
        removeUID = 
          request.getParameter(REMOVE_UID_PARAM);
        if ((removeUID != null) && (removeUID.length() == 0)) {
          removeUID = null;
        }

        // step options:
        long nowTime = -1;
        actorAgent = 
          request.getParameter(ACTOR_AGENT_PARAM);
        if ((actorAgent != null) && (actorAgent.length() == 0)) {
          actorAgent = null;
        }
        String sPauseTime = 
          request.getParameter(PAUSE_TIME_PARAM);
        if ((sPauseTime != null) && (sPauseTime.length() > 0)) {
          boolean isDelta = false;
          if (sPauseTime.startsWith("+")) {
            isDelta = true;
            sPauseTime = sPauseTime.substring(1);
            if (nowTime <= 0) {
              nowTime = System.currentTimeMillis();
            }
          }
          pauseTime = Long.parseLong(sPauseTime);
          if (isDelta) {
            pauseTime += nowTime;
          }
        }
        String sTimeoutTime = 
          request.getParameter(TIMEOUT_TIME_PARAM);
        if ((sTimeoutTime != null) && (sTimeoutTime.length() > 0)) {
          boolean isDelta = false;
          if (sTimeoutTime.startsWith("+")) {
            isDelta = true;
            sTimeoutTime = sTimeoutTime.substring(1);
            if (nowTime <= 0) {
              nowTime = System.currentTimeMillis();
            }
          }
          timeoutTime = Long.parseLong(sTimeoutTime);
          if (isDelta) {
            timeoutTime += nowTime;
          }
        }

        // ticket options:
        mobileAgent = 
          request.getParameter(MOBILE_AGENT_PARAM);
        if ((mobileAgent != null) && (mobileAgent.length() == 0)) {
          mobileAgent = null;
        }
        destNode = 
          request.getParameter(DEST_NODE_PARAM);
        if ((destNode != null) && (destNode.length() == 0)) {
          destNode = null;
        }
        originNode = 
          request.getParameter(ORIGIN_NODE_PARAM);
        if ((originNode != null) && (originNode.length() == 0)) {
          originNode = null;
        }
        isForceRestart = "true".equals(
            request.getParameter(IS_FORCE_RESTART_PARAM));
      }

      private void writeResponse() throws IOException {
        if (ADD_VALUE.equals(action)) {
          try {
            Ticket ticket = createTicket(
                mobileAgent,
                originNode,
                destNode,
                isForceRestart);
            StepOptions options = createStepOptions(
                actorAgent,
                ticket,
                pauseTime,
                timeoutTime);
            Step step = createStep(options);
            addStep(step);
          } catch (Exception e) {
            writeFailure(e);
            return;
          }
          writeSuccess();
        } else if (REMOVE_VALUE.equals(action)) {
          try {
            UID uid = UID.toUID(removeUID);
            Step step = queryStep(uid);
            if (step != null) {
              removeStep(step);
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
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.print("<html><head><title>Step ");
        out.print(agentId);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<h2>Step ");
        out.print(agentId);
        out.print("</h2>\n");
        writeForm(out);
        out.print(
            "</body></html>\n");
        out.close();
      }

      private void writeFailure(Exception e) throws IOException {
        // generate an HTML error response, with a 404 error code.
        //
        // use "setStatus" instead of "sendError" -- see bug 1259

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter out = response.getWriter();

        out.print("<html><head><title>Failed step for ");
        out.print(agentId);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<center><h1>Failed step for ");
        out.print(agentId);
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
      }

      private void writeSuccess() throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.print("<html><head><title>Step ");
        out.print(agentId);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<center><h1>Submitted step for ");
        out.print(agentId);
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
        long nowTime = System.currentTimeMillis();
        out.print("<i>Time: ");
        out.print(new Date(nowTime));
        out.print("</i> (");
        out.print(nowTime);
        out.print(")<p>");
        out.print(
            "<h2>"+
            "Local Steps:</h2><p>");
        // show current Steps objects
        Collection c = querySteps();
        int n = ((c != null) ? c.size() : 0);
        if (n == 0) {
          out.print("<i>none</i>");
        } else {
          out.print(
              "<table border=1 cellpadding=1\n"+
              " cellspacing=1 width=95%\n"+
              " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
              "<tr>"+
              "<td rowspan=2>\n"+
              "<font size=+1 color=mediumblue><b>UID</b></font>"+
              "</td>"+
              "<td colspan=5>"+
              "<font size=+1 color=mediumblue><b>Options</b></font>"+
              "</td>"+
              "<td colspan=4>"+
              "<font size=+1 color=mediumblue><b>Status</b></font>"+
              "</td>"+
              "</tr>\n"+
              "<tr>\n"+
              "<td><font color=mediumblue><b>Source</b></font></td>\n"+
              "<td><font color=mediumblue><b>Target</b></font></td>\n"+
              "<td><font color=mediumblue><b>Ticket</b></font></td>\n"+
              "<td><font color=mediumblue><b>Pause Time</b></font></td>\n"+
              "<td><font color=mediumblue><b>Timeout Time</b></font></td>\n"+
              "<td><font color=mediumblue><b>State</b></font></td>\n"+
              "<td><font color=mediumblue><b>Start Time</b></font></td>\n"+
              "<td><font color=mediumblue><b>End Time</b></font></td>\n"+
              "<td><font color=mediumblue><b>Move Status</b></font></td>\n"+
              "</tr>\n");
          Iterator iter = c.iterator();
          for (int i = 0; i < n; i++) {
            Step step = (Step) iter.next();
            out.print("<tr><td>");
            out.print(step.getUID());
            out.print("</td><td>");
            StepOptions options = step.getOptions();
            out.print(options.getSource());
            out.print("</td><td>");
            out.print(options.getTarget());
            out.print("</td><td>");
            out.print(options.getTicket());
            out.print("</td><td>");
            out.print(options.getPauseTime());
            out.print("</td><td>");
            out.print(options.getTimeoutTime());
            out.print("</td><td bgcolor=\"");
            StepStatus results = step.getStatus();
            switch (results.getState()) {
              case StepStatus.UNSEEN:
                out.print("white");
                break;
              case StepStatus.PAUSED:
              case StepStatus.RUNNING:
                out.print("yellow");
                break;
              case StepStatus.SUCCESS:
                out.print("green");
                break;
              case StepStatus.FAILURE:
              case StepStatus.TIMEOUT:
              default:
                out.print("red");
                break;
            }
            out.print("\">");
            out.print(results.getStateAsString());
            out.print("</td><td>");
            out.print(results.getStartTime());
            out.print("</td><td>");
            out.print(results.getEndTime());
            out.print("</td><td>");
            out.print(results.getMoveStatus());
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

        // allow user to remove an existing step
        out.print(
            "<p><hr><p>"+
            "<h2>Remove an existing Step:</h2>\n");
        n = ((c != null) ? c.size() : 0);
        boolean anyRemovable = false;
        if (n > 0) {
          Iterator iter = c.iterator();
          for (int i = 0; i < n; i++) {
            Step step = (Step) iter.next();
            StepOptions options = step.getOptions();
            if (!(agentId.equals(options.getSource()))) {
              // only allow removal of steps created by
              // this servlet or another local plugin
              continue;
            }
            UID uid = step.getUID();
            if (!(anyRemovable)) {
              anyRemovable = true;
              out.print(
                  "<select name=\""+
                  REMOVE_UID_PARAM+
                  "\">");
            }
            out.print("<option value=\"");
            out.print(uid);
            out.print("\">");
            out.print(uid);
            out.print("</option>");
          }
          if (anyRemovable) {
            out.print(
                "</select>"+
                "<input type=\"submit\" name=\""+
                ACTION_PARAM+
                "\" value=\""+
                REMOVE_VALUE+
                "\">\n");
          }
        }
        if (!(anyRemovable)) {
          out.print("<i>none</i>");
        }

        // allow user to submit a new Step request
        out.print(
            "<p><hr><p>"+
            "<h2>Create a new Step:</h2>\n");
        out.print(
            "<table>\n"+
            "<tr><td>"+
            "Actor Agent"+
            "</td><td>\n"+
            "<input type=\"text\" name=\""+
            ACTOR_AGENT_PARAM+
            "\" size=30 value=\"");
        out.print(
          (actorAgent != null) ?  actorAgent : agentId.toString());
        out.print(
            "\">"+
            "</td><td>"+
            "<i>(which agent should run the step)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Pause Time"+
            "</td><td>\n"+
            "<input type=\"text\" name=\""+
            PAUSE_TIME_PARAM+
            "\" size=30 value=\"");
        out.print(pauseTime > 0 ? pauseTime : 0);
        out.print(
            "\">"+
            "</td><td>"+
            "<i>(pause time <a href=\"#time\">notes</a>)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Timeout Time"+
            "</td><td>\n"+
            "<input type=\"text\" name=\""+
            TIMEOUT_TIME_PARAM+
            "\" size=30 value=\"");
        out.print(timeoutTime > 0 ? timeoutTime : 0);
        out.print(
            "\">"+
            "</td><td>"+
            "<i>(timeout time <a href=\"#time\">notes</a>)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Mobile Agent"+
            "</td><td>\n"+
            "<input type=\"text\" name=\""+
            MOBILE_AGENT_PARAM+
            "\" size=30 value=\"");
        out.print(
            mobileAgent != null ? mobileAgent : agentId.toString());
        out.print(
            "\">"+
            "</td><td>"+
            "<i>(which agent to move)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Origin Node"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            ORIGIN_NODE_PARAM+
            "\" size=30 value=\"");
        out.print(
            originNode != null ? originNode : nodeId.toString());
        out.print(
            "\">"+
            "</td><td>"+
            "<i>(where the agent should be now)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Destination Node"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            DEST_NODE_PARAM+
            "\" size=30 value=\"");
        out.print(
            destNode != null ? destNode : nodeId.toString());
        out.print(
            "\">"+
            "</td><td>"+
            "<i>(where the agent should be relocated)</i>"+
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
            "</select>"+
            "</td><td>"+
            "<i>(restart in place if already at"+
            " destination)</i>"+
            "</td></tr>\n"+
            "<tr><td colwidth=2>"+
            "<input type=\"submit\" name=\""+
            ACTION_PARAM+
            "\" value=\""+
            ADD_VALUE+
            "\">"+
            " &nbsp; "+
            "<input type=\"reset\">"+
            "</td></tr>\n"+
            "</table>\n");

        // footnotes
        out.print(
            "<p><hr><p>"+
            "<a name=\"time\"><br>"+
            "Time is either 0 or negative for none, +<i>N</i> for"+
            " N milliseconds after the submit button is pressed, or"+
            " <i>N</i> for absolute time.<br>"+
            "For example, +5000 is 5 seconds after the button press."+
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

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

package org.cougaar.core.examples.mobility.script;

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
 * mobility test procs.
 * <p>
 * The path of the servlet is "/move/proc".
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
 *       to be removed.  Any running procs are killed.
 *       </li><p>
 *   <li><tt>scriptUID=String</tt><br>
 *       If the action is "Add", this is the UID of the script.
 *       </li><p>
 * </ul>
 * <p>
 * Note the <b>SECURITY</b> issues of moving agents!
 */
public class ProcServlet
extends BaseServletComponent
implements BlackboardClient
{

  protected static final UnaryPredicate PROC_PRED =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Proc);
      }
    };

  protected static final UnaryPredicate SCRIPT_PRED =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Script);
      }
    };

  protected MessageAddress agentId;
  protected MessageAddress nodeId;

  protected BlackboardService blackboard;
  protected DomainService domain;
  protected LoggingService log;
  protected MobilityTestFactory mobilityTestFactory;

  protected String getPath() {
    return "/move/proc";
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

    // get the mobility test factory (for script creation)
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
      mobilityTestFactory = null;
    }
    if ((log != null) && (log != LoggingService.NULL)) {
      serviceBroker.releaseService(
          this, LoggingService.class, log);
      log = LoggingService.NULL;
    }
  }

  protected Collection queryScripts() {
    Collection ret = null;
    try {
      blackboard.openTransaction();
      ret = blackboard.query(SCRIPT_PRED);
    } finally {
      blackboard.closeTransactionDontReset();
    }
    return ret;
  }

  protected Collection queryProcs() {
    Collection ret = null;
    try {
      blackboard.openTransaction();
      ret = blackboard.query(PROC_PRED);
    } finally {
      blackboard.closeTransactionDontReset();
    }
    return ret;
  }

  protected Script queryScript(final UID uid) {
    if (uid == null) {
      throw new IllegalArgumentException("null uid");
    }
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        return 
          ((o instanceof Script) &&
           (uid.equals(((Script) o).getUID())));
      }
    };
    Script ret = null;
    try {
      blackboard.openTransaction();
      Collection c = blackboard.query(pred);
      if ((c != null) && (c.size() >= 1)) {
        ret = (Script) c.iterator().next();
      }
    } finally {
      blackboard.closeTransactionDontReset();
    }
    return ret;
  }

  protected Proc queryProc(final UID uid) {
    if (uid == null) {
      throw new IllegalArgumentException("null uid");
    }
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        return 
          ((o instanceof Proc) &&
           (uid.equals(((Proc) o).getUID())));
      }
    };
    Proc ret = null;
    try {
      blackboard.openTransaction();
      Collection c = blackboard.query(pred);
      if ((c != null) && (c.size() >= 1)) {
        ret = (Proc) c.iterator().next();
      }
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

  protected Proc createProc(UID scriptUID) {
    Proc proc = mobilityTestFactory.createProc(scriptUID);
    if (log.isInfoEnabled()) {
      log.info("Created new proc: "+proc);
    }
    return proc;
  }

  protected void addProc(Proc proc) {
    try {
      blackboard.openTransaction();
      blackboard.publishAdd(proc);
    } finally {
      blackboard.closeTransactionDontReset();
    }
  }

  protected void removeProc(Proc proc) {
    try {
      blackboard.openTransaction();
      blackboard.publishRemove(proc);
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

      // content:

      public static final String SCRIPT_UID_PARAM = "scriptUID";
      public UID scriptUID;

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

        // script uid:
        String s = request.getParameter(SCRIPT_UID_PARAM);
        if ((s != null) && (s.length() > 0)) {
          scriptUID = UID.toUID(s);
        }
      }

      private void writeResponse() throws IOException {
        if (ADD_VALUE.equals(action)) {
          try {
            Script script = queryScript(scriptUID);
            if (script == null) {
              throw new IllegalArgumentException(
                  "Unknown script with UID "+scriptUID);
            }
            Proc proc = createProc(scriptUID);
            addProc(proc);
          } catch (Exception e) {
            writeFailure(e);
            return;
          }
          writeSuccess();
        } else if (REMOVE_VALUE.equals(action)) {
          try {
            UID uid = UID.toUID(removeUID);
            Proc proc = queryProc(uid);
            if (proc != null) {
              removeProc(proc);
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
        out.print("<html><head><title>Proc ");
        out.print(agentId);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<h2>Proc ");
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

        out.print("<html><head><title>Failed proc for ");
        out.print(agentId);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<center><h1>Failed proc for ");
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
        out.print("<html><head><title>Proc ");
        out.print(agentId);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<center><h1>Submitted proc for ");
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
            "Local Procs:</h2><p>");
        // show current proc objects
        Collection procs = queryProcs();
        int nprocs = ((procs != null) ? procs.size() : 0);
        if (nprocs == 0) {
          out.print("<i>none</i>");
        } else {
          out.print(
              "<table border=1 cellpadding=1\n"+
              " cellspacing=1 width=95%\n"+
              " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
              "<tr><td>\n"+
              "<font size=+1 color=mediumblue><b>UID</b></font>"+
              "</td><td>\n"+
              "<font size=+1 color=mediumblue><b>Script</b></font>"+
              "</td><td>"+
              "<font size=+1 color=mediumblue><b>Start Time</b></font>"+
              "</td><td>"+
              "<font size=+1 color=mediumblue><b># Moves</b></font>"+
              "</td><td>"+
              "<font size=+1 color=mediumblue><b>End Time</b></font>"+
              "</td><td>"+
              "<font size=+1 color=mediumblue><b>Step Status</b></font>"+
              "</td><td>"+
              "<font size=+1 color=mediumblue><b>Step</b></font>"+
              "</td><td>"+
              "<font size=+1 color=mediumblue><b>Script Index</b></font>"+
              "</td></tr>\n");
          Iterator iter = procs.iterator();
          for (int i = 0; i < nprocs; i++) {
            Proc proc = (Proc) iter.next();
            out.print(
                "<tr><td>"+
                proc.getUID()+
                "</td><td>"+
                proc.getScriptUID()+ // FIXME href link?
                "</td><td>"+
                proc.getStartTime()+
                "</td><td>"+
                proc.getMoveCount()+
                "</td>");
            long endTime = proc.getEndTime();
            UID stepUID = proc.getStepUID();
            if ((endTime > 0) && (stepUID == null)) {
              out.print(
                  "<td>"+
                  endTime+
                  "</td><td colspan=3 bgcolor=\""+
                  "#BBFFBB"+ // green
                  "\">Completed</td>");
            } else {
              out.print(
                  "<td>"+
                  endTime+
                  "</td><td bgcolor=\"");
              String s;
              if (stepUID == null) {
                s = "#BBBBBB"+ // grey
                  "\">Null";
              } else {
                Step step = queryStep(stepUID);
                if (step == null) {
                  s = "#BBBBFF"+ // blue
                    "\">Not in blackboard!";
                } else {
                  StepStatus status = step.getStatus();
                  String color;
                  switch (status.getState()) {
                    case StepStatus.UNSEEN:
                      color = "#FFFFFF"; // white
                      break;
                    case StepStatus.PAUSED:
                      color = "#FFFFBB"; // yellow
                      break;
                    case StepStatus.RUNNING:
                      color = "#EFFFBB"; // yellow (green)
                      break;
                    case StepStatus.SUCCESS:
                      color = "#BBFFBB"; // green
                      break;
                    case StepStatus.FAILURE:
                    case StepStatus.TIMEOUT:
                    default:
                      color = "#FFBBBB"; // red
                      break;
                  }
                  s = color+"\">"+status.getStateAsString();
                }
              }
              out.print(
                  s+
                  "</td><td>"+
                  stepUID+ // FIXME href link?
                  "</td><td>"+
                  proc.getScriptIndex()+
                  "</td>");
            }
            out.print("</tr>\n");
          }
          out.print("</table>\n");
        }
        out.print(
            "<p><input type=\"submit\" name=\""+
            ACTION_PARAM+
            "\" value=\""+
            REFRESH_VALUE+
            "\">\n");

        // allow user to remove an existing proc
        out.print(
            "<p><hr><p>"+
            "<h2>Remove an existing Proc:</h2>\n");
        if (nprocs > 0) {
          out.print(
              "Proc UID: <select name=\""+
              REMOVE_UID_PARAM+
              "\">");
          Iterator iter = procs.iterator();
          for (int i = 0; i < nprocs; i++) {
            Proc proc = (Proc) iter.next();
            UID uid = proc.getUID();
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

        // allow user to submit a new Proc
        out.print(
            "<p><hr><p>"+
            "<h2>Create a new Proc:</h2>\n");
        Collection scripts = queryScripts();
        int nscripts = ((scripts != null) ? scripts.size() : 0);
        if (nscripts > 0) {
          out.print(
              "Script UID: <select name=\""+
              SCRIPT_UID_PARAM+
              "\">");
          Iterator iter = scripts.iterator();
          for (int i = 0; i < nscripts; i++) {
            Script script = (Script) iter.next();
            UID uid = script.getUID();
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
              ADD_VALUE+
              "\">\n");
        } else {
          out.print("<i>no scripts</i>");
        }

        out.print(
            "<p>"+
            "<input type=\"reset\">"+
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

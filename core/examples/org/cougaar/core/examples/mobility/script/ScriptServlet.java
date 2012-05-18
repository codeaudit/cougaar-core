/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.core.examples.mobility.script;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.UnaryPredicate;

/**
 * Servlet that allows the client to add, remove, and view
 * mobility test scripts.
 * <p>
 * The path of the servlet is "/move/script".
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
 *   <li><tt>content=String</tt><br>
 *       If the action is "Add", this is the content of the
 *       script.
 *       </li><p>
 *   <li><tt>file=String</tt><br>
 *       If the action is "Add", this is the file that contains
 *       the script.  If both "content" and "file" are specified,
 *       the "content" parameter is ignored.
 *       </li><p>
 * </ul>
 * <p>
 * Note the <b>SECURITY</b> issues of moving agents!
 */
public class ScriptServlet
extends BaseServletComponent
implements BlackboardClient
{

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
    return "/move/script";
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

  protected String readFile(String file) {
    // should use a config-finder service; for now this is fine:
    ConfigFinder configFinder = ConfigFinder.getInstance();
    StringBuffer ret = new StringBuffer();
    try {
      InputStream is = configFinder.open(file);
      BufferedReader br = 
        new BufferedReader(
            new InputStreamReader(is));
      char[] buf = new char[1024];
      while (true) {
        int len = br.read(buf);
        if (len < 0) {
          break;
        }
        ret.append(buf, 0, len);
      }
      br.close();
    } catch (IOException ioe) {
      throw new RuntimeException(
        "Unable to read file \""+file+"\"", ioe);
    }
    return ret.toString();
  }

  protected Script createScript(String content) {
    Script script = mobilityTestFactory.createScript(content);
    if (log.isInfoEnabled()) {
      log.info("Created new script: "+script);
    }
    return script;
  }

  protected void addScript(Script script) {
    try {
      blackboard.openTransaction();
      blackboard.publishAdd(script);
    } finally {
      blackboard.closeTransactionDontReset();
    }
  }

  protected void removeScript(Script script) {
    try {
      blackboard.openTransaction();
      blackboard.publishRemove(script);
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

      public static final String CONTENT_PARAM = "content";
      public String content;

      // file:

      public static final String FILE_PARAM = "file";
      public String file;

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

      private void parseParams() {
        // action:
        action = request.getParameter(ACTION_PARAM);

        // remove param:
        removeUID = 
          request.getParameter(REMOVE_UID_PARAM);
        if ((removeUID != null) && (removeUID.length() == 0)) {
          removeUID = null;
        }

        // content:
        content = 
          request.getParameter(CONTENT_PARAM);
        if ((content != null) && (content.length() == 0)) {
          content = null;
        }

        // file:
        file = 
          request.getParameter(FILE_PARAM);
        if ((file != null) && (file.length() == 0)) {
          file = null;
        }
      }

      private void writeResponse() throws IOException {
        if (ADD_VALUE.equals(action)) {
          try {
            String s = content;
            if (file != null) {
              s = readFile(file);
            }
            Script script = createScript(s);
            addScript(script);
          } catch (Exception e) {
            writeFailure(e);
            return;
          }
          writeSuccess();
        } else if (REMOVE_VALUE.equals(action)) {
          try {
            UID uid = UID.toUID(removeUID);
            Script script = queryScript(uid);
            if (script != null) {
              removeScript(script);
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
        out.print("<html><head><title>Script ");
        out.print(agentId);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<h2>Script ");
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

        out.print("<html><head><title>Failed script for ");
        out.print(agentId);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<center><h1>Failed script for ");
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
        out.print("<html><head><title>Script ");
        out.print(agentId);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<center><h1>Submitted script for ");
        out.print(agentId);
        out.print("</h1></center><p>\n");
        writeForm(out);
        out.print("</body></html>\n");
        out.close();
      }

      private void writeForm(
          PrintWriter out) {
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
            "Local Script:</h2><p>");
        // show current Script objects
        Collection c = queryScripts();
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
              "<td>\n"+
              "<font size=+1 color=mediumblue><b>Size</b></font>"+
              "</td>"+
              "<td>"+
              "<font size=+1 color=mediumblue><b>Text</b></font>"+
              "</td>"+
              "</tr>\n");
          Iterator iter = c.iterator();
          for (int i = 0; i < n; i++) {
            Script script = (Script) iter.next();
            int size = script.getSize();
            out.print(
                "<tr><td>"+
                script.getUID()+
                "</td><td>"+
                size+
                "</td><td><pre>\n");
            if (size > 0) {
              for (int j = 0; j < size; j++) {
                Script.Entry e = script.getEntry(j);
                // encode string?
                out.print(e+"\n");
              }
            } else {
              out.print("&nbsp;");
            }
            out.print("</pre></td></tr>\n");
          }
          out.print("</table>\n");
        }
        out.print(
            "<p><input type=\"submit\" name=\""+
            ACTION_PARAM+
            "\" value=\""+
            REFRESH_VALUE+
            "\">\n");

        // allow user to remove an existing script
        out.print(
            "<p><hr><p>"+
            "<h2>Remove an existing Script:</h2>\n");
        n = ((c != null) ? c.size() : 0);
        if (n > 0) {
          out.print(
              "<select name=\""+
              REMOVE_UID_PARAM+
              "\">");
          Iterator iter = c.iterator();
          for (int i = 0; i < n; i++) {
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
              REMOVE_VALUE+
              "\">\n");
        } else {
          out.print("<i>none</i>");
        }

        // allow user to submit a new Script
        out.print(
            "<p><hr><p>"+
            "<h2>Create a new Script:</h2>\n"+
            "File: <input type=\"text\" name=\""+
            FILE_PARAM+
            "\" size=60"+
            ((file != null) ? (" value=\""+file+"\"") : "")+
            "><p>"+
            "Or, instead, use the text below:<br>\n"+
            "<textarea name=\""+
            CONTENT_PARAM+
            "\" rows=30 cols=100>");
        if (content != null) {
          out.print(content);
        } else {
          out.print(
              "\n# See ScriptParser Javadocs!"+
              "\n"+
              "\n# Quick notes:"+
              "\n#   label name"+
              "\n#   move actor, [@+]pause, [@+]timeout, mobile, origin, dest, [true]"+
              "\n#   goto name"+
              "\n"+
              "\n# Quick examples:"+
              "\n# At system time 1018040407000 milliseconds, move"+
              "\n# agent X to node N, with no timeout:"+
              "\n#   move , 1018040407000, , X, , N,"+
              "\n# At 20 seconds after process creation, move Rover from"+
              "\n# BeginNode to EndNode, taking at most 60 seconds since "+
              "\n# the process start time:"+
              "\n#   move , @0:20.00, @0:60.00, Rover, BeginNode, EndNode,"+
              "\n# Equivalent shorthand:"+
              "\n#   move , @:20, @:60, Rover, BeginNode, EndNode,"+
              "\n# Have agent X ask agent A to restart in place right away:"+
              "\n#   move X, 0:00, 0:00, A, , , true"+
              "\n# Equivalent shorthand:"+
              "\n#   move X, , , A, , , true"+
              "\n# At 10 seconds after the prior move completes, move agent"+
              "\n# X to node N, don't restart if X is already on node N, and"+
              "\n# wait at most 1 minute 45 seconds for the move to complete:"+
              "\n#   move , +:10, +1:45, X, , N, false"+
              "\n");
        }
        out.print(
            "</textarea><p>\n"+
            "<input type=\"submit\" name=\""+
            ACTION_PARAM+
            "\" value=\""+
            ADD_VALUE+
            "\">"+
            " &nbsp; "+
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

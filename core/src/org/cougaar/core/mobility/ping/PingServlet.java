/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
 **/

package org.cougaar.core.mobility.ping;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

/**
 * Simple viewer of Pings created by the local agent.
 */
public class PingServlet 
extends BaseServletComponent 
implements BlackboardClient 
{

  public static final boolean DEFAULT_IGNORE_ROLLBACK = false;
  public static final int DEFAULT_LIMIT = 10;
  public static final int DEFAULT_SEND_FILLER_SIZE = -1;
  public static final int DEFAULT_ECHO_FILLER_SIZE = -1;
  public static final long DEFAULT_TIMEOUT = 30000L;

  public static final UnaryPredicate PING_PRED = 
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Ping);
      }
    };

  private String myPath = "/ping";

  private AgentIdentificationService agentIdService;
  private UIDService uidService;
  private BlackboardService blackboard;

  private MessageAddress agentId;

  public void setParameter(Object o) {
    myPath = (String) ((List) o).get(0);
  }

  protected String getPath() {
    return myPath;
  }

  protected Servlet createServlet() {
    return new MyServlet();
  }

  public void setAgentIdentificationService(
      AgentIdentificationService agentIdService) {
    this.agentIdService = agentIdService;
    agentId = agentIdService.getMessageAddress();
  }

  public void setBlackboardService(
      BlackboardService blackboard) {
    this.blackboard = blackboard;
  }

  public void setUIDService(UIDService uidService) {
    this.uidService = uidService;
  }

  public void unload() {
    if (agentIdService != null) {
      serviceBroker.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    if (uidService != null) {
      serviceBroker.releaseService(
          this, UIDService.class, uidService);
      uidService = null;
    }
    if (blackboard != null) {
      serviceBroker.releaseService(
          this, BlackboardService.class, blackboard);
      blackboard = null;
    }
    super.unload();
  }

  protected Collection queryAllPings() {
    Collection ret = null;
    try {
      blackboard.openTransaction();
      ret = blackboard.query(PING_PRED);
    } finally {
      blackboard.closeTransactionDontReset();
    }
    return ret;
  }

  protected Ping queryPing(final UID uid) {
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        return 
          ((o instanceof Ping) &&
           (uid.equals(((Ping) o).getUID())));
      }
    };
    Ping ret = null;
    try {
      blackboard.openTransaction();
      Collection c = blackboard.query(pred);
      if ((c != null) && (c.size() >= 1)) {
        ret = (Ping) c.iterator().next();
      }
    } finally {
      blackboard.closeTransactionDontReset();
    }
    return ret;
  }

  protected void addPing(Ping ping) {
    try {
      blackboard.openTransaction();
      blackboard.publishAdd(ping);
    } finally {
      blackboard.closeTransactionDontReset();
    }
  }

  protected void removePing(Ping ping) {
    try {
      blackboard.openTransaction();
      blackboard.publishRemove(ping);
    } finally {
      blackboard.closeTransactionDontReset();
    }
  }

  private class MyServlet extends HttpServlet {

    private static final String ACTION_PARAM = "action";
    private static final String ADD_VALUE = "Add";
    private static final String REMOVE_VALUE = "Remove";
    private static final String REFRESH_VALUE = "Refresh";

    private static final String TARGET_PARAM = "target";
    private static final String TIMEOUT_PARAM = "timeout";
    private static final String IGNORE_ROLLBACK_PARAM = "fail";
    private static final String LIMIT_PARAM = "limit";
    private static final String SEND_FILLER_SIZE_PARAM = "sendFiller";
    private static final String ECHO_FILLER_SIZE_PARAM = "echoFiller";

    private static final String REMOVE_UID_PARAM = "removeUID";

    public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {

      boolean isAdd = false;
      boolean isRemove = false;

      String target = null;
      long timeout = DEFAULT_TIMEOUT;
      boolean ignoreRollback = DEFAULT_IGNORE_ROLLBACK;
      int limit = DEFAULT_LIMIT;
      int sendFillerSize = DEFAULT_SEND_FILLER_SIZE;
      int echoFillerSize = DEFAULT_ECHO_FILLER_SIZE;
      String sremoveUID = null;
        
      // parse URL parameters
      {
        String action = req.getParameter(ACTION_PARAM);
        isAdd = ADD_VALUE.equals(action);
        isRemove = REMOVE_VALUE.equals(action);

        target = req.getParameter(TARGET_PARAM);
        if ((target != null) && (target.length() == 0)) {
          target = null;
        }
        String stimeout = req.getParameter(TIMEOUT_PARAM);
        if (stimeout != null) {
          timeout = Long.parseLong(stimeout);
        }
        String signoreRollback = req.getParameter(IGNORE_ROLLBACK_PARAM);
        ignoreRollback = 
          "on".equals(signoreRollback) || 
          "true".equals(signoreRollback);
        String slimit = req.getParameter(LIMIT_PARAM);
        if (slimit != null) {
          limit = Integer.parseInt(slimit);
        }
        String ssendFillerSize = req.getParameter(SEND_FILLER_SIZE_PARAM);
        if (ssendFillerSize != null) {
          sendFillerSize = Integer.parseInt(ssendFillerSize);
        }
        String sechoFillerSize = req.getParameter(ECHO_FILLER_SIZE_PARAM);
        if (sechoFillerSize != null) {
          echoFillerSize = Integer.parseInt(sechoFillerSize);
        }
        sremoveUID = req.getParameter("removeUID");
        if ((sremoveUID != null) && (sremoveUID.length() == 0)) {
          sremoveUID = null;
        }
      }

      res.setContentType("text/html");

      boolean isError = false;

      MessageAddress targetId = null;
      if (isAdd) {
        if (target == null) {
          isError = true;
        } else {
          targetId = MessageAddress.getMessageAddress(target);
          if (agentId.equals(targetId)) {
            isError = true;
          }
        }
      }

      Ping removePing = null;
      if (isRemove && (sremoveUID != null)) {
        UID uid = UID.toUID(sremoveUID);
        removePing = queryPing(uid);
        if (removePing == null) {
          isError = true;
        }
      }

      if (isError) {
        // use "setStatus" instead of "sendError" -- see bug 1259
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }

      PrintWriter out = res.getWriter();
      Date nowDate = new Date();
      long nowTime = nowDate.getTime();
      out.print(
          "<html><head><title>"+
          "Agent "+
          agentId+
          " Ping Viewer"+
          "</title></head><body>\n"+
          "<h1>Agent "+
          agentId+
          " Ping Viewer</h1>\n"+
          "<p>"+
          "Time: "+
          nowDate+" ("+
          nowTime+
          ")"+
          "<p>"+
          "<form method=\"GET\" action=\"");
      out.print(req.getRequestURI());
      out.print(
          "\">\n"+
          "<p>");

      if (isAdd) {

        if (targetId == null) {
          out.print("Please specify a target agent");
          out.print("</body></html>");
          out.close();
          return;
        } else if (agentId.equals(targetId)) {
          out.print("Target can't equal local agent "+agentId);
          out.print("</body></html>");
          out.close();
          return;
        }

        UID uid = uidService.nextUID();
        Ping ping = new PingImpl(
            uid, agentId, targetId, timeout,
            ignoreRollback, limit, sendFillerSize, echoFillerSize);
        addPing(ping);

        out.print(
            "<hr>"+
            "Added new ping from "+agentId+" to "+targetId+" with UID "+uid+
            "<p>");
      }

      if (isRemove) {

        if (sremoveUID == null) {
          out.print("Please select a UID to remove");
          out.print("</body></html>");
          out.close();
          return;
        } else if (removePing == null) {
          out.print("Unknown ping "+sremoveUID);
          out.print("</body></html>");
          out.close();
          return;
        }

        removePing(removePing);
        out.print(
            "<hr>"+
            "Remove ping "+sremoveUID+
            "<p>");
      }

      out.print(
          "<h2>Existing Pings from Agent "+
          agentId+
          ":</h2>"+
          "<table border=1>\n"+
          "<tr><th rowspan=2>"+
          "</th><th rowspan=2>UID"+
          "</th><th rowspan=2>Status"+
          "</th><th rowspan=2>Time"+
          "</th><th rowspan=2>Count"+
          "</th><th colspan=7>Configuration"+
          "</th></tr>\n"+
          "<tr><th>Source"+
          "</th><th>Target"+
          "</th><th>Timeout"+
          "</th><th>IgnoreRollback"+
          "</th><th>Limit"+
          "</th><th>SendFillerSize"+
          "</th><th>EchoFillerSize"+
          "</th></tr>\n");

      Collection c = queryAllPings();
      int n = ((c != null) ? c.size() : 0);
      if (n > 0) {
        Iterator iter = c.iterator();
        for (int i = 0; i < n; i++) {
          Ping ping = (Ping) iter.next();
          if (!(agentId.equals(ping.getSource()))) {
            continue;
          }
          out.print(
              "<tr><td>"+
              i+
              "</td><td>"+
              ping.getUID()+
              "</td>");
          String em = ping.getError();
          int plimit = ping.getLimit();
          boolean finished = 
            ((em == null) &&
             (plimit > 0) &&
             (ping.getSendCount() >= plimit));
          if (em == null) {
            if (finished) {
              out.print(
                  "<td bgcolor=\"#BBFFBB\">Finished all "+
                  plimit+" pings</td>");
            } else {
              out.print("<td bgcolor=\"#FFFFBB\">Running</td>");
            }
          } else {
            out.print("<td bgcolor=\"#FFBBBB\">"+em+"</td>");
          }
          long st = ping.getSendTime();
          if (st > 0) {
            long rt = ping.getReplyTime();
            if (rt > 0) {
              out.print("<td bgcolor=\"BBFFBB\">"+(rt-st)+"</td>");
            } else if (finished) {
              out.print("<td bgcolor=\"BBFFBB\">? finished</td>");
            } else {
              out.print(
                  "<td bgcolor=\"FFFFBB\">"+(nowTime-st)+
                  "+ <b>?</b></td>");
            }
          } else {
            out.print("<td>N/A</td>");
          }
          out.print(
              "<td>"+
              ping.getSendCount()+
              "</td><td>"+
              ping.getSource()+
              "</td><td>"+
              ping.getTarget()+
              "</td><td>"+
              ping.getTimeoutMillis()+
              "</td><td>"+
              ping.isIgnoreRollback()+
              "</td><td>"+
              ping.getLimit()+
              "</td><td>"+
              ping.getSendFillerSize()+
              "</td><td>"+
              ping.getEchoFillerSize()+
              "</td>"+
              "</tr>\n");
        }
      }
      out.print(
          "</table>\n"+
          "<input type=\"submit\" name=\""+
          ACTION_PARAM+
          "\" value=\""+
          REFRESH_VALUE+
          "\">");

      // allow user to remove an existing ping
      out.print(
          "<p><hr><p>"+
          "<h2>Remove an existing Ping:</h2>\n");
      if (n > 0) {
        out.print(
            "<select name=\""+
            REMOVE_UID_PARAM+
            "\">");
        Iterator iter = c.iterator();
        for (int i = 0; i < n; i++) {
          Ping ping = (Ping) iter.next();
          UID uid = ping.getUID();
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

      out.print(
          "<hr>"+
          "<h2>Add new Ping:</h2>"+
          "<table>"+
          "<tr><td>"+
          "Target Agent</td><td><input name=\""+
          TARGET_PARAM+
          "\" type=\"text\""+
          " size=30"+
          ((target != null) ? (" value=\""+target+"\"") : "")+
          "></td><td><i>Destination agent, which can't be "+
          agentId+
          "</i></td></tr>\n"+
          "<tr><td>Timeout Millis</td><td><input name=\""+
          TIMEOUT_PARAM+
          "\" type=\"text\" size=30 value=\""+
          timeout+
          "\"></td><td><i>Milliseconds until timeout, or -1 "+
          "if no timeout, default is "+
          DEFAULT_TIMEOUT+
          "</i></td></tr>\n"+
          "<tr><td>Ignore Rollback</td><td><input name=\""+
          IGNORE_ROLLBACK_PARAM+
          "\" type=\"checkbox\""+
          (ignoreRollback ? " checked" : "")+
          "></td><td><i>Ignore ping counter failures, perhaps due to agent restarts"+
          ", default is "+
          DEFAULT_IGNORE_ROLLBACK+
          "</i></td></tr>\n"+
          "<tr><td>Repeat Limit</td><td><input name=\""+
          LIMIT_PARAM+
          "\" type=\"text\" size=30 value=\""+
          limit+
          "\"></td><td><i>Number of pings to send, or -1 for"+
          " infinite, default is "+
          DEFAULT_LIMIT+
          "</i></td></tr>\n"+
          "<tr><td>Send Filler Size</td><td><input name=\""+
          SEND_FILLER_SIZE_PARAM+
          "\" type=\"text\" size=30 value=\""+
          sendFillerSize+
          "\"></td><td><i>Extra \"filler\" bytes to make send-side"+
          " ping messages larger, or -1 for"+
          " no send-size filler, default is "+
          DEFAULT_SEND_FILLER_SIZE+
          "</i></td></tr>\n"+
          "<tr><td>Echo Filler Size</td><td><input name=\""+
          ECHO_FILLER_SIZE_PARAM+
          "\" type=\"text\" size=30 value=\""+
          echoFillerSize+
          "\"></td><td><i>Extra \"filler\" bytes to make target-size"+
          " ping messages larger, or -1 for"+
          " no target-side filler, default is "+
          DEFAULT_ECHO_FILLER_SIZE+
          "</i></td></tr>\n"+
          "</table>\n"+
          "<input type=\"submit\" name=\""+
          ACTION_PARAM+
          "\" value=\""+
          ADD_VALUE+
          "\">\n"+
          " &nbsp; "+
          "<input type=\"submit\" name=\""+
          ACTION_PARAM+
          "\" value=\""+
          REFRESH_VALUE+
          "\">"+
          "</form>\n");

      out.print(
          "</body></html>\n");
      out.close();
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

}

/*
 *
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
 
package org.cougaar.planning.servlet;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.DecimalFormat;

import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.planning.servlet.data.Failure;
import org.cougaar.planning.servlet.data.xml.*;
import org.cougaar.planning.servlet.data.completion.*;

/**
 * A <code>Servlet</code>, loaded by the 
 * <code>SimpleServletComponent</code>, that generates 
 * HTML, XML, and serialized-Object views of Task completion
 * information.
 *
 * @see org.cougaar.core.servlet.SimpleServletComponent
 */
public class CompletionServlet
extends HttpServlet
{
  private SimpleServletSupport support;

  public void setSimpleServletSupport(SimpleServletSupport support) {
    this.support = support;
  }

  public void doGet(
      HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException
  {
    // create a new "Completor" context per request
    Completor ct = new Completor(support, request, response);
    ct.execute();    
  }


  public void doPost(
      HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException
  {
    // create a new "Completor" context per request
    Completor ct = new Completor(support, request, response);
    ct.execute();  
  }

  /** 
   * Inner-class to hold state and generate the response.
   */
  protected static class Completor {

    public static final int FORMAT_DATA = 0;
    public static final int FORMAT_XML = 1;
    public static final int FORMAT_HTML = 2;

    private boolean anyArgs;
    private int format;
    private boolean showTables;

    // since "Completor" is a static inner class, here
    // we hold onto the support API.
    //
    // this makes it clear that Completor only uses
    // the "support" from the outer class.
    private SimpleServletSupport support;
    private HttpServletRequest request;
    private HttpServletResponse response;

    // writer from the request for HTML output
    private PrintWriter out;

    // base url
    private String baseURL;

    public Completor(SimpleServletSupport support,
                     HttpServletRequest request, 
                     HttpServletResponse response)
    {
      this.support = support;
      this.request = request;
      this.response = response;
      format = FORMAT_HTML;
    }         

    public void execute() throws IOException, ServletException 
    {
      // save the absolute address
      this.baseURL =   
        request.getScheme()+
        "://"+
        request.getServerName()+
        ":"+
        request.getServerPort()+
        "/";

      // create a URL parameter visitor
      ServletUtil.ParamVisitor vis = 
        new ServletUtil.ParamVisitor() {
          public void setParam(String name, String value) {
            if (eq("format", name)) {
              anyArgs = true;
              if (eq("data", value)) {
                format = FORMAT_DATA;
              } else if (eq("xml", value)) {
                format = FORMAT_XML;
              } else if (eq("html", value)) {
                format = FORMAT_HTML;
              }
            } else if (eq("showTables", name)) {
              anyArgs = true;
              showTables =
                ((value == null) || 
                 (eq("true", value)));
              // stay backwards-compatable
            } else if (eq("data", name)) {
              anyArgs = true;
              format = FORMAT_DATA;
            } else if (eq("xml", name)) {
              anyArgs = true;
              format = FORMAT_XML;
            } else if (eq("html", name)) {
              anyArgs = true;
              format = FORMAT_HTML;
            }
          }
        };

      // visit the URL parameters
      ServletUtil.parseParams(vis, request);
      String viewType = request.getParameter("viewType");
      if ("viewAllAgents".equals(viewType)) {
        viewAllAgents();
        return;
      }
      if ("viewTitle".equals(viewType)) {
        viewTitle();
        return;
      }
      if ("viewAgentSmall".equals(viewType)) {
        viewAgentSmall();
        return;
      }
      if ("viewMoreLink".equals(viewType)) {
        viewMoreLink();
        return;
      }

      // get result
      CompletionData result = getCompletionData();

      // write data
      try {
        if (format == FORMAT_HTML) {
          // html      
          this.out = response.getWriter();
          printCompletionDataAsHTML(result);
        } else {
          // unsupported
          OutputStream out = response.getOutputStream();
          if (format == FORMAT_DATA) {      
            // serialize
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(result);
            oos.flush();
          } else {
            // xml
            out.write(("<?xml version='1.0'?>\n").getBytes());
            XMLWriter w =
              new XMLWriter(
                  new OutputStreamWriter(out));
            result.toXML(w);
            w.flush();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private static String[] iframeBrowsers = {
      "mozilla/5",
      "msie 5",
      "msie 6"
    };

    private static final int MAX_AGENT_FRAMES = 17;

    private void viewMoreLink() throws IOException {
      response.setContentType("text/html");
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      out.print
        ("<html>\n" +
         "<head>\n" +
         "<title>Completion of More Agents</title>\n" +
         "</head>\n");
      out.println("<body>");
      String firstAgent = request.getParameter("firstAgent");
      if (firstAgent != null) {
        out.println("<A href=\"completion?viewType=viewAllAgents&firstAgent=" + firstAgent + "\" + target=\"_top\">");
        out.println("<h2><center>More Agents</h2></center>");
        out.println("</A>");
      }
      out.println("</body>");
      out.println("</html>");
    }

    private void viewTitle() throws IOException {
      String title = request.getParameter("title");
      response.setContentType("text/html");
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      out.print
        ("<html>\n" +
         "<head>\n" +
         "<title>" + title + "</title>\n" +
         "</head>\n");
      out.println("<body>");
      out.println("<h2><center>" + title + "</h2></center>");
      out.println("</body>");
      out.println("</html>");
    }

    // Output a page showing summary info for all agents
    private void viewAllAgents() throws IOException {
      response.setContentType("text/html");
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      List agents = support.getAllEncodedAgentNames();
      out.print
        ("<html>\n" +
         "<head>\n" +
         "<title>Completion of All Agents</title>\n" +
         "</head>\n");
      boolean use_iframes = false;
      String browser = request.getHeader("user-agent").toLowerCase();
      if (browser != null) {
        for (int i = 0; i < iframeBrowsers.length; i++) {
          if (browser.indexOf(iframeBrowsers[i]) >= 0) {
            use_iframes = true;
            break;
          }
        }
      }
      if (use_iframes) {
        out.println("<body>");
        out.println("<h2><center>Completion of All Agents</h2></center>");
        for (int i = 0, n = agents.size(); i < n; i++) {
          int col = i % 3;
          String agentName = (String) agents.get(i);
          out.println("<iframe src=\"/$" + agentName + "/completion?viewType=viewAgentSmall\" scrolling=\"no\" width=300 height=90>" + agentName + "</iframe>");
          out.println("</td>");
        }
        out.println("</body>");
      } else {
        int totalAgents = agents.size();
        int nagents = totalAgents;
        String firstAgent = request.getParameter("firstAgent");
        int agent0 = 0;
        boolean needMore = false;
        String title = "All Agents";
        if (firstAgent != null) {
          try {
            agent0 = Integer.parseInt(firstAgent);
            if (agent0 < 0) {
              agent0 = 0;
            } else if (agent0 >= nagents) {
              agent0 = nagents - MAX_AGENT_FRAMES;
            }
            nagents -= agent0;
          } catch (Exception e) {
          }
        }
        if (nagents > MAX_AGENT_FRAMES) {
          needMore = true;
          nagents = MAX_AGENT_FRAMES;
        }
        if (agent0 > 0 || nagents < totalAgents) {
          title =
            "Agents+" + ((String) agents.get(agent0)) +
            "+Through+" + ((String) agents.get(agent0 + nagents - 1));
        }
        int nrows = (nagents + 2 + (needMore ? 1 : 0)) / 3;
        out.print("<frameset rows=\"40");
        for (int row = 0; row < nrows; row++) {
          out.print(",100");
        }
        out.println("\">");
        out.println("  <frame src=\"completion?viewType=viewTitle&title=Completion+of+" + title + "\" scrolling=\"no\">");
        for (int row = 0; row < nrows; row++) {
          out.println("  <frameset cols=\"300,300,300\">");
          for (int col = 0; col < 3; col++) {
            int agentn = agent0 + row * 3 + col;
            if (agentn < agent0 + nagents) {
              String agentName = (String) agents.get(agentn);
              out.println("    <frame src=\"/$" + agentName + "/completion?viewType=viewAgentSmall\" scrolling=\"no\">");
            } else if (agentn == agent0 + nagents && needMore) {
              out.println("    <frame src=\"completion?viewType=viewMoreLink&firstAgent=" + (agent0 + MAX_AGENT_FRAMES) + "\" scrolling=\"no\">");
            }
          }
          out.println("  </frameset>");
        }
        out.println("</frameset>");
      }
      out.println("<html>");
    }

    // Output a small page showing summary info for one agent
    private void viewAgentSmall() throws IOException {
      response.setContentType("text/html");
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      String agent = support.getEncodedAgentName();
      CompletionData result = getCompletionData();
      int nTasks = result.getNumberOfTasks();
      int nUnplannedTasks = result.getNumberOfUnplannedTasks();
      int nPlannedTasks = (nTasks - nUnplannedTasks);
      double percentPlannedTasks =
        ((nTasks > 0) ? 
         (1.0 * nPlannedTasks) / nTasks :
         0.0);
      int nUnestimatedTasks = result.getNumberOfUnestimatedTasks();
      int nEstimatedTasks = (nPlannedTasks - nUnestimatedTasks);
      double percentEstimatedTasks =
        ((nPlannedTasks > 0) ? 
         (1.0 * nEstimatedTasks) / nPlannedTasks :
         0.0);
      int nFailedTasks = result.getNumberOfFailedTasks();
      int nSuccessfulTasks = (nEstimatedTasks - nFailedTasks);
      double percentSuccessfulTasks =
        ((nEstimatedTasks > 0) ? 
         (1.0 * nSuccessfulTasks) / nEstimatedTasks :
         0.0);
      int nUnconfidentTasks = result.getNumberOfUnconfidentTasks();
      int nFullConfidenceTasks = (nSuccessfulTasks - nUnconfidentTasks);
      double percentFullConfidenceTasks =
        ((nSuccessfulTasks > 0) ? 
         (1.0 * nFullConfidenceTasks) / nSuccessfulTasks :
         0.0);
      String bgcolor, fgcolor, lncolor;
      if (percentPlannedTasks < 0.89 ||
          percentSuccessfulTasks < 0.89 ||
          percentFullConfidenceTasks < 0.89) {
        bgcolor = "#aa0000";
        fgcolor = "#ffffff";
        lncolor = "#ffff00";
      } else if (percentPlannedTasks < 0.99 ||
                 percentSuccessfulTasks < 0.99 ||
                 percentFullConfidenceTasks < 0.99) {
        bgcolor = "#ffff00";
        fgcolor = "#000000";
        lncolor = "#0000ff";
      } else {
        bgcolor = "white";
        fgcolor = "black";
        lncolor = "#0000ff";
      }
      out.println("<html>");
      out.println("<head>");
      out.println("</head>");
      out.println("<body bgcolor=\"" + bgcolor + "\" text=\"" + fgcolor + "\" vlink=\"" + lncolor + "\" link=\"" + lncolor + "\">");
      out.print("<pre><a href=\"/$" + agent + "/completion\" target=\"_top\">");
      out.println(formatLabel(agent + " Tasks:") + "</a>" + formatInteger(nTasks));
      out.println(formatLabel("Planned:")        + formatInteger(nPlannedTasks)        + "(" + formatPercent(percentPlannedTasks)        + ")");
      out.println(formatLabel("Successful:")     + formatInteger(nSuccessfulTasks)     + "(" + formatPercent(percentSuccessfulTasks)     + ")");
      out.println(formatLabel("Completed:")      + formatInteger(nFullConfidenceTasks) + "(" + formatPercent(percentFullConfidenceTasks) + ")</pre>");
      out.println("</body>");
      out.println("</html>");
    }

    private static String formatLabel(String lbl) {
      int nchars = lbl.length();
      if (nchars > 24) return lbl;
      return lbl + "                        ".substring(nchars);
    }

    private static String formatInteger(int n) {
      return formatInteger(n, 5);
    }

    private static String formatInteger(int n, int w) {
      String r = String.valueOf(n);
      return "        ".substring(0, w - r.length()) + r;
    }

    private static String formatPercent(double percent) {
      return formatInteger((int) (percent * 100.0), 3) + "%";
    }

    // startsWithIgnoreCase
    private static final boolean eq(String a, String b) {
      return a.regionMatches(true, 0, b, 0, a.length());
    }

    protected static final UnaryPredicate TASK_PRED =
      new UnaryPredicate() {
        public boolean execute(Object o) {
          return (o instanceof Task);
        }
      };


    protected Collection getAllTasks() {
      Collection col = support.queryBlackboard(TASK_PRED);
      if (col == null) {
        col = new ArrayList(0);
      }
      return col;
    }

    protected CompletionData getCompletionData() {
      // get tasks
      Collection tasks = getAllTasks();
      long nowTime = System.currentTimeMillis();
      int nTasks = tasks.size();
      Iterator taskIter = tasks.iterator();
      if (showTables) {
        // create and initialize our result
        FullCompletionData result = new FullCompletionData();
        result.setNumberOfTasks(nTasks);
        result.setTimeMillis(nowTime);
        // examine tasks
        for (int i = 0; i < nTasks; i++) {
          Task ti = (Task)taskIter.next();
          PlanElement pe = ti.getPlanElement();
          if (pe != null) {
            AllocationResult peEstResult = pe.getEstimatedResult();
            if (peEstResult != null) {
              double estConf = peEstResult.getConfidenceRating();
              if (peEstResult.isSuccess()) {
                if (estConf > 0.99) {
                  // 100% success
                } else {
                  result.addUnconfidentTask(
                      makeUnconfidentTask(estConf, ti));
                }
              } else {
                result.addFailedTask(makeFailedTask(estConf, ti));
              }
            } else {
              result.addUnestimatedTask(makeUnestimatedTask(ti));
            }
          } else {
            result.addUnplannedTask(makeUnplannedTask(ti));
          }
        }
        return result;
      } else {
        // create and initialize our result
        SimpleCompletionData result = new SimpleCompletionData();
        result.setNumberOfTasks(nTasks);
        result.setTimeMillis(nowTime);
        // examine tasks
        int nUnplannedTasks = 0;
        int nUnestimatedTasks = 0;
        int nFailedTasks = 0;
        int nUnconfidentTasks = 0;
        for (int i = 0; i < nTasks; i++) {
          Task ti = (Task)taskIter.next();
          PlanElement pe = ti.getPlanElement();
          if (pe != null) {
            AllocationResult peEstResult = pe.getEstimatedResult();
            if (peEstResult != null) {
              double estConf = peEstResult.getConfidenceRating();
              if (peEstResult.isSuccess()) {
                if (estConf > 0.99) {
                  // 100% success
                } else {
                  nUnconfidentTasks++;
                }
              } else {
                nFailedTasks++;
              }
            } else {
              nUnestimatedTasks++;
            }
          } else {
            nUnplannedTasks++;
          }
        }
        result.setNumberOfUnplannedTasks(nUnplannedTasks);
        result.setNumberOfUnestimatedTasks(nUnestimatedTasks);
        result.setNumberOfFailedTasks(nFailedTasks);
        result.setNumberOfUnconfidentTasks(nUnconfidentTasks);
        return result;
      }
    }

    /**
     * Create an <code>UnplannedTask</code> for the given <code>Task</code>.
     */
    protected UnplannedTask makeUnplannedTask(Task task) {
      UnplannedTask upt = new UnplannedTask();
      fillAbstractTask(upt, task);
      // leave confidence as 0%
      return upt;
    }

    /**
     * Create an <code>UnestimatedTask</code> for the given <code>Task</code>.
     */
    protected UnestimatedTask makeUnestimatedTask(Task task) {
      UnestimatedTask uet = new UnestimatedTask();
      fillAbstractTask(uet, task);
      // leave confidence as 0%
      return uet;
    }

    /**
     * Create an <code>UnconfidentTask</code> for the given <code>Task</code>.
     *
     * @param confidence a double &gt;= 0.0 and &lt; 1.0
     */
    protected UnconfidentTask makeUnconfidentTask(double confidence, Task task) {
      UnconfidentTask uct = new UnconfidentTask();
      fillAbstractTask(uct, task);
      uct.setConfidence(confidence);
      return uct;
    }

    /**
     * Create a <code>FailedTask</code> for the given <code>Task</code>.
     */
    protected FailedTask makeFailedTask(double confidence, Task task) {
      FailedTask ft = new FailedTask();
      fillAbstractTask(ft, task);
      ft.setConfidence(confidence);
      return ft;
    }

    /**
     * Fill an <code>AbstractTask</code> for the given <code>Task</code>,
     * which will grab:<pre>
     *   the UID, 
     *   TASK.PSP's URL for that UID,
     *   the ParentUID, 
     *   TASK.PSP's URL for that ParentUID,
     *   a String description of the PlanElement</pre>.
     */
    protected void fillAbstractTask(AbstractTask toAbsTask, Task task) {

      // set task UID
      UID taskUID = ((task != null) ? task.getUID() : null);
      String sTaskUID = ((taskUID != null) ? taskUID.toString() : null);
      if (sTaskUID == null) {
        return;
      }
      toAbsTask.setUID(sTaskUID);
      String sourceClusterId = 
        support.encodeAgentName(task.getSource().getAddress());
      toAbsTask.setUID_URL(
          getTaskUID_URL(support.getEncodedAgentName(), sTaskUID));
      // set parent task UID
      UID pTaskUID = task.getParentTaskUID();
      String spTaskUID = ((pTaskUID != null) ? pTaskUID.toString() : null);
      if (spTaskUID != null) {
        toAbsTask.setParentUID(spTaskUID);
        toAbsTask.setParentUID_URL(
            getTaskUID_URL(sourceClusterId, spTaskUID));
      }
      // set plan element
      toAbsTask.setPlanElement(getPlanElement(task.getPlanElement()));
      // set verb
      toAbsTask.setVerb(task.getVerb().toString());
    }

    /**
     * Get the TASKS.PSP URL for the given UID String.
     *
     * Assumes that the TASKS.PSP URL is fixed at "/tasks".
     */
    protected String getTaskUID_URL(
        String clusterId, String sTaskUID) {
      return 
        //baseURL+  // FIXME prefix with base URL?
        "/$"+
        clusterId+
        "/tasks?mode=3&uid="+
        sTaskUID;
    }

    /**
     * Get a brief description of the given <code>PlanElement</code>.
     */
    protected String getPlanElement(
        PlanElement pe) {
      return 
        (pe instanceof Allocation) ?
        "Allocation" :
        (pe instanceof Expansion) ?
        "Expansion" :
        (pe instanceof Aggregation) ?
        "Aggregation" :
        (pe instanceof Disposition) ?
        "Disposition" :
        (pe instanceof AssetTransfer) ?
        "AssetTransfer" :
        (pe != null) ?
        pe.getClass().getName() : 
        null;
    }

    /**
     * Write the given <code>CompletionData</code> as formatted HTML.
     */
    protected void printCompletionDataAsHTML(CompletionData result) {
      // javascript based on PlanViewServlet
      out.print(
          "<html>\n"+
          "<script language=\"JavaScript\">\n"+
          "<!--\n"+
          "function mySubmit() {\n"+
          "  var tidx = document.myForm.formCluster.selectedIndex\n"+
          "  var cluster = document.myForm.formCluster.options[tidx].text\n"+
          "  document.myForm.action=\"/$\"+cluster+\"");
      out.print(support.getPath());
      out.print("\"\n"+
          "  return true\n"+
          "}\n"+
          "// -->\n"+
          "</script>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          "</title>"+
          "</head>\n"+
          "<body>"+
          "<h2><center>Completion at ");
      out.print(support.getEncodedAgentName());
      out.print(
          "</center></h2>\n"+
          "<form name=\"myForm\" method=\"get\" "+
          "onSubmit=\"return mySubmit()\">\n"+
          "Completion data at "+
          "<select name=\"formCluster\">\n");
      // lookup all known cluster names
      List names = support.getAllEncodedAgentNames();
      int sz = names.size();
      for (int i = 0; i < sz; i++) {
        String n = (String) names.get(i);
        out.print("  <option ");
        if (n.equals(support.getEncodedAgentName())) {
          out.print("selected ");
        }
        out.print("value=\"");
        out.print(n);
        out.print("\">");
        out.print(n);
        out.print("</option>\n");
      }
      out.print(
          "</select>, \n"+
          "<input type=\"checkbox\" name=\"showTables\" value=\"true\" ");
      if (showTables) {
        out.print("checked");
      }
      out.print("> show table, \n"+
          "<input type=\"submit\" name=\"formSubmit\" value=\"Reload\"><br>\n"+
          "</form>\n");
      printCountersAsHTML(result);
      printTablesAsHTML(result);
      out.print("</body></html>");
      out.flush();
    }

    protected void printCountersAsHTML(CompletionData result) {
      out.print(
          "<pre>\n"+
          "Time: <b>");
      long timeMillis = result.getTimeMillis();
      out.print(new Date(timeMillis));
      out.print("</b>   (");
      out.print(timeMillis);
      out.print(
          " MS)"+
          "\n\nNumber of Tasks: <b>");
      int nTasks = result.getNumberOfTasks();
      out.print(nTasks);
      out.print("\n</b>Subset of Tasks[");
      out.print(nTasks);
      out.print("] planned (non-null PlanElement): <b>");
      int nUnplannedTasks = result.getNumberOfUnplannedTasks();
      int nPlannedTasks = (nTasks - nUnplannedTasks);
      out.print(nPlannedTasks);
      out.print("</b>  (<b>");
      double percentPlannedTasks =
        ((nTasks > 0) ? 
         (100.0 * (((double)nPlannedTasks) / nTasks)) :
         0.0);
      out.print(percentPlannedTasks);
      out.print(
          " %</b>)"+
          "\nSubset of planned[");
      out.print(nPlannedTasks);
      out.print("] estimated (non-null EstimatedResult): <b>");
      int nUnestimatedTasks = result.getNumberOfUnestimatedTasks();
      int nEstimatedTasks = (nPlannedTasks - nUnestimatedTasks);
      out.print(nEstimatedTasks);
      out.print("</b>  (<b>");
      double percentEstimatedTasks =
        ((nPlannedTasks > 0) ? 
         (100.0 * (((double)nEstimatedTasks) / nPlannedTasks)) :
         0.0);
      out.print(percentEstimatedTasks);
      out.print(
          " %</b>)"+
          "\nSubset of estimated[");
      out.print(nEstimatedTasks);
      out.print("] that are estimated successful: <b>");
      int nFailedTasks = result.getNumberOfFailedTasks();
      int nSuccessfulTasks = (nEstimatedTasks - nFailedTasks);
      out.print(nSuccessfulTasks);
      out.print("</b>  (<b>");
      double percentSuccessfulTasks =
        ((nEstimatedTasks > 0) ? 
         (100.0 * (((double)nSuccessfulTasks) / nEstimatedTasks)) :
         0.0);
      out.print(percentSuccessfulTasks);
      out.print(
          " %</b>)"+
          "\nSubset of estimated successful[");
      out.print(nSuccessfulTasks);
      out.print("] with 100% confidence: <b>");
      int nUnconfidentTasks = result.getNumberOfUnconfidentTasks();
      int nFullConfidenceTasks = (nSuccessfulTasks - nUnconfidentTasks);
      out.print(nFullConfidenceTasks);
      out.print("</b>  (<b>");
      double percentFullConfidenceTasks =
        ((nSuccessfulTasks > 0) ? 
         (100.0 * (((double)nFullConfidenceTasks) / nSuccessfulTasks)) :
         0.0);
      out.print(percentFullConfidenceTasks);
      out.print(" %</b>)\n");
      out.print("</pre>\n");
    }

    protected void printTablesAsHTML(CompletionData result) {
      if (result instanceof FullCompletionData) {
        int nUnplannedTasks = result.getNumberOfUnplannedTasks();
        beginTaskHTMLTable(
            ("Unplanned Tasks["+nUnplannedTasks+"]"),
            "(PlanElement == null)");
        for (int i = 0; i < nUnplannedTasks; i++) {
          printAbstractTaskAsHTML(i, result.getUnplannedTaskAt(i));
        }
        endTaskHTMLTable();
        int nUnestimatedTasks = result.getNumberOfUnestimatedTasks();
        beginTaskHTMLTable(
            ("Unestimated Tasks["+nUnestimatedTasks+"]"),
            "(Est. == null)");
        for (int i = 0; i < nUnestimatedTasks; i++) {
          printAbstractTaskAsHTML(i, result.getUnestimatedTaskAt(i));
        }
        endTaskHTMLTable();
        int nFailedTasks = result.getNumberOfFailedTasks();
        beginTaskHTMLTable(
            ("Failed Tasks["+nFailedTasks+"]"),
            "(Est.isSuccess() == false)");
        for (int i = 0; i < nFailedTasks; i++) {
          printAbstractTaskAsHTML(i, result.getFailedTaskAt(i));
        }
        endTaskHTMLTable();
        int nUnconfidentTasks = result.getNumberOfUnconfidentTasks();
        beginTaskHTMLTable(
            ("Unconfident Tasks["+nUnconfidentTasks+"]"),
            "((Est.isSuccess() == true) &amp;&amp; (Est.Conf. < 100%))");
        for (int i = 0; i < nUnconfidentTasks; i++) {
          printAbstractTaskAsHTML(i, result.getUnconfidentTaskAt(i));
        }
        endTaskHTMLTable();
      } else {
        // no table data
        out.print(
            "<p>"+
            "<a href=\"");
        out.print("/$");
        out.print(support.getEncodedAgentName());
        out.print(support.getPath());
        out.print(
            "?showTables=true\">"+
            "Full Listing of Unplanned/Unestimated/Failed/Unconfident Tasks (");
        out.print(
            (result.getNumberOfTasks() - 
             result.getNumberOfFullySuccessfulTasks()));
        out.println(" lines)</a><br>");
        out.println("<a href=\"completion?viewType=viewAllAgents\">Show all agents</a>");
      }
    }

    /**
     * Begin a table of <tt>printAbstractTaskAsHTML</tt> entries.
     */
    protected void beginTaskHTMLTable(
        String title, String subTitle) {
      out.print(
          "<table border=1 cellpadding=3 cellspacing=1 width=\"100%\">\n"+
          "<tr bgcolor=lightgrey><th align=left colspan=5>");
      out.print(title);
      if (subTitle != null) {
        out.print(
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<tt><i>");
        out.print(subTitle);
        out.print("</i></tt>");
      }
      out.print(
          "</th></tr>\n"+
          "<tr>"+
          "<th></th>"+
          "<th>UID</th>"+
          "<th>ParentUID</th>"+
          "<th>Verb</th>"+
          "<th>Confidence</th>"+
          "<th>PlanElement</th>"+
          "</tr>\n");
    }

    /**
     * End a table of <tt>printAbstractTaskAsHTML</tt> entries.
     */
    protected void endTaskHTMLTable() {
      out.print(
          "</table>\n"+
          "<p>\n");
    }

    /**
     * Write the given <code>AbstractTask</code> as formatted HTML.
     */
    protected void printAbstractTaskAsHTML(int index, AbstractTask at) {
      out.print("<tr align=right><td>");
      out.print(index);
      out.print("</td><td>");
      String uidURL = at.getUID_URL();
      if (uidURL != null) {
        out.print("<a href=\"");
        out.print(uidURL);
        out.print("\" target=\"itemFrame\">");
      }
      out.print(at.getUID());
      if (uidURL != null) {
        out.print("</a>");
      }
      out.print("</td><td>");
      String pUidURL = at.getParentUID_URL();
      if (pUidURL != null) {
        out.print("<a href=\"");
        out.print(at.getParentUID_URL());
        out.print("\" target=\"itemFrame\">");
      }
      out.print(at.getParentUID());
      if (pUidURL != null) {
        out.print("</a>");
      }
      out.print("</td><td>");
      out.print(at.getVerb());
      out.print("</td><td>");
      double conf = at.getConfidence();
      out.print(
          (conf < 0.001) ? 
          "0.0%" : 
          ((100.0 * conf) + "%"));
      out.print("</td><td>");
      out.print(at.getPlanElement());
      out.print("</td></tr>\n");
    }
  }
}

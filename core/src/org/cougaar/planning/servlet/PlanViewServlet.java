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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.servlet.*;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.util.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.measure.AbstractMeasure;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.*;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A <code>Servlet</code>, loaded by the 
 * <code>SimpleServletComponent</code>, that generates HTML views 
 * of an Agent's Blackboard.
 * <p>
 * This Servlet allows the user to: <ol>
 *   <li> List Tasks, PlanElements, Assets, UniqueObjects</li>
 *   <li> View detailed information for Tasks, PlanElements, 
 *        etc.</li>
 *   <li> Search for an item by UID</li>
 *   <li> Illustrates the use of URLs to allow the user to 
 *        seamlessly jump between views of separate blackboards</li>
 *   <li> Provide XML views of data (using 
 *        org.cougaar.util.XMLify)</li>
 * </ol>.
 * <p>
 * This is a very large Servlet and is overly-complex to be
 * a good example for future Servlet developers.  Consider
 * using this simple example:
 * <pre>
 *    public class HelloServlet extends HttpServlet {
 *      private SimpleServletSupport support;
 *      public void setSimpleServletSupport(SimpleServletSupport support) {
 *        this.support = support;
 *      }
 *      public void doGet(
 *          HttpServletRequest req,
 *          HttpServletResponse res) throws IOException {
 *        PrintWriter out = req.getWriter();
 *        out.print("Hello from agent "+support.getEncodedAgentName());
 *      }
 *    }
 * </pre>
 * <p>
 * <pre>
 * @property org.cougaar.planning.servlet.planview.preds
 *    Name of the example predicate file that is loading into
 *    the "Advanced Search" page, which defaults to 
 *    "default.preds.dat"
 * </pre>
 *
 * @see org.cougaar.core.servlet.SimpleServletComponent
 */
public class PlanViewServlet
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
    // create a new "PlanViewer" context per request
    PlanViewer pv = new PlanViewer(support);
    pv.execute(request, response);  
  }

  /**
   * This inner class does all the work.
   * <p>
   * A new class is created per request, to keep all the
   * instance fields separate.  If there was only one
   * instance then multiple simultaneous requests would
   * corrupt the instance fields (e.g. the "out" stream).
   * <p>
   * This acts as a <b>context</b> per request.
   */
  private static class PlanViewer {

    // some constants:
    private static final String PREDS_FILENAME_PROPERTY =
      "org.cougaar.planning.servlet.planview.preds";
    private static final String DEFAULT_PREDS_FILENAME = 
      "default.preds.dat";
    private static final boolean DEBUG = false;
    private static final int DEFAULT_LIMIT = 100;

    //
    // parameters from the URL:
    //

    /**
     * "mode" constants, which control which page to generate.
     */
    public static final String MODE = "mode";
    public static final int MODE_FRAME                        =  0;
    public static final int MODE_ALL_TASKS                    =  1;
    public static final int MODE_CLUSTERS                     =  2;
    public static final int MODE_TASK_DETAILS                 =  3;
    public static final int MODE_TASKS_SUMMARY                =  4;
    public static final int MODE_PLAN_ELEMENT_DETAILS         =  5;
    public static final int MODE_ALL_PLAN_ELEMENTS            =  6;
    public static final int MODE_ASSET_DETAILS                =  7;
    public static final int MODE_ALL_ASSETS                   =  8;
    public static final int MODE_SEARCH                       =  9;
    public static final int MODE_XML_HTML_DETAILS             = 10;
    public static final int MODE_XML_RAW_DETAILS              = 11;
    public static final int MODE_ALL_UNIQUE_OBJECTS           = 12;
    public static final int MODE_WELCOME                      = 13;
    public static final int MODE_WELCOME_DETAILS              = 14;
    public static final int MODE_TASK_DIRECT_OBJECT_DETAILS   = 15;
    public static final int MODE_ASSET_TRANSFER_ASSET_DETAILS = 16;
    public static final int MODE_XML_HTML_ATTACHED_DETAILS    = 17;
    public static final int MODE_XML_RAW_ATTACHED_DETAILS     = 18;
    public static final int MODE_ADVANCED_SEARCH_FORM         = 19;
    public static final int MODE_ADVANCED_SEARCH_RESULTS      = 20;
    private int mode = -1;

    // filter by uid
    public static final String ITEM_UID = "uid";
    private String itemUID;

    // filter by task verb
    public static final String VERB = "verb";
    private String verbFilter;

    // limit quantity of data
    public static final String LIMIT = "limit";
    private boolean limit;

    // predicate
    public static final String PREDICATE = "pred";
    private String pred;

    // predicate style
    public static final String PREDICATE_STYLE = "predStyle";
    private String predStyle;

    // view parsed predicate for debugging
    public static final String PREDICATE_DEBUG = "predDebug";
    private boolean predDebug;

    // sort results by UID
    public static final String SORT_BY_UID = "sortByUID";
    private boolean sortByUID; 

    // writer from the request
    private PrintWriter out;

    // since "PlanViewer" is a static inner class, here
    // we hold onto the support API.
    //
    // this makes it clear that PlanViewer only uses
    // the "support" from the outer class.
    private SimpleServletSupport support;

    public PlanViewer(SimpleServletSupport support) {
      this.support = support;
    }

    /**
     * Main method.
     */
    public void execute(
        HttpServletRequest request, 
        HttpServletResponse response) throws IOException, ServletException 
    {
      this.out = response.getWriter();

      // create a URL parameter visitor
      ServletUtil.ParamVisitor vis = 
        new ServletUtil.ParamVisitor() {
          public void setParam(String name, String value) {
            if (name.equalsIgnoreCase(MODE)) {
              try {
                mode = Integer.parseInt(value);
              } catch (Exception eBadNumber) {
                System.err.println("INVALID MODE: "+value);
                mode = MODE_FRAME;
              }
            } else if (name.equalsIgnoreCase(ITEM_UID)) {
              if (value != null) {
                try {
                  itemUID = URLDecoder.decode(value, "UTF-8");
                } catch (Exception eBadEnc) {
                  System.err.println("INVALID UID: "+value);
                }
              }
            } else if (name.equalsIgnoreCase(VERB)) {
              verbFilter = value;
            } else if (name.equalsIgnoreCase(LIMIT)) {
              limit = "true".equalsIgnoreCase(value);
            } else if (name.equalsIgnoreCase(PREDICATE)) {
              pred = value;
            } else if (name.equalsIgnoreCase(PREDICATE_STYLE)) {
              predStyle = value;
            } else if (name.equalsIgnoreCase(PREDICATE_DEBUG)) {
              predDebug = 
                ((value != null) ?  
                 value.equalsIgnoreCase("true") : 
                 true);
            } else if (name.equalsIgnoreCase(SORT_BY_UID)) {
	      sortByUID = 
		((value != null) ?  
                 value.equalsIgnoreCase("true") : 
                 true);
	    }
          }
        };

      // visit the URL parameters
      ServletUtil.parseParams(vis, request);

      try {
        // decide which page to generate
        switch (mode) {
          default:
            if (DEBUG) {
              System.err.println("DEFAULT MODE");
            }
          case MODE_FRAME:
            displayFrame();
            break;
          case MODE_WELCOME:
            displayWelcome();
            break;
          case MODE_WELCOME_DETAILS:
            displayWelcomeDetails();
            break;
          case MODE_ALL_TASKS:
            displayAllTasks();
            break;
          case MODE_TASK_DETAILS:
            displayTaskDetails();
            break;
          case MODE_TASKS_SUMMARY:
            displayTasksSummary();
            break;
          case MODE_PLAN_ELEMENT_DETAILS:
            displayPlanElementDetails();
            break;
          case MODE_ALL_PLAN_ELEMENTS:
            displayAllPlanElements();
            break;
          case MODE_ASSET_DETAILS:
          case MODE_TASK_DIRECT_OBJECT_DETAILS:
          case MODE_ASSET_TRANSFER_ASSET_DETAILS:
            displayAssetDetails();
            break;
          case MODE_ALL_ASSETS:
            displayAllAssets();
            break;
          case MODE_CLUSTERS:
          case MODE_SEARCH:
            displaySearch();
            break;
          case MODE_XML_HTML_DETAILS:
          case MODE_XML_HTML_ATTACHED_DETAILS:
          case MODE_XML_RAW_DETAILS:
          case MODE_XML_RAW_ATTACHED_DETAILS:
            displayUniqueObjectDetails();
            break;
          case MODE_ALL_UNIQUE_OBJECTS:
            displayAllUniqueObjects();
            break;
          case MODE_ADVANCED_SEARCH_FORM:
            displayAdvancedSearchForm();
            break;
          case MODE_ADVANCED_SEARCH_RESULTS:
            displayAdvancedSearchResults();
            break;
        }
      } catch (Exception e) {
        System.err.println(
            "/$"+
            support.getEncodedAgentName()+
            support.getPath()+
            " Exception: ");
        e.printStackTrace();
        out.print(
            "<html><body><h1>"+
            "<font color=red>Unexpected Exception!</font>"+
            "</h1><p><pre>");
        e.printStackTrace(out);
        out.print("</pre></body></html>");
        out.flush();
      }
    }

    /** BEGIN DISPLAY ROUTINES **/

    /**
     * displayFrame.
     */
    private void displayFrame()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Frame");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "Cougaar PlanViewer"+
          "</title>\n"+
          "</head>\n"+
          "<frameset cols=\"25%,75%\">\n"+
          "<frameset rows=\"32%,68%\">\n"+
          "<frame src=\"/$");
      out.print(support.getEncodedAgentName());
      out.print(support.getPath());     
      out.print(
          "?"+
          MODE+
          "="+
          MODE_SEARCH+
          "\" name=\"searchFrame\">\n");
      //
      // Show blank WelcomeDetails page in itemFrame, since user
      // probably didn't specify $encodedAgentName in URL.
      //
      out.print("<frame src=\"/$");
      out.print(support.getEncodedAgentName());
      out.print(support.getPath());
      out.print(
          "?"+
          MODE+
          "="+
          MODE_WELCOME_DETAILS+
          "\" name=\"itemFrame\">\n"+
          "</frameset>\n"+
          "<frame src=\"/$");
      out.print(support.getEncodedAgentName());
      out.print(support.getPath());
      // 
      // Show blank Welcome page in tablesFrame, since user
      // probably didn't specify $encodedAgentName in URL.
      //
      out.print(
          "?"+
          MODE+
          "="+
          MODE_WELCOME+
          "\" name=\"tablesFrame\">\n"+
          "</frameset>\n"+
          "<noframes>\n"+
          "<h2>Frame Task</h2>\n"+
          "<p>"+
          "This document is designed to be viewed using the frames feature. "+
          "If you see this message, you are using a non-frame-capable web "+
          "client.\n"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayWelcome.
     */
    private void displayWelcome()
    {
      if (DEBUG) {
        System.out.println("Display Welcome");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "COUGAAR PlanView"+
          "</title>\n"+
          "</head>\n"+
          "<body ");
      out.print(
          "bgcolor=\"#F0F0F0\">\n"+
          "<p>"+
          "<font size=small color=mediumblue>No Agent selected.</font>\n"+
          "</body>\n"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayWelcomeDetails.
     */
    private void displayWelcomeDetails()
    {
      if (DEBUG) {
        System.out.println("Display Welcome Details");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "Item Details View"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<p>"+
          "<font size=small color=mediumblue>No Item selected.</font>\n"+
          "</body>\n"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayTaskDetails.
     */
    private void displayTaskDetails()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Task Details");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "Children Task View"+
          "</title>"+
          "</head>\n"+
          "<body  bgcolor=\"#F0F0F0\">\n"+
          "<b>");
      // link to cluster
      printLinkToTasksSummary();
      out.print(
          "</b><br>\n"+
          "Task<br>");
      // find task
      UniqueObject baseObj = 
        findUniqueObjectWithUID(itemUID);
      if (baseObj instanceof Task) {
        printTaskDetails((Task)baseObj);
      } else {
        out.print(
            "<p>"+
            "<font size=small color=mediumblue>");
        if (itemUID == null) {
          out.print("No Task selected.");
        } else if (baseObj == null) {
          out.print("No Task matching \"");
          out.print(itemUID);
          out.print("\" found.");
        } else {
          out.print("UniqueObject with UID \"");
          out.print(itemUID);
          out.print("\" is not a Task: ");
          out.print(baseObj.getClass().getName());
        }
        out.print(
            "</font>"+
            "<p>\n");
      }
      out.print(
          "</body>\n"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayAllTasks.
     */
    private void displayAllTasks()
    {
      if (DEBUG) {
        System.out.println("\nDisplay All Tasks");
      }
      // find tasks
      Collection col;
      if (verbFilter != null) {
        col = findTasksWithVerb(verbFilter);
      } else {
        col = findAllTasks();
      }
      int numTasks = col.size();
      Iterator tasksIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched Tasks");
      }
      // begin page
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " Tasks"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<p>"+
          "<center>");
      if (limit && (numTasks > DEFAULT_LIMIT)) {
        out.print("Showing first <b>");
        out.print(DEFAULT_LIMIT);
        out.print("</b> of ");
      }
      out.print("<b>");
      out.print(numTasks);
      out.print(
          "</b> Task");
      if (numTasks != 1) {
        out.print("s");
      }
      if (verbFilter != null) {
        out.print(" with verb ");
        out.print(verbFilter);
      }
      out.print(" at ");
      out.print(support.getEncodedAgentName());
      out.print("</center>\n");
      if (limit && (numTasks > DEFAULT_LIMIT)) {
        out.print("<center>");
        // link to all tasks.
        printLinkToAllTasks(
            verbFilter, 0, numTasks, true);
        out.print("</center>\n");
      }
      // print table headers
      out.print(
          "\n<table align=center border=1 cellpadding=1\n"+
          " cellspacing=1 width=75%\n"+
          " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=7>"+
          "<font size=+1 color=mediumblue><b>Tasks</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td rowspan=2><font color=mediumblue><b>UID</b></font></td>\n"+
          "<td rowspan=2><font color=mediumblue><b>Verb</b></font></td>\n"+
          "<td colspan=4>"+
          "<font color=mediumblue><b>Direct Object</b></font>"+
          "</td>\n"+
          "<td rowspan=2>"+
          "<font color=mediumblue><b>Prepositional Phrases</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>UID</b></font></td>\n"+
          "<td><font color=mediumblue><b>TypeID</b></font></td>\n"+
        "<td><font color=mediumblue><b>ItemID</b></font></td>\n"+
        "<td><font color=mediumblue><b>Quantity</b></font></td>\n"+
        "</tr>\n");
      if (numTasks > 0) {
        // print table rows
        int rows = 0;
        while (tasksIter.hasNext()) {
          Task task = (Task)tasksIter.next();
          out.print(
              "<tr>\n"+
              "<td>\n");
          printLinkToLocalTask(task);
          out.print(
              "</td>\n"+
              "<td>\n");
          // show verb
          Verb v = task.getVerb();
          if (v != null) {
            out.print(v.toString());
          } else {
            out.print("<font color=red>missing verb</font>");
          }
          out.print("</td>\n");
          // show direct object
          printTaskDirectObjectTableRow(task);
          // show prepositional phrases
          out.print(
              "<td>"+
              "<font size=-1>");
          Enumeration enprep = task.getPrepositionalPhrases();
          while (enprep.hasMoreElements()) {
            PrepositionalPhrase pp = 
              (PrepositionalPhrase)enprep.nextElement();
            String prep = pp.getPreposition();
            out.print("<font color=mediumblue>");
            out.print(prep);
            out.print("</font>");
            printObject(pp.getIndirectObject());
            out.print(",");
          }
          out.print(
              "</font>"+
              "</td>\n"+
              "</tr>\n");
          if ((++rows % DEFAULT_LIMIT) == 0) {
            if (limit) {
              // limit to DEFAULT_LIMIT
              break;
            }
            // restart table
            out.print("</table>\n");
            out.flush();
            out.print(
                "<table align=center border=1 cellpadding=1\n"+
                " cellspacing=1 width=75%\n"+
                " bordercolordark=#660000 bordercolorlight=#cc9966>\n");
          }
        }
        // end table
        out.print("</table>\n");
        if (limit && (rows == DEFAULT_LIMIT)) {
          // link to unlimited view
          out.print(
              "<p>"+
              "<center>");
          printLinkToAllTasks(
              verbFilter, 0, numTasks, true);
          out.print(
              "<br>"+
              "</center>\n");
        }
      } else {
        // end table
        out.print(
            "</table>\n"+
            "<center>"+
            "<font color=mediumblue>\n"+
            "No Tasks");
        if (verbFilter != null) {
          out.print(" with verb ");
          out.print(verbFilter);
        }
        out.print(" found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayTaskSummary.
     */
    private void displayTasksSummary()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Tasks Summary");
      }
      // find tasks
      boolean oldSortByUID = sortByUID;
      sortByUID = false;
      Collection col = findAllTasks();
      sortByUID = oldSortByUID;
      int numTasks = col.size();
      Iterator tasksIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched Tasks");
      }
      // begin page
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " Tasks Summary"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<center>");
      printLinkToAllTasks(
          null, 0, numTasks, false);
      out.print("</center>\n");
      if (numTasks > DEFAULT_LIMIT) {
        // give limit option
        out.print("<center>");
        printLinkToAllTasks(
            null, DEFAULT_LIMIT, numTasks, false);
        out.print("</center>\n");
      }
      // begin table
      out.print(
          "<p>\n"+
          "<table align=center border=1 cellpadding=1 cellspacing=1\n"+
          " width=75% bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=2>"+
          "<font size=+1 color=mediumblue><b>Tasks Summary</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>Verb</font></b></td>\n"+
          "<td><font color=mediumblue><b>Count</font></b></td>\n"+
          "</tr>\n");
      // table rows
      if (numTasks != 0) {
        // count by verb
        HashMap tasksInfoMap = new HashMap();
        while (tasksIter.hasNext()) {
          Task task = (Task)tasksIter.next();
          Verb verb = task.getVerb();
          VerbSummaryInfo info = 
            (VerbSummaryInfo)tasksInfoMap.get(verb);
          if (info == null) {
            info = new VerbSummaryInfo(verb);
            tasksInfoMap.put(verb, info);
          }
          ++info.counter;
        }
        // sort by verb
        Collection sortedInfosCol =
          Sortings.sort(
              tasksInfoMap.values(),
              SummaryInfo.LARGEST_COUNTER_FIRST_ORDER);
        Iterator sortedInfosIter = sortedInfosCol.iterator();
        // print rows
        while (sortedInfosIter.hasNext()) {
          VerbSummaryInfo info = (VerbSummaryInfo)sortedInfosIter.next();
          out.print(
              "<tr>\n"+
              "<td>\n");
          // link to all tasks with verb
          printLinkToAllTasks(
              info.verb.toString(), 0, info.counter, false);
          if (info.counter > DEFAULT_LIMIT) {
            // link to limited number of tasks with verb
            out.print(" (");
            printLinkToAllTasks(
                info.verb.toString(), DEFAULT_LIMIT, info.counter, false);
            out.print(")");
          }
          out.print(
              "</td>\n"+
              "<td align=right>");
          out.print(info.counter);
          out.print(
              "</td>\n"+
              "</tr>\n");
        }
      }
      // end table
      out.print("</table>\n");
      if (numTasks == 0) {
        out.print(
            "<center>"+
            "<font color=mediumblue >\n"+
            "No Tasks found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
    /**
     * displayPlanElementDetails.
     */
    private void displayPlanElementDetails()
    {
      if (DEBUG) {
        System.out.println("\nDisplay PlanElement Details");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "PlanElement View"+
          "</title>"+
          "</head>\n"+
          "<body  bgcolor=\"#F0F0F0\">\n"+
          "<b>");
      // link to cluster
      printLinkToTasksSummary();
      out.print(
          "</b><br>\n");
      // find plan element
      UniqueObject baseObj = 
        findUniqueObjectWithUID(itemUID);
      if (baseObj instanceof PlanElement) {
        printPlanElementDetails((PlanElement)baseObj);
      } else {
        out.print(
            "<p>"+
            "<font size=small color=mediumblue>");
        if (itemUID == null) {
          out.print("No PlanElement selected.");
        } else if (baseObj == null) {
          out.print("No PlanElement matching \"");
          out.print(itemUID);
          out.print("\" found.");
        } else {
          out.print("UniqueObject with UID \"");
          out.print(itemUID);
          out.print("\" is not a PlanElement: ");
          out.print(baseObj.getClass().getName());
        }
        out.print(
            "</font>"+
            "<p>\n");
      }
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayAllPlanElements.
     */
    private void displayAllPlanElements()
    {
      if (DEBUG) {
        System.out.println("\nDisplay All PlanElements");
      }
      Collection col = findAllPlanElements();
      int numPlanElements = col.size();
      Iterator peIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched PlanElements");
      }
      // begin page
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " PlanElements"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<center>");
      if (limit && (numPlanElements > DEFAULT_LIMIT)) {
        out.print("Showing first <b>");
        out.print(DEFAULT_LIMIT);
        out.print("</b> of ");
      }
      out.print("<b>");
      out.print(numPlanElements);
      out.print(
          "</b> PlanElement");
      if (numPlanElements != 1) {
        out.print("s");
      }
      out.print(" at ");
      out.print(support.getEncodedAgentName());
      out.print("</center>");
      if (limit && (numPlanElements > DEFAULT_LIMIT)) {
        out.print("<center>");
        // link to all pes
        printLinkToAllPlanElements(
            0, numPlanElements, false);
        out.print("</center>");
      }
      out.print(
          "\n<table align=center border=1 cellpadding=1\n"+
          " cellspacing=1 width=75%\n"+
          " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=2>"+
          "<font size=+1 color=mediumblue><b>PlanElements</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>UID</b></font></td>\n"+
          "<td><font color=mediumblue><b>Type</b></font></td>\n"+
          "</tr>\n");
      if (numPlanElements > 0) {
        // print table rows
        int rows = 0;
        while (peIter.hasNext()) {
          PlanElement pe = (PlanElement)peIter.next();
          out.print(
              "<tr>\n"+
              "<td>\n");
          printLinkToPlanElement(pe);
          out.print(
              "</td>\n"+
              "<td>\n");
          int peType = getItemType(pe);
          if (peType != ITEM_TYPE_OTHER) {
            out.print(ITEM_TYPE_NAMES[peType]);
          } else {
            out.print("<font color=red>");
            if (pe != null) {
              out.print(pe.getClass().getName());
            } else {
              out.print("null");
            }
            out.print("</font>");
          }
          out.print(
              "</td>"+
              "</tr>\n");
          if ((++rows % DEFAULT_LIMIT) == 0) {
            if (limit) {
              // limit to DEFAULT_LIMIT
              break;
            }
            // restart table
            out.print("</table>\n");
            out.flush();
            out.print(
                "<table align=center border=1 cellpadding=1\n"+
                " cellspacing=1 width=75%\n"+
                " bordercolordark=#660000 bordercolorlight=#cc9966>\n");
          }
        }
        // end table
        out.print("</table>\n");
        if (limit && (rows == DEFAULT_LIMIT)) {
          // link to unlimited view
          out.print(
              "<p>"+
              "<center>");
          printLinkToAllPlanElements(
              0, numPlanElements, false);
          out.print(
              "<br>"+
              "</center>\n");
        }
      } else {
        out.print(
            "</table>"+
            "<center>"+
            "<font color=mediumblue>\n"+
            "No PlanElements found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayAssetDetails.
     */
    private void displayAssetDetails()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Asset Details");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "Asset View"+
          "</title>"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<b>");
      // link to cluster
      printLinkToTasksSummary();
      out.print(
          "</b><br>\n"+
          "Asset<br>");
      // find "base" UniqueObject with the specifed UID
      UniqueObject baseObj = 
        findUniqueObjectWithUID(itemUID);
      Asset asset = null;
      // get asset
      switch (mode) {
        case MODE_ASSET_DETAILS:
          // asset itself
          if (baseObj instanceof Asset) {
            asset = (Asset)baseObj;
          }
          break;
        case MODE_TASK_DIRECT_OBJECT_DETAILS:
          // asset attached to Task
          if (baseObj instanceof Task) {
            asset = ((Task)baseObj).getDirectObject();
          }
          break;
        case MODE_ASSET_TRANSFER_ASSET_DETAILS:
          // asset attached to AssetTransfer
          if (baseObj instanceof AssetTransfer) {
            asset = ((AssetTransfer)baseObj).getAsset();
          }
          break;
        default:
          break;
      }
      if (asset != null) {
        printAssetDetails(baseObj, asset);
      } else {
        String baseType;
        switch (mode) {
          case MODE_ASSET_DETAILS:
            baseType = "Asset";
            break;
          case MODE_TASK_DIRECT_OBJECT_DETAILS:
            baseType = "Task";
            break;
          case MODE_ASSET_TRANSFER_ASSET_DETAILS:
            baseType = "AssetTransfer";
            break;
          default:
            baseType = "<font color=red>Error</font>";
            break;
        }
        out.print(
            "<p>"+
            "<font size=small color=mediumblue>");
        if (itemUID == null) {
          out.print("No ");
          out.print(baseType);
          out.print(" selected.");
        } else if (baseObj == null) {
          out.print("No ");
          out.print(baseType);
          out.print(" matching \"");
          out.print(itemUID);
          out.print("\" found in ");
          out.print(support.getEncodedAgentName());
          out.print(".");
        } else {
          out.print("UniqueObject with UID \"");
          out.print(itemUID);
          out.print("\" is not of type ");
          out.print(baseType);
          out.print(": ");
          out.print(baseObj.getClass().getName());
        }
        out.print(
            "</font>"+
            "<p>\n");
      }
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayAllAssets.
     */
    private void displayAllAssets()
    {
      if (DEBUG) {
        System.out.println("\nDisplay All Assets");
      }
      Collection col = findAllAssets();
      int numAssets = col.size();
      Iterator assetIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched Assets");
      }
      // begin page
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " Assets"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<center>");
      if (limit && (numAssets > DEFAULT_LIMIT)) {
        out.print("Showing first <b>");
        out.print(DEFAULT_LIMIT);
        out.print("</b> of ");
      }
      out.print("<b>");
      out.print(numAssets);
      out.print(
          "</b> Asset");
      if (numAssets != 1) {
        out.print("s");
      }
      out.print(" at ");
      out.print(support.getEncodedAgentName());
      out.print("</center>");
      if (limit && (numAssets > DEFAULT_LIMIT)) {
        out.print("<center>");
        // link to all assets
        printLinkToAllAssets(
            0, numAssets, false);
        out.print("</center>");
      }
      out.print(
          "\n<table align=center border=1 cellpadding=1\n"+
          " cellspacing=1 width=75%\n"+
          " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=4>"+
          "<font size=+1 color=mediumblue><b>Assets</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>UID</font></b></td>\n"+
          "<td><font color=mediumblue><b>TypeID</font></b></td>\n"+
          "<td><font color=mediumblue><b>ItemID</font></b></td>\n"+
          "<td><font color=mediumblue><b>Quantity</font></b></td>\n"+
          "</tr>\n");
      if (numAssets > 0) {
        // print table rows
        int rows = 0;
        while (assetIter.hasNext()) {
          Asset asset = (Asset)assetIter.next();
          out.print("<tr>\n");
          printAssetTableRow(asset);
          out.print("</tr>\n");
          if ((++rows % DEFAULT_LIMIT) == 0) {
            // restart table
            if (limit) {
              // limit to DEFAULT_LIMIT
              break;
            }
            out.print("</table>\n");
            out.flush();
            out.print(
                "<table align=center border=1 cellpadding=1\n"+
                " cellspacing=1 width=75%\n"+
                " bordercolordark=#660000 bordercolorlight=#cc9966>\n");
          }
        }
        // end table
        out.print("</table>\n");
        if (limit && (rows == DEFAULT_LIMIT)) {
          // link to unlimited view
          out.print(
              "<p>"+
              "<center>");
          printLinkToAllAssets(
              0, numAssets, false);
          out.print(
              "<br>"+
              "</center>\n");
        }
      } else {
        out.print(
            "</table>"+
            "<center>"+
            "<font color=mediumblue>\n"+
            "No Assets found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
     * displaySearch.
     * <p>
     * Uses JavaScript to set the FORM action, since the user selects
     * the cluster _after_ page load and the action must point to the
     * correct Agent's URL.
     */
    private void displaySearch()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Form");
      }
      out.print(
          "<html>\n"+
          "<script language=\"JavaScript\">\n"+
          "<!--\n"+
          "function mySubmit() {\n"+
          "  var tidx = document.myForm.formAgent.selectedIndex\n"+
          "  var encAgent = document.myForm.formAgent.options[tidx].value\n"+
          "  var type = document.myForm.formType.selectedIndex\n"+
          "  var uid = trim(document.myForm."+
          ITEM_UID+
          ".value)\n"+
          "  if (uid.length > 0) {\n"+
          "    document.myForm.target=\"itemFrame\"\n"+
          "    if (type == 0) {\n"+
          "      document.myForm."+
          MODE+
          ".value= \""+
          MODE_TASK_DETAILS+
          "\"\n"+
          "    } else if (type == 1) {\n"+
          "      document.myForm."+
          MODE+
        ".value= \""+
        MODE_PLAN_ELEMENT_DETAILS+
        "\"\n"+
        "    } else if (type == 2) {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_ASSET_DETAILS+
        "\"\n"+
        "    } else {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_XML_HTML_DETAILS+
        "\"\n"+
        "    }\n"+
        "    if (uid.charAt(0) == '/') {\n"+
        "      document.myForm."+
        ITEM_UID+
        ".value = encAgent + uid\n"+
        "    } else if (uid != document.myForm."+
        ITEM_UID+
        ".value) {\n"+
        "      document.myForm."+
        ITEM_UID+
        ".value = uid\n"+
        "    }\n"+
        "  } else {\n"+
        "    document.myForm.target=\"tablesFrame\"\n"+
        "    if (type == 0) {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_TASKS_SUMMARY+
        "\"\n"+
        "    } else if (type == 1) {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_ALL_PLAN_ELEMENTS+
        "\"\n"+
        "    } else if (type == 2) {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_ALL_ASSETS+
        "\"\n"+
        "    } else {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_ALL_UNIQUE_OBJECTS+
        "\"\n"+
        "    }\n"+
        "  }\n"+
        "  document.myForm.action=\"/$\"+encAgent+\"");
      out.print(support.getPath());
      out.print("\"\n"+
          "  return true\n"+
          "}\n"+
          "\n"+
          "// javascript lacks String.trim()?\n"+
          "function trim(val) {\n"+
          "  var len = val.length\n"+
          "  if (len == 0) {\n"+
          "    return \"\"\n"+
          "  }\n"+
          "  var i\n"+
          "  for (i = 0; ((i < len) && (val.charAt(i) == ' ')); i++) {}\n"+
          "  if (i == len) {\n"+
          "    return \"\";\n"+
          "  }\n"+
          "  var j \n"+
          "  for (j = len-1; ((j > i) && (val.charAt(j) == ' ')); j--) {}\n"+
          "  j++\n"+
          "  if ((i == 0) && (j == len)) {\n"+
          "    return val\n"+
          "  }\n"+
          "  var ret = val.substring(i, j)\n"+
        "  return ret\n"+
        "}\n"+
        "// -->\n"+
        "</script>\n"+
        "<head>\n"+
        "<title>Logplan Search</title>\n"+
        "</head>\n"+
        "<body bgcolor=\"#F0F0F0\">\n"+
        "<noscript>\n"+
        "<b>This page needs Javascript!</b><br>\n"+
        "Consult your browser's help pages..\n"+
        "<p><p><p>\n"+
        "</noscript>\n"+
        "<form name=\"myForm\" method=\"get\" onSubmit=\"return mySubmit()\">\n"+
        "<input type=\"hidden\" name=\""+
        MODE+
        "\" value=\"fromJavaScript\">\n"+
        "<input type=\"hidden\" name=\""+
        LIMIT+
        "\" value=\"true\">\n"+
        "<select name=\"formAgent\">\n");
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
          "</select><br>\n"+
          "<select name=\"formType\">\n"+
          "  <option selected value=\"0\">Tasks</option>\n"+
          "  <option value=\"1\">PlanElements</option>\n"+
          "  <option value=\"2\">Assets</option>\n"+
          "  <option value=\"3\">UniqueObjects</option>\n"+
          "</select><br>\n"+
          "UID:<input type=\"text\" name=\""+
          // user should enter an encoded UID
          ITEM_UID+
          "\" size=12><br>\n"+
	  "Sort results by UID<input type=\"checkbox\" name=\"sortByUID\" value=\"true\"><br>\n"+
          "<input type=\"submit\" name=\"formSubmit\" value=\"Search\"><br>\n"+
          "<p>\n"+
          // link to advanced search
          "<a href=\"/$");
      out.print(support.getEncodedAgentName());
      out.print(support.getPath());
      out.print(
          "?"+
          MODE+
          "="+
          MODE_ADVANCED_SEARCH_FORM+
          "\" target=\"advSearch\">Advanced search</a>"+
          "</form>\n"+
          "</body>\n"+
          "</html>\n");
    }

    /**
     * displayUniqueObjectDetails.
     */
    private void displayUniqueObjectDetails()
    {
      boolean asHTML;
      boolean isAttached;
      switch (mode) {
        default:
          // error, but treat as "MODE_XML_HTML_DETAILS"
        case MODE_XML_HTML_DETAILS:
          asHTML = true;
          isAttached = false;
          break;
        case MODE_XML_HTML_ATTACHED_DETAILS:
          asHTML = true;
          isAttached = true;
          break;
        case MODE_XML_RAW_DETAILS:
          asHTML = false;
          isAttached = false;
          break;
        case MODE_XML_RAW_ATTACHED_DETAILS:
          asHTML = false;
          isAttached = true;
          break;
      }
      if (DEBUG) {
        System.out.println(
            "\nDisplay UniqueObject "+
            (asHTML ? "HTML" : "Raw")+                     
            (isAttached ? " Attached" : "")+
            " Details");
      }
      // find base object using the specified UID
      UniqueObject baseObj = 
        findUniqueObjectWithUID(itemUID);
      // get the attached object
      Object attachedObj;
      if (isAttached) {
        // examine baseObj to find attached XMLizable
        // 
        // currently only a few cases are supported:
        //   Asset itself
        //   Task's "getDirectObject()"
        //   AssetTransfer's "getAsset()"
        if (baseObj instanceof Asset) {
          // same as above "MODE_XML_[HTML|RAW]_DETAILS"
          attachedObj = baseObj;
        } else if (baseObj instanceof Task) {
          attachedObj = ((Task)baseObj).getDirectObject();
        } else if (baseObj instanceof AssetTransfer) {
          attachedObj = ((AssetTransfer)baseObj).getAsset();
        } else {
          // error
          attachedObj = null;
        }
      } else {
        // the base itself
        attachedObj = baseObj;
      }
      // cast to XMLizable
      XMLizable xo =
        ((attachedObj instanceof XMLizable) ?
         (XMLizable)attachedObj :
         null);
      if (asHTML) {
        // print as HTML
        out.print("<html>\n<head>\n<title>");
        out.print(itemUID);
        out.print(
            " View</title>"+
            "</head>\n<body bgcolor=\"#F0F0F0\">\n<b>");
        // link to cluster
        printLinkToTasksSummary();
        out.print(
            "</b><br>\n"+
            "UniqueObject<br>");
        if (xo != null) {
          // link to non-html view of object
          out.print("<p>");
          printLinkToXML(xo, false);
          out.print("<br><hr><br><pre>\n");
          // print HTML-wrapped XML
          printXMLizableDetails(xo, true);
          out.print("\n</pre><br><hr><br>\n");
        } else {
          out.print("<p><font size=small color=mediumblue>");
          if (itemUID == null) {
            out.print("No UniqueObject selected.");
          } else if (baseObj == null) {
            out.print("No UniqueObject matching \"");
            out.print(itemUID);
            out.print("\" found.");
          } else if (attachedObj == null) {
            out.print("UniqueObject with UID \"");
            out.print(itemUID);
            out.print("\" of type ");
            out.print(baseObj.getClass().getName());
            out.print(" has null attached Object.");
          } else {
            out.print("UniqueObject with UID \"");
            out.print(itemUID);
            out.print("\" of type ");
            out.print(baseObj.getClass().getName());
            out.print(" has non-XMLizable attached Object: ");
            out.print(attachedObj.getClass().getName());
          }
          out.print("</font><p>\n");
        }
        out.print("</body></html>\n");
      } else {
        // print raw XML
        printXMLizableDetails(xo, false);
      }
      out.flush();
    }

    /**
     * displayAllUniqueObjects.
     */
    private void displayAllUniqueObjects()
    {
      if (DEBUG) {
        System.out.println("\nDisplay All UniqueObjects");
      }
      Collection col = findAllUniqueObjects();
      int numUniqueObjects = col.size();
      Iterator uoIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched UniqueObjects");
      }
      // begin page
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " UniqueObjects"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<center>");
      if (limit && (numUniqueObjects > DEFAULT_LIMIT)) {
        out.print("Showing first <b>");
        out.print(DEFAULT_LIMIT);
        out.print("</b> of ");
      }
      out.print("<b>");
      out.print(numUniqueObjects);
      out.print(
          "</b> UniqueObject");
      if (numUniqueObjects != 1) {
        out.print("s");
      }
      out.print(" at ");
      out.print(support.getEncodedAgentName());
      out.print("</center>\n");
      if (limit && (numUniqueObjects > DEFAULT_LIMIT)) {
        out.print("<center>");
        // link to all uniqueObjects.
        printLinkToAllUniqueObjects(
            0, numUniqueObjects, false);
        out.print("</center>\n");
      }
      out.print(
          "\n<table align=center border=1 cellpadding=1\n"+
          " cellspacing=1 width=75%\n"+
          " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=2>"+
          "<font size=+1 color=mediumblue><b>UniqueObjects</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>UID</font></b></td>\n"+
          "<td><font color=mediumblue><b>Type</font></b></td>\n"+
          "</tr>\n");
      if (numUniqueObjects > 0) {
        // print table rows
        int rows = 0;
        while (uoIter.hasNext()) {
          UniqueObject uo = (UniqueObject)uoIter.next();
          int itemType = getItemType(uo);
          out.print(
              "<tr>\n"+
              "<td>");
          switch (itemType) {
            case ITEM_TYPE_ALLOCATION:
            case ITEM_TYPE_EXPANSION:
            case ITEM_TYPE_AGGREGATION:
            case ITEM_TYPE_DISPOSITION:
            case ITEM_TYPE_ASSET_TRANSFER:
              printLinkToPlanElement((PlanElement)uo);
              break;
            case ITEM_TYPE_TASK:
              printLinkToLocalTask((Task)uo);
              break;
            case ITEM_TYPE_ASSET:
              // found this asset in local blackboard
              printLinkToLocalAsset((Asset)uo);
              break;
            case ITEM_TYPE_WORKFLOW:
            default:
              if (uo instanceof XMLizable) {
                // XMLizable and a local UniqueObject
                printLinkToXML(uo, true);
              } else {
                out.print("<font color=red>No XML for ");
                UID uoU;
                String uoUID;
                if (((uoU = uo.getUID()) != null) &&
                    ((uoUID = uoU.toString()) != null)) {
                  out.print(uoUID);
                } else {
                  out.print("null");
                }
                out.print("</font>");
              } 
              break;
          }
          out.print(
              "</td>\n"+
              "<td>");
          if (itemType != ITEM_TYPE_OTHER) {
            out.print(ITEM_TYPE_NAMES[itemType]);
          } else {
            out.print("<font color=red>");
            out.print(uo.getClass().getName());
            out.print("</font>");
          }
          out.print(
              "</td>\n"+
              "</tr>\n");
          if ((++rows % DEFAULT_LIMIT) == 0) {
            if (limit) {
              // limit to DEFAULT_LIMIT
              break;
            }
            // restart table
            out.print("</table>\n");
            out.flush();
            out.print(
                "<table align=center border=1 cellpadding=1\n"+
                " cellspacing=1 width=75%\n"+
                " bordercolordark=#660000 bordercolorlight=#cc9966>\n");
          }
        }
        // end table
        out.print("</table>\n");
        if (limit && (rows == DEFAULT_LIMIT)) {
          // link to unlimited view
          out.print(
              "<p>"+
              "<center>");
          printLinkToAllUniqueObjects(
              0, numUniqueObjects, false);
          out.print(
              "<br>"+
              "</center>\n");
        }
      } else {
        out.print(
            "</table>"+
            "<center>"+
            "<font color=mediumblue>\n"+
            "No UniqueObjects found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    // keep a Map of ordered (name, value) pairs
    private static PropertyTree TEMPLATE_PREDS = null;
    private static synchronized final PropertyTree getTemplatePreds() { 
      if (TEMPLATE_PREDS == null) {
        String fname = System.getProperty(PREDS_FILENAME_PROPERTY);
        if (fname == null) {
          fname = DEFAULT_PREDS_FILENAME;
        }
        try {
          InputStream in = ConfigFinder.getInstance().open(fname);
          TEMPLATE_PREDS = PredTableParser.parse(in);
        } catch (IOException ioe) {
          System.err.println("Unable to open predicate file \""+fname+"\":");
          TEMPLATE_PREDS = new PropertyTree(1);
          TEMPLATE_PREDS.put("Unable to load \\\\"+fname+"\\\"", "");
        }
      }
      return TEMPLATE_PREDS;
    }

    private void displayAdvancedSearchForm()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Advanced Search Form");
      }
      out.print(
          "<html>\n"+
          "<script language=\"JavaScript\">\n"+
          "<!--\n"+
          "function mySubmit() {\n"+
          "  var tidx = document.myForm.formAgent.selectedIndex\n"+
          "  var encAgent = document.myForm.formAgent.options[tidx].value\n"+
          "  document.myForm.action=\"/$\"+encAgent+\"");
      out.print(support.getPath());
      out.print(
          "\"\n"+
          "  return true\n"+
          "}\n"+
          "\n"+
          "function setPred() {\n"+
          "  var i = document.myForm.formPred.selectedIndex\n"+
          "  var s\n"+
          "  switch(i) {\n"+
          "    default: alert(\"unknown (\"+i+\")\"); break\n");
      PropertyTree templatePreds = getTemplatePreds();
      int nTemplatePreds = templatePreds.size();
      for (int i = 0; i < nTemplatePreds; i++) {
        out.print("case ");
        out.print(i);
        out.print(": s=\"");
        out.print(templatePreds.getValue(i));
        out.print("\"; break\n");
      }
      out.print(
          "  }\n"+
          "  document.myForm."+
          PREDICATE_STYLE+
          ".selectedIndex=0\n"+
          "  document.myForm.pred.value=s\n"+
          "}\n"+
          "// -->\n"+
          "</script>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " Advanced Search Form"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\" "+
          " onload=\"setPred()\">\n"+
          "<font size=+1><b>Advanced Search</b></font><p>"+
          // should add link here for usage!!!
          "<noscript>\n"+
          "<b>This page needs Javascript!</b><br>\n"+
          "Consult your browser's help pages..\n"+
          "<p><p><p>\n"+
          "</noscript>\n"+
          "<form name=\"myForm\" method=\"get\" "+
          "target=\"predResults\" onSubmit=\"return mySubmit()\">\n"+
          "Search cluster <select name=\"formAgent\">\n");
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
      out.print("</select><br>\n");
      if (nTemplatePreds > 0) {
        out.print(
            "<b>Find all </b>"+
            "<select name=\"formPred\" "+
            "onchange=\"setPred()\">\n");
        for (int i = 0; i < nTemplatePreds; i++) {
          out.print("<option>");
          out.print(templatePreds.getKey(i));
          out.print("</option>\n");
        }
        out.print(
            "</select><br>\n");
      }
      out.print(
          "<input type=\"checkbox\" name=\""+
          LIMIT+
          "\" value=\"true\" checked>"+
          "limit to "+
          DEFAULT_LIMIT+
          " matches<br>\n"+
          "<input type=\"submit\" name=\"formSubmit\" value=\"Search\"><br>\n"+
          "<p><hr>\n"+
          "Style: <select name=\""+
          PREDICATE_STYLE+
          "\">\n"+
          "<option selected>Lisp format</option>\n"+
          "<option>XML format</option>\n"+
          "</select>,&nbsp;\n"+
          "<input type=\"checkbox\" name=\""+
          PREDICATE_DEBUG+
          "\" value=\"true\">View parsed predicate<br>\n"+
          "<textarea name=\""+
          PREDICATE+
          "\" rows=15 cols=70>\n"+
          "</textarea><br>\n"+
        "<input type=\"hidden\" name=\""+
        MODE+
        "\" value=\""+
        MODE_ADVANCED_SEARCH_RESULTS+
        "\">\n"+
        "<br><hr>\n"+
        "</form>\n"+
        "<i><b>Documentation</b> is available in the \"contract\" "+
        "guide and javadocs, as "+
        "/src/org/cougaar/lib/contract/lang/index.html"+
        "</i>"+
        "</body>"+
        "</html>\n");
      out.flush();
    }

    private void displayAdvancedSearchResults()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Advanced Search Results");
      }

      String inputPred = pred;
      int inputStyle =
        (Operator.PRETTY_FLAG |
         Operator.VERBOSE_FLAG |
         (((predStyle == null) ||
           (!("xml".regionMatches(true, 0, predStyle, 0, 3)))) ?
          Operator.PAREN_FLAG :
          Operator.XML_FLAG));

      out.print("<html><head><title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " Advanced Search Results</title><head>\n"+
          "<body bgcolor=\"#F0F0F0\"><p>\n"+
          "Search <b>");
      out.print(support.getEncodedAgentName());
      out.print("</b> using ");
      out.print(
          (((inputStyle & Operator.PAREN_FLAG) != 0) ? 
           "Lisp" : "XML"));
      out.print("-style predicate: <br><pre>\n");
      out.print(inputPred);
      out.print("</pre><p>\n<hr><br>\n");

      // get an instance of the default operator factory
      OperatorFactory operFactory = OperatorFactory.getInstance();

      // parse the input to create a unary predicate
      Operator parsedPred;
      try {
        parsedPred = operFactory.create(inputStyle, inputPred);
      } catch (Exception parseE) {
        // display compile error
        out.print(
            "<font color=red size=+1>Parsing failure:</font>"+
            "<p><pre>");
        out.print(parseE.getMessage());
        out.print("</pre></body></html>");
        out.flush();
        return;
      }

      if (parsedPred == null) {
        // empty string?
        out.print(
            "<font color=red size=+1>Given empty string?</font>"+
            "</body></html>");
        out.flush();
        return;
      }

      if (predDebug) {
        // this is useful in general, but clutters the screen...
        out.print("Parsed as:<pre>\n");
        out.print(parsedPred.toString(inputStyle));
        out.print("</pre><br><hr><br>\n");
      }

      Collection col = searchUsingPredicate(parsedPred);
      int numObjects = col.size();
      Iterator oIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched Matching Objects["+numObjects+"]");
      }
      out.print(
          "<b>Note:</b> "+
          "links below will appear in the \"");
      out.print(support.getPath());
      out.print(
          "\" lower-left \"details\" "+
          "frame<p>"+
          "<center>");
      if (limit && (numObjects > DEFAULT_LIMIT)) {
        out.print("Showing first <b>");
        out.print(DEFAULT_LIMIT);
        out.print("</b> of ");
      }
      out.print("<b>");
      out.print(numObjects);
      out.print("</b> Object");
      if (numObjects != 1) {
        out.print("s");
      }
      out.print(" at ");
      out.print(support.getEncodedAgentName());
      out.print("</center>\n");
      out.print(
          "\n<table align=center border=1 cellpadding=1\n"+
          " cellspacing=1 width=75%\n"+
          " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=2>"+
          "<font size=+1 color=mediumblue><b>Matching Objects</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>UID</font></b></td>\n"+
          "<td><font color=mediumblue><b>Type</font></b></td>\n"+
          "</tr>\n");
      if (numObjects > 0) {
        // print table rows
        int rows = 0;
        while (oIter.hasNext()) {
          Object o = oIter.next();
          int itemType = getItemType(o);
          out.print(
              "<tr>\n"+
              "<td>");
          switch (itemType) {
            case ITEM_TYPE_ALLOCATION:
            case ITEM_TYPE_EXPANSION:
            case ITEM_TYPE_AGGREGATION:
            case ITEM_TYPE_DISPOSITION:
            case ITEM_TYPE_ASSET_TRANSFER:
              printLinkToPlanElement((PlanElement)o);
              break;
            case ITEM_TYPE_TASK:
              printLinkToLocalTask((Task)o);
              break;
            case ITEM_TYPE_ASSET:
              // found this asset in local blackboard
              printLinkToLocalAsset((Asset)o);
              break;
            case ITEM_TYPE_WORKFLOW:
            default:
              if (o instanceof XMLizable) {
                // XMLizable and a local UniqueObject
                printLinkToXML((XMLizable)o, true);
              } else {
                out.print("<font color=red>No XML for ");
                UID uoU;
                String uoUID;
                if (o instanceof UniqueObject) {
                  if (((uoU = ((UniqueObject)o).getUID()) != null) &&
                      ((uoUID = uoU.toString()) != null)) {
                    out.print(uoUID);
                  } else {
                    out.print("null-UID");
                  }
                } else if (o != null) {
                  out.print("non-UniqueObject");
                } else {
                  out.print("null");
                }
                out.print("</font>");
              } 
              break;
          }
          out.print(
              "</td>\n"+
              "<td>");
          if (itemType != ITEM_TYPE_OTHER) {
            out.print(ITEM_TYPE_NAMES[itemType]);
          } else {
            out.print("<font color=red>");
            out.print(o.getClass().getName());
            out.print("</font>");
          }
          out.print(
              "</td>\n"+
              "</tr>\n");
          if ((++rows % DEFAULT_LIMIT) == 0) {
            if (limit) {
              // limit to DEFAULT_LIMIT
              break;
            }
            // restart table
            out.print("</table>\n");
            out.flush();
            out.print(
                "<table align=center border=1 cellpadding=1\n"+
                " cellspacing=1 width=75%\n"+
                " bordercolordark=#660000 bordercolorlight=#cc9966>\n");
          }
        }
        // end table
        out.print("</table>\n");
      } else {
        out.print(
            "</table>"+
            "<center>"+
            "<font color=mediumblue>\n"+
            "No matching Objects found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /** END DISPLAY ROUTINES **/

    /** BEGIN PRINT ROUTINES **/

    /**
     * printTaskDetails.
     *
     * Includes support for printing early-best-latest dates
     * for END_TIMEs with VScoringFunctions.
     *
     */
    private void printTaskDetails(Task task)
    {
      out.print(
          "<ul>\n"+
          "<li>"+
          "<font size=small color=mediumblue>UID= ");
      // show uid
      UID tu;
      String tuid;
      if (((tu = task.getUID()) != null) &&
          ((tuid = tu.toString()) != null)) {
        out.print(tuid);
      } else {
        out.print("</font><font color=red>missing</font>");
      }
      out.print(
          "</font>"+
          "</li>\n"+
          "<li>"+
          "<font size=small color=mediumblue>Verb= ");
      // show verb
      Verb verb = task.getVerb();
      if (verb != null) {
        out.print(verb.toString());
      } else {
        out.print("</font><font color=red>missing");
      }
      out.print(
          "</font>"+
          "</li>\n"+
          "<li>"+
          "<font size=small color=mediumblue>"+
          "DirectObject= ");
      // link to Task's direct object
      printLinkToTaskDirectObject(task);
      out.print(
          "</font>"+
          "</li>\n"+
          "<li>"+
          "<font size=small color=mediumblue>"+
          "PlanElement= ");
      // link to plan element
      PlanElement pe = task.getPlanElement();
      printLinkToPlanElement(pe);
      out.print(
          " (");
      int peType = getItemType(pe);
      if (peType != ITEM_TYPE_OTHER) {
        out.print(ITEM_TYPE_NAMES[peType]);
      } else {
        out.print("<font color=red>");
        if (pe != null) {
          out.print(pe.getClass().getName());
        } else {
          out.print("null");
        }
        out.print("</font>");
      }
      out.print(
          ")"+
          "</font>"+
          "</li>");
      // show parent task(s) by UID
      if (task instanceof MPTask) {
        out.print(
            "<li>\n"+
            "<font size=small color=mediumblue>"+
            "ParentTasks<br>\n"+
            "<ol>\n");
        /********************************************************
         * Only want UIDs, so easy fix when getParentTasks is   *
         * replaced with getParentTaskUIDs.                     *
         ********************************************************/
        Enumeration parentsEn = ((MPTask)task).getParentTasks();
        while (parentsEn.hasMoreElements()) {
          Task pt = (Task)parentsEn.nextElement();
          out.print("<li>");
          // parents of an MPTask are always local
          printLinkToLocalTask(pt);
          out.print("</li>\n");
        }
        out.print(
            "</ol>\n"+
            "</font>\n"+
            "</li>\n");
      } else {
        out.print(
            "<li>\n"+
            "<font size=small color=mediumblue>"+
            "ParentTask= \n");
        printLinkToParentTask(task);
        out.print(
            "</font>"+
            "</li>\n");
      }
      // show preferences
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Preferences"+
          "</font>"+
          "<ol>\n");
      Enumeration enpref = task.getPreferences();
      while (enpref.hasMoreElements()) {
        Preference pref = (Preference)enpref.nextElement();
        int type = pref.getAspectType();
        out.print(
            "<font size=small color=mediumblue>"+
            "<li>");
        out.print(AspectValue.aspectTypeToString(type));
        out.print("= ");
        ScoringFunction sf = pref.getScoringFunction();
        AspectScorePoint best = sf.getBest();
        double bestVal = best.getValue();
        String bestString;
        if ((type == AspectType.START_TIME) || 
            (type == AspectType.END_TIME)) {
          if ((type == AspectType.END_TIME) &&
              (sf instanceof ScoringFunction.VScoringFunction)) {
            bestString = 
              "<br>" + 
              "Earliest " + getTimeString(getEarlyDate (sf)) + 
              "<br>" + 
              "Best " + getTimeString((long)bestVal) +
              "<br>" + 
              "Latest " + getTimeString(getLateDate (sf));
          } else {
            bestString = getTimeString((long)bestVal);
          }
        } else {
          bestString = Double.toString(bestVal);
        }
        out.print(bestString);
        out.print(
            "</li>"+
            "</font>\n");
      }
      out.print(
          "</ol>"+
          "</li>\n"+
          "<li>\n"+
          "<font size=small color=mediumblue>"+
          "PrepositionalPhrases<br>\n"+
          "<ol>\n");
      // show prepositional phrases
      Enumeration enprep = task.getPrepositionalPhrases();
      while (enprep.hasMoreElements()) {
        PrepositionalPhrase pp = 
          (PrepositionalPhrase)enprep.nextElement();
        out.print("<li>");
        if (pp != null) {
          String prep = pp.getPreposition();
          out.print("<i>");
          out.print(prep);
          out.print(" </i>");
          Object indObj = pp.getIndirectObject();
          if (!(indObj instanceof Schedule)) {
            // typical case
            printObject(indObj);
          } else {
            // display full schedule information
            Schedule sc = (Schedule)indObj;
            out.print(
                "Schedule:<ul>\n"+
                "<li>Type: ");
            out.print(sc.getScheduleType());
            if (sc.isEmpty()) {
              out.print("</li>\n<li><font color=red>empty</font>");
            } else {
              out.print("</li>\n<li>StartTime= ");
              out.print(getTimeString(sc.getStartTime()));
              out.print("</li>\n<li>EndTime= ");
              out.print(getTimeString(sc.getEndTime()));
              out.print("</li>\n");
              out.print("<li>Elements:");
              out.print("\n<ol>\n");
              Iterator iterator = new ArrayList(sc).iterator();
              while (iterator.hasNext()) {
                ScheduleElement se = (ScheduleElement)iterator.next();
                out.print(
                    "<li>StartTime= ");
                out.print(getTimeString(se.getStartTime()));
                out.print("<br>EndTime= ");
                out.print(getTimeString(se.getEndTime()));
                if (se instanceof LocationRangeScheduleElement) {
                  LocationRangeScheduleElement locSE = 
                    (LocationRangeScheduleElement)se;
                  out.print("<br>StartLocation= ");
                  out.print(locSE.getStartLocation());
                  out.print("<br>EndLocation= ");
                  out.print(locSE.getEndLocation());
                  if (locSE instanceof ItineraryElement) {
                    out.print("<br>Verb= ");
                    out.print(((ItineraryElement)locSE).getRole());
                  }
                } else if (se instanceof LocationScheduleElement) {
                  out.print("<br>Location= ");
                  out.print(((LocationScheduleElement)se).getLocation());
                }
                out.print("</li>\n");
              } 
              out.print("</ol>\n");
            }
            out.print("</li>\n</ul>\n");
          }
        } else {
          out.print("<font color=red>null</font>");
        }
        out.print("</li>");
      }
      out.print(
          "</font>"+
          "</ol>\n"+
          "</li>\n");
      out.print("</ul>\n");
      // link to XML view
      out.print("<font size=small color=mediumblue>");
      // this task is local
      printLinkToXML(task, true);
      out.print("</font>");
    }

    /**
     * Part of support for printing early-best-latest dates
     * for END_TIMEs with VScoringFunctions.
     */
    private static long getEarlyDate(ScoringFunction vsf) {
      Enumeration validRanges = getValidEndDateRanges(vsf);
      while (validRanges.hasMoreElements()) {
        AspectScoreRange range = 
          (AspectScoreRange)validRanges.nextElement();
        return 
          ((AspectScorePoint)range.getRangeStartPoint()
           ).getAspectValue().longValue();
      }
      // should be TimeSpan.MIN_VALUE!
      return 0;
    }

    /**
     * Part of support for printing early-best-latest dates
     * for END_TIMEs with VScoringFunctions.
     */
    private static long getLateDate(ScoringFunction vsf) {
      Enumeration validRanges = getValidEndDateRanges(vsf);
      while (validRanges.hasMoreElements()) {
        AspectScoreRange range = 
          (AspectScoreRange)validRanges.nextElement();
        if (!validRanges.hasMoreElements())
          return ((AspectScorePoint)range.getRangeEndPoint()
              ).getAspectValue().longValue();
      }
      return TimeSpan.MAX_VALUE;
    }

    /* Needed for support of printing early-best-latest END_TIMEs */
    private static Calendar cal = java.util.Calendar.getInstance();

    /* Needed for support of printing early-best-latest END_TIMEs */
    private static Date endOfRange;
    static {
      cal.set(2200, 0, 0, 0, 0, 0);
      cal.set(Calendar.MILLISECOND, 0);
      endOfRange = (Date) cal.getTime();
    }

    /**
     * Part of support for printing early-best-latest dates
     * for END_TIMEs with VScoringFunctions.
     */
    private static Enumeration getValidEndDateRanges(ScoringFunction sf) {
      Enumeration validRanges = 
        sf.getValidRanges(
            TimeAspectValue.create(AspectType.END_TIME, 0l),
            TimeAspectValue.create(AspectType.END_TIME, endOfRange));
      return validRanges;
    }

    /**
     * printPlanElementDetails.
     *
     * PlanElements are always in the blackboard and have UIDs, so we
     * don't need a "baseObj" (e.g. the Task that this PlanElement
     * is attached to).
     */
    private void printPlanElementDetails(PlanElement pe)
    {
      int peType = getItemType(pe);
      // show type
      if (peType != ITEM_TYPE_OTHER) {
        out.print(ITEM_TYPE_NAMES[peType]);
      } else {
        out.print(
            "<font color=red>");
        out.print(pe.getClass().getName());
        out.print(
            "</font>\n");
      }
      out.print("<ul>\n");
      // show UID
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "UID= ");
      UID peu = pe.getUID();
      out.print((peu != null) ? peu.toString() : "null");
      out.print(
          "</font>"+
          "</li>\n");
      // show task
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Task= ");
      printLinkToLocalTask(pe.getTask());
      out.print(
          "</font>"+
          "</li>\n");
      // show plan
      Plan plan = pe.getPlan();
      if (plan != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Plan= ");
        out.print(plan.getPlanName());
        out.print(
            "</font>"+
            "</li>\n");
      }
      // show allocation results
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Allocation Results</font>\n"+
          "<ul>\n");
      AllocationResult ar;
      if ((ar = pe.getEstimatedResult()) != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Estimated</font>");
        printAllocationResultDetails(ar);
        out.print(
            "</li>\n");
      }
      if ((ar = pe.getReportedResult()) != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Reported</font>");
        printAllocationResultDetails(ar);
        out.print(
            "</li>\n");
      }
      if ((ar = pe.getReceivedResult()) != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Received</font>");
        printAllocationResultDetails(ar);
        out.print(
            "</li>\n");
      }
      if ((ar = pe.getObservedResult()) != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Observed</font>");
        printAllocationResultDetails(ar);
        out.print(
            "</li>\n");
      }
      out.print(
          "</ul>"+
          "</li>\n");
      // show PE subclass information
      switch (peType) {
        case ITEM_TYPE_ALLOCATION:
          printAllocationDetails((Allocation)pe);
          break;
        case ITEM_TYPE_EXPANSION:
          printExpansionDetails((Expansion)pe);
          break;
        case ITEM_TYPE_AGGREGATION:
          printAggregationDetails((Aggregation)pe);
          break;
        case ITEM_TYPE_DISPOSITION:
          printDispositionDetails((Disposition)pe);
          break;
        case ITEM_TYPE_ASSET_TRANSFER:
          printAssetTransferDetails((AssetTransfer)pe);
          break;
        default: // other
          out.print(
              "<li>"+
              "<font color=red>"+
              "No details for class ");
          out.print(pe.getClass().getName());
          out.print("</font></li>");
          break;
      }
      out.print("</ul>\n");
      // link to XML view
      out.print("<font size=small color=mediumblue>");
      // planElements are always local
      printLinkToXML(pe, true);
      out.print("</font>");
    }

    /**
     * printAllocationResultDetails.
     */
    private void printAllocationResultDetails(AllocationResult ar)
    {
      out.print(
          "<ul>\n"+
          "<font size=small color=mediumblue>"+
          "<li>"+
          "isSuccess= ");
      // show isSuccess
      out.print(ar.isSuccess());
      out.print(
          "</li>"+
          "</font>\n"+
          "<font size=small color=mediumblue>"+
          "<li>"+
          "Confidence= ");
      // show confidence rating
      out.print(ar.getConfidenceRating());
      out.print(
          "</li>"+
          "</font>\n");
      // for all (type, result) pairs
      int[] arTypes = ar.getAspectTypes();
      double[] arResults = ar.getResult();
      for (int i = 0; i < arTypes.length; i++) {
        out.print(
            "<font size=small color=mediumblue>"+
            "<li>");
        // show type
        int arti = arTypes[i];
        out.print(AspectValue.aspectTypeToString(arti));
        out.print("= ");
        // show value
        double arri = arResults[i];
        switch (arti) {
          case AspectType.START_TIME:
          case AspectType.END_TIME:
          case AspectType.POD_DATE:
            // date
            out.print(
                getTimeString((long)arri));
            break;
          default:
            // other
            out.print(arri);
            break;
        }
        out.print(
            "</li>"+
            "</font>\n");
      }
      // show phased details
      if (ar.isPhased()) {
        out.print(
            "<font size=small color=mediumblue>"+
            "<li>"+
            "isPhased= true"+
            "</li>"+
            "</font>\n");
        // user likely not interested in phased results
      }
      out.print(
          "</ul>\n");
    }

    /**
     * printAllocationDetails.
     */
    private void printAllocationDetails(Allocation ac)
    {
      // show asset
      Asset asset = ac.getAsset();
      if (asset != null) {
        // link to allocated asset
        ClusterPG clusterPG = asset.getClusterPG();
        MessageAddress agentID;
        String remoteAgentID =
          ((((clusterPG = asset.getClusterPG()) != null) &&
            ((agentID = clusterPG.getMessageAddress()) != null)) ?
           agentID.toString() :
           null);
        boolean isRemoteAgent = (remoteAgentID != null);
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>");
        out.print(isRemoteAgent ? "Agent" : "Asset");
        out.print("= ");
        // allocations are always to an asset in the local blackboard
        printLinkToLocalAsset(asset);
        out.print(
            "</font>"+
            "</li>\n");
        if (isRemoteAgent) {
          // link to task in other cluster
          String encRemoteAgentID = 
            support.encodeAgentName(remoteAgentID);
          Task allocTask = ((AllocationforCollections)ac).getAllocationTask();
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "AllocTask= ");
          printLinkToTask(
              allocTask, 
              encRemoteAgentID);
          out.print(
              "</font>"+
              "</li>\n");
        }
      } else {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Asset= </font>"+
            "<font color=red>null</font>"+
            "</li>\n");
      }
    }

    /**
     * printExpansionDetails.
     */
    private void printExpansionDetails(Expansion ex)
    {
      // link to child tasks
      out.print(
          "<li>"+
          "<font size=small color=black>"+
          "<i>Child Tasks</i>"+
          "</font>"+
          "<ol>\n");
      Enumeration en = ex.getWorkflow().getTasks();
      while (en.hasMoreElements()) {
        Task tsk = (Task)en.nextElement();
        out.print(
            "<font size=small color=mediumblue>"+
            "<li>");
        // expanded task is always local
        printLinkToLocalTask(tsk);
        out.print(
            "</li>"+
            "</font>");
      }
      out.print(
          "</ol>"+
          "</li>\n");
    }

    /**
     * printAggregationDetails.
     */
    private void printAggregationDetails(Aggregation agg)
    {
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "MPTask= ");
      Composition comp = agg.getComposition();
      if (comp != null) {
        // link to composed mp task
        Task compTask = comp.getCombinedTask();
        // composed task is always local
        printLinkToLocalTask(compTask);
      } else {
        out.print("<font color=red>null Composition</font>");
      }
      out.print(
          "</font>\n"+
          "</li>\n");
    }

    /**
     * printDispositionDetails.
     */
    private void printDispositionDetails(Disposition d)
    {
      // nothing to say?
      out.print(
          "<font size=small color=mediumblue>"+
          "Success= ");
      out.print(d.isSuccess());
      out.print("</font>\n");
    }

    /**
     * printAssetTransferDetails.
     */
    private void printAssetTransferDetails(AssetTransfer atrans)
    {
      // show attached asset
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Asset= ");
      printLinkToAssetTransferAsset(atrans);
      out.print(
          "</font>"+
          "</li>\n");
      // show role
      Role role = atrans.getRole();
      if (role != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Role= ");
        out.print(role.getName());
        out.print(
            "</font>"+
            "</li>\n");
      }
      // show assignor
      MessageAddress assignor = atrans.getAssignor();
      if (assignor != null) {
        String name = assignor.toString();
        String encName = 
          ((name != null) ?
           (support.encodeAgentName(name)) :
           (null));
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Assignor= ");
        printLinkToTasksSummary(encName);
        out.print(
            "</font>"+
            "</li>\n");
      }
      // show assignee
      Asset assignee = atrans.getAssignee();
      if (assignee != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Assignee= ");
        // assignee asset is always in the local blackboard
        printLinkToLocalAsset(assignee);
        out.print(
            "</font>"+
            "</li>\n");
      }
    }

    /**
     * printAssetDetails.
     */
    private void printAssetDetails(
        UniqueObject baseObj, Asset asset)
    {
      if (asset instanceof AssetGroup) {
        // recursive for AssetGroups!
        List assets = ((AssetGroup)asset).getAssets();
        int nAssets = ((assets != null) ? assets.size() : 0);
        out.print("AssetGroup[");
        out.print(nAssets);
        out.print("]:\n<ol>\n");
        for (int i = 0; i < nAssets; i++) {
          Asset as = (Asset)assets.get(i);
          out.print("<li>\n");
          if (as != null) {
            // recurse!
            //
            // unable to show XML for elements, so pass null baseObj
            printAssetDetails(null, as);
          } else {
            out.print("<font color=red>null</font>");
          }
          out.print("\n</li>\n");
        }
        out.print("</ol>\n");
        if (baseObj != null) {
          if (asset instanceof XMLizable) {
            // link to HTML-encoded XML view
            out.print("<font size=small color=mediumblue>");
            printLinkToAttachedXML(
                baseObj,
                (XMLizable)asset,
                true);
            out.print("</font>");
          } else {
            // asset not XMLizable
            out.print("<font color=red>Asset not XMLable</font>");
          }
        }
        return;
      }
      // if asset is an aggregate, info_asset is the
      // aggregate's asset which contains Type and Item info.
      Asset info_asset = asset;
      int quantity = 1;
      boolean isAggregateAsset = (asset instanceof AggregateAsset);
      if (isAggregateAsset) {
        do {
          AggregateAsset agg = (AggregateAsset)info_asset;
          quantity *= (int)agg.getQuantity();
          info_asset = agg.getAsset();
        } while (info_asset instanceof AggregateAsset);
        if (info_asset == null) {
          // bad!  should throw exception, but I doubt this will
          // ever happen...
          info_asset = asset;
        }
      }
      out.print("<ul>\n");
      if (isAggregateAsset) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Quantity= ");
        // show quantity
        out.print(quantity);
        out.print(
            "</font>"+
            "</li>\n");
      } else {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "UID= ");
        // show UID
        UID u = asset.getUID();
        String foundUID = ((u != null) ? u.toString() : "null");
        out.print(foundUID);
        out.print(
            "</font>"+
            "</li>\n");
      }
      // show class
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Class= ");
      out.print(info_asset.getClass().getName());
      out.print(
          "</font>"+
          "</li>\n");
      // show type id info
      TypeIdentificationPG tipg = info_asset.getTypeIdentificationPG();
      if (tipg != null) {
        String tiid = tipg.getTypeIdentification();
        if (tiid != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "TypeID= ");
          out.print(tiid);
          out.print(
              "</font>"+
              "</li>");
        }
        String tin = tipg.getNomenclature();
        if (tin != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "TypeNomenclature= ");
          out.print(tin);
          out.print(
              "</font>"+
              "</li>");
        }
        String tiati = tipg.getAlternateTypeIdentification();
        if (tiati != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "AlternateTypeID= ");
          out.print(tiati);
          out.print(
              "</font>"+
              "</li>");
        }
      } else {
        out.print(
            "<li>"+
            "<font color=red>"+
            "TypeID missing"+
            "</font>"+
            "</li>\n");
      }
      // show item id
      ItemIdentificationPG iipg = info_asset.getItemIdentificationPG();
      if (iipg != null) {
        String iiid = iipg.getItemIdentification();
        if (iiid != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "ItemID= ");
          out.print(iiid);
          out.print(
              "</font>"+
              "</li>");
        }
        String iin = iipg.getNomenclature();
        if (iin != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "ItemNomenclature= ");
          out.print(iin);
          out.print(
              "</font>"+
              "</li>");
        }
        String iiati = iipg.getAlternateItemIdentification();
        if (iiati != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "AlternateItemID= ");
          out.print(iiati);
          out.print(
              "</font>"+
              "</li>");
        }
      } else {
        out.print(
            "<li>"+
            "<font color=red>"+
            "ItemID missing"+
            "</font>"+
            "</li>\n");
      }
      // show role schedule
      RoleSchedule rs;
      Schedule sc;
      if (((rs = asset.getRoleSchedule()) != null) &&
          ((sc = rs.getAvailableSchedule()) != null) &&
          !sc.isEmpty() ) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "RoleSchedule<br>\n"+
            "Start= ");
        out.print(getTimeString(sc.getStartTime()));
        out.print("<br>End= ");
        out.print(getTimeString(sc.getEndTime()));
        out.print("<br>\n");
        Enumeration rsEn = rs.getRoleScheduleElements();
        if (rsEn.hasMoreElements()) {
          out.print(
              "RoleScheduleElements<br>\n"+
              "<ol>\n");
          do {
            PlanElement pe = (PlanElement)rsEn.nextElement();
            out.print("<li>");
            // planElements are always local
            printLinkToPlanElement(pe);
            out.print("</li>\n");
          } while (rsEn.hasMoreElements());
          out.print("</ol>\n");
        } else {
          out.print("RoleScheduleElements: none<br>\n");
        }
        Iterator iterator = new ArrayList(sc).iterator();
        if (iterator.hasNext()) {
          out.print(
              "AvailableScheduleElements<br>\n"+
              "<ol>\n");
          while (iterator.hasNext()) {
            ScheduleElement se = (ScheduleElement)iterator.next();
            out.print(
                "<li>Start= ");
            out.print(getTimeString(se.getStartTime()));
            out.print("<br>End= ");
            out.print(getTimeString(se.getEndTime()));
            out.print("</li>\n");
          } 
          out.print("</ol>\n");
        } else {
          out.print("AvailableScheduleElements: none<br>\n");
        }
        out.print(
            "</font>"+
            "</li>\n");
      }
      // show location schedule
      LocationSchedulePG locSchedPG;
      Schedule locSched;
      if (((locSchedPG = asset.getLocationSchedulePG()) != null) &&
          ((locSched = locSchedPG.getSchedule()) != null) &&
          (!(locSched.isEmpty()))) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "LocationSchedule<br>\n"+
            "Start= ");
        out.print(getTimeString(locSched.getStartTime()));
        out.print("<br>End= ");
        out.print(getTimeString(locSched.getEndTime()));
        out.print("<br>\n");
        Enumeration locSchedEn = locSched.getAllScheduleElements();
        if (locSchedEn.hasMoreElements()) {
          out.print(
              "LocationScheduleElements<br>\n"+
              "<ol>\n");
          do {
            ScheduleElement se = (ScheduleElement)locSchedEn.nextElement();
            out.print(
                "<li>Start= ");
            out.print(getTimeString(se.getStartTime()));
            out.print("<br>End= ");
            out.print(getTimeString(se.getEndTime()));
            if (se instanceof LocationScheduleElement) {
              LocationScheduleElement lse = (LocationScheduleElement)se;
              Location loc = lse.getLocation();
              if (loc != null) {
                out.print("<br>Location= \"");
                out.print(loc);
                out.print("\"");
              }
            }
            out.print("</li>\n");
          } while (locSchedEn.hasMoreElements());
          out.print("</ol>\n");
        } else {
          out.print("LocationScheduleElements: none<br>\n");
        }
        out.print(
            "</font>"+
            "</li>\n");
      }
      // PGs?
      out.print("</ul>");
      if (baseObj != null) {
        // provide XML view
        if (asset instanceof XMLizable) {
          // link to HTML-encoded XML view
          out.print("<font size=small color=mediumblue>");
          printLinkToAttachedXML(
              baseObj,
              (XMLizable)asset,
              true);
          out.print("</font>");
        } else {
          // asset not XMLizable
          out.print("<font color=red>Asset not XMLable</font>");
        }
      } else {
        // likely recursed on an AssetGroup, and the top-level group
        //   had a "View XML" link.
      }
    }

    /**
     * printAssetTableRow.
     *
     * Asset that is in the local blackboard and has a UID.  Treat this
     * as an Asset attached to itself.
     */
    private void printAssetTableRow(Asset asset)
    {
      printAttachedAssetTableRow(
          asset,
          asset,
          MODE_ASSET_DETAILS);
    }

    /**
     * printTaskDirectObjectTableRow.
     */
    private void printTaskDirectObjectTableRow(Task task)
    {
      printAttachedAssetTableRow(
          task,
          ((task != null) ? task.getDirectObject() : null),
          MODE_TASK_DIRECT_OBJECT_DETAILS);
    }

    /**
     * printAttachedAssetTableRow.
     * <p>
     * Print asset information in three table columns:<br>
     * <ol>
     *   <li>UID</li>
     *   <li>TypeID</li>
     *   <li>ItemID</li>
     *   <li>Quantity</li>
     * </ol>
     * Be sure to have a corresponding table!
     *
     * @see #printTaskDirectObjectTableRow
     */
    private void printAttachedAssetTableRow(
        UniqueObject baseObj, Asset asset, int baseMode)
    {
      if ((baseObj == null) ||
          (asset == null)) {
        out.print(
            "<td colspan=4>"+
            "<font color=red>null</font>"+
            "</td>\n");
      } else if (asset instanceof AssetGroup) {
        // link to asset group
        //   "UID" of the baseObj, and a link using that UID
        //   "TypeID" is a bold "AssetGroup"
        //   "ItemID" is "N/A"
        //   "Quantity" is the number of items in the group
        out.print("<td>");
        printLinkToAttachedAsset(baseObj, asset, baseMode);
        out.print(
            "</td>\n"+
            "<td>"+
            "<b>AssetGroup</b>"+
            "</td>\n"+
            "<td>"+
            "N/A"+
            "</td>\n"+
            "<td align=right>");
        List assets = ((AssetGroup)asset).getAssets();
        int nAssets = ((assets != null) ? assets.size() : 0);
        out.print(nAssets);
        out.print(
            "</td>\n");
      } else {
        // if asset is an aggregate, info_asset is the
        // aggregate's asset which contains Type and Item info.
        Asset info_asset;
        int quantity;
        if (asset instanceof AggregateAsset) {
          info_asset = asset;
          quantity = 1;
          do {
            AggregateAsset agg = (AggregateAsset)info_asset;
            quantity *= (int)agg.getQuantity();
            info_asset = agg.getAsset();
          } while (info_asset instanceof AggregateAsset);
          if (info_asset == null) {
            out.print(
                "<td colspan=4>"+
                "<font color=red>null</font>"+
                "</td>\n");
            return;
          }
        } else {
          info_asset = asset;
          if (asset instanceof AssetGroup) {
            List assets = ((AssetGroup)asset).getAssets();
            quantity = ((assets != null) ? assets.size() : 0);
          } else {
            quantity = 1;
          }
        }
        // link to asset
        out.print("<td>");
        printLinkToAttachedAsset(baseObj, asset, baseMode);
        out.print(
            "</td>\n"+
            "<td>");
        // show type id
        TypeIdentificationPG tipg = info_asset.getTypeIdentificationPG();
        if (tipg != null) {
          out.print(
              tipg.getTypeIdentification());
        } else {
          out.print("<font color=red>missing typeID</font>");
        }
        out.print(
            "</td>\n"+
            "<td>");
        // show item id
        ItemIdentificationPG iipg = info_asset.getItemIdentificationPG();
        if (iipg != null) {
          out.print(
              iipg.getItemIdentification());
        } else {
          out.print("<font color=red>missing itemID</font>");
        }
        out.print(
            "</td>\n"+
            "<td align=right>");
        // show quantity
        out.print(quantity);
        out.print("</td>\n");
      }
    }

    /**
     * printXMLizableDetails.
     * <p>
     * Prints XML for given XMLizable Object.
     * <p>
     * Considered embedding some Applet JTree viewer, e.g.<br>
     * <code>ui.planviewer.XMLViewer</code>
     * but would need separate Applet code.
     * <p>
     * Also considered using some nifty javascript XML tree viewer, e.g.<br>
     * <code>http://developer.iplanet.com/viewsource/smith_jstree/smith_jstree.html</code><br>
     * but would take some work...
     * <p>
     * @param printAsHTML uses XMLtoHTMLOutputStream to pretty-print the XML
     */
    private void printXMLizableDetails(
        XMLizable xo, boolean printAsHTML)
    {
      try {
        // convert to XML
        Document doc = new DocumentImpl();
        Element element = xo.getXML((Document)doc);
        doc.appendChild(element);

        // print to output
        if (printAsHTML) {
          OutputFormat format = new OutputFormat();
          format.setPreserveSpace(false);
          format.setIndent(2);

          PrintWriter pout = new PrintWriter(new XMLtoHTMLOutputStream(out));
          XMLSerializer serializer = new XMLSerializer(pout, format);
          out.print("<pre>\n");
          serializer.serialize(doc);
          out.print("\n</pre>\n");
          pout.flush();
        } else {
          OutputFormat format = new OutputFormat();
          format.setPreserveSpace(true);

          PrintWriter pout = new PrintWriter(out);
          XMLSerializer serializer = new XMLSerializer(pout, format);
          serializer.serialize(doc);
          pout.flush();
        }
      } catch (Exception e) {
        if (printAsHTML) {
          out.print("\nException!\n\n");
          e.printStackTrace(out);
        }
      }
    }

    /** END PRINT ROUTINES **/

    /** BEGIN PRINTLINK ROUTINES **/

    /**
     * print link to task summary at this cluster.
     */

    private void printLinkToTasksSummary()
    {
      printLinkToTasksSummary(
          support.getEncodedAgentName());
    }

    /**
     * print link to task summary for given cluster
     */
    private void printLinkToTasksSummary(
        String encodedAgentName)
    {
      if (encodedAgentName != null) {
        out.print("<a href=\"/$");
        // link to cluster
        out.print(encodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_TASKS_SUMMARY);
        out.print("\" target=\"tablesFrame\">");
        out.print(support.getEncodedAgentName());
        out.print(
            "</a>");
      } else {
        out.print("<font color=red>Unknown cluster</font>");
      }
    }

    /** simple flags for parameter checking **/
    private static final byte _FLAG_LIMIT   = (1 << 0);
    private static final byte _FLAG_VERB    = (1 << 1);
    private static final byte _FLAG_VERBOSE = (1 << 2);
    private static final byte _FLAG_SORT    = (1 << 3);

    /**
     * printLinkToAllTasks for the local cluster.
     */
    private void printLinkToAllTasks(
        String verb, int limit, int numTasks, boolean verbose)
    {
      printLinkToAllTasks(
          support.getEncodedAgentName(),
          verb, limit, numTasks, verbose);
    }

    /**
     * printLinkToAllTasks.
     */
    private void printLinkToAllTasks(
        String encodedAgentName,
        String verb, int limit, int numTasks, boolean verbose)
    {
      if (encodedAgentName != null) {
        out.print("<a href=\"/$");
        out.print(encodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_ALL_TASKS);
        // set flags
        byte flags = 0;
        if (limit > 0) {
          out.print(
              "&"+
              LIMIT+
              "=true");
          flags |= _FLAG_LIMIT;
        }
        if (verb != null) {
          out.print(
              "&"+
              VERB+
              "=");
          out.print(verb);
          flags |= _FLAG_VERB;
        }
        if (verbose) {
          flags |= _FLAG_VERBOSE;
        }
	if (sortByUID)
	  out.print ("&" + SORT_BY_UID + "=true");

        out.print("\" target=\"tablesFrame\">");
        // print over-customized output .. make parameter?
        switch (flags) {
          case (_FLAG_LIMIT):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b>");
            break;
          case (_FLAG_LIMIT | _FLAG_VERBOSE):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b> of <b>");
            out.print(numTasks);
            out.print("</b> Tasks at ");
            out.print(encodedAgentName);
            break;
          case (_FLAG_LIMIT | _FLAG_VERB):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b>");
            break;
          case (_FLAG_LIMIT | _FLAG_VERB | _FLAG_VERBOSE):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b> of <b>");
            out.print(numTasks);
            out.print("</b> Tasks with verb ");
            out.print(verb);
            out.print("at ");
            out.print(encodedAgentName);
            break;
          case (_FLAG_VERB):
            out.print(verb);
            break;
          case (_FLAG_VERB | _FLAG_VERBOSE):
            out.print("View all <b>");
            out.print(numTasks);
            out.print("</b> Tasks with verb ");
            out.print(verb);
            out.print(" at ");
            out.print(encodedAgentName);
            break;
          default:
          case (0):
          case (_FLAG_VERBOSE):
            out.print("View all <b>");
            out.print(numTasks);
            out.print("</b> Tasks at ");
            out.print(support.getEncodedAgentName());
            break;
        }
        out.print("</a>");
      } else {
        out.print("<font color=red>Unknown cluster</font>");
      }
    }

    /**
     * printLinkToAllPlanElements for the local cluster.
     */
    private void printLinkToAllPlanElements(
        int limit, int numPlanElements, boolean verbose)
    {
      printLinkToAllPlanElements(
          support.getEncodedAgentName(),
          limit, numPlanElements, verbose);
    }

    /**
     * printLinkToAllPlanElements.
     */
    private void printLinkToAllPlanElements(
        String encodedAgentName,
        int limit, int numPlanElements, boolean verbose)
    {
      if (encodedAgentName != null) {
        out.print("<a href=\"/$");
        out.print(encodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_ALL_PLAN_ELEMENTS);
        // set flags
        byte flags = 0;
        if (limit > 0) {
          out.print(
              "&"+
              LIMIT+
              "=true");
          flags |= _FLAG_LIMIT;
        }
        if (verbose) {
          flags |= _FLAG_VERBOSE;
        }
        out.print("\" target=\"tablesFrame\">");
        // print over-customized output .. make parameter?
        switch (flags) {
          case (_FLAG_LIMIT):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b>");
            break;
          case (_FLAG_LIMIT | _FLAG_VERBOSE):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b> of <b>");
            out.print(numPlanElements);
            out.print("</b> PlanElements at ");
            out.print(encodedAgentName);
            break;
          default:
          case (0):
          case (_FLAG_VERBOSE):
            out.print("View all <b>");
            out.print(numPlanElements);
            out.print("</b> PlanElements at ");
            out.print(encodedAgentName);
            break;
        }
        out.print("</a>");
      } else {
        out.print("<font color=red>Unknown cluster</font>");
      }
    }

    /**
     * printLinkToAllAssets for the local cluster.
     */
    private void printLinkToAllAssets(
        int limit, int numAssets, boolean verbose)
    {
      printLinkToAllAssets(
          support.getEncodedAgentName(),
          limit, numAssets, verbose);
    }

    /**
     * printLinkToAllAssets.
     */
    private void printLinkToAllAssets(
        String encodedAgentName,
        int limit, int numAssets, boolean verbose)
    {
      if (encodedAgentName != null) {
        out.print("<a href=\"/$");
        out.print(encodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_ALL_ASSETS);
        // set flags
        byte flags = 0;
        if (limit > 0) {
          out.print(
              "&"+
              LIMIT+
              "=true");
          flags |= _FLAG_LIMIT;
        }
        if (verbose) {
          flags |= _FLAG_VERBOSE;
        }
        out.print("\" target=\"tablesFrame\">");
        // print over-customized output .. make parameter?
        switch (flags) {
          case (_FLAG_LIMIT):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b>");
            break;
          case (_FLAG_LIMIT | _FLAG_VERBOSE):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b> of <b>");
            out.print(numAssets);
            out.print("</b> Assets at ");
            out.print(encodedAgentName);
            break;
          default:
          case (0):
          case (_FLAG_VERBOSE):
            out.print("View all <b>");
            out.print(numAssets);
            out.print("</b> Assets at ");
            out.print(encodedAgentName);
            break;
        }
        out.print("</a>");
      } else {
        out.print("<font color=red>Unknown cluster</font>");
      }
    }

    /**
     * printLinkToAllUniqueObjects for the local cluster.
     */
    private void printLinkToAllUniqueObjects(
        int limit, int numUniqueObjects, boolean verbose)
    {
      printLinkToAllUniqueObjects(
          support.getEncodedAgentName(),
          limit, numUniqueObjects, verbose);
    }

    /**
     * printLinkToAllUniqueObjects.
     */
    private void printLinkToAllUniqueObjects(
        String encodedAgentName,
        int limit, int numUniqueObjects, boolean verbose)
    {
      if (encodedAgentName != null) {
        out.print("<a href=\"/$");
        out.print(encodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_ALL_UNIQUE_OBJECTS);
        // set flags
        byte flags = 0;
        if (limit > 0) {
          out.print(
              "&"+
              LIMIT+
              "=true");
          flags |= _FLAG_LIMIT;
        }
        if (verbose) {
          flags |= _FLAG_VERBOSE;
        }
        out.print("\" target=\"tablesFrame\">");
        // print over-customized output .. make parameter?
        switch (flags) {
          case (_FLAG_LIMIT):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b>");
            break;
          case (_FLAG_LIMIT | _FLAG_VERBOSE):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b> of <b>");
            out.print(numUniqueObjects);
            out.print("</b> UniqueObjects at ");
            out.print(encodedAgentName);
            break;
          default:
          case (0):
          case (_FLAG_VERBOSE):
            out.print("View all <b>");
            out.print(numUniqueObjects);
            out.print("</b> UniqueObjects at ");
            out.print(encodedAgentName);
            break;
        }
        out.print("</a>");
      } else {
        out.print("<font color=red>Unknown cluster</font>");
      }
    }

    /**
     * printLinkToParentTask.
     * <p>
     * Get task's parent before linking.
     */
    private void printLinkToParentTask(Task task)
    {
      UID ptU;
      String ptUID;
      if (task == null) {
        out.print("<font color=red>null</font>");
      } else if (((ptU = task.getParentTaskUID()) == null) ||
          ((ptUID = ptU.toString()) == null)) {
        out.print("<font color=red>parent not unique</font>");
      } else {
        MessageAddress tClusterID = task.getSource();
        String ptEncodedAgentName;
        if ((tClusterID == null) ||
            ((ptEncodedAgentName = tClusterID.toString()) == null)) {
          ptEncodedAgentName = support.getEncodedAgentName();
        } else {
          ptEncodedAgentName = support.encodeAgentName(ptEncodedAgentName);
        }
        out.print("<a href=\"/$");
        out.print(ptEncodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_TASK_DETAILS+
            "&"+
            ITEM_UID+
            "=");
        out.print(encodeUID(ptUID));
        out.print("\" target=\"itemFrame\">");
        out.print(ptUID);
        out.print("</a>");
      }
    }

    /**
     * printLinkToLocalTask.
     * <p>
     * Tasks that stay in the current cluster.
     */
    private void printLinkToLocalTask(Task task)
    {
      printLinkToTask(
          task, 
          support.getEncodedAgentName());
    }

    /**
     * printLinkToTask.
     * <p>
     * This method attempts to works around task forwarding across
     * clusters in the "Down" sense, i.e. allocations.
     */
    private void printLinkToTask(
        Task task, 
        String atEncodedAgentName)
    {
      UID taskU;
      String taskUID;
      if (task == null) {
        out.print("<font color=red>null</font>");
      } else if (((taskU = task.getUID()) == null) ||
          ((taskUID = taskU.toString()) == null)) {
        out.print("<font color=red>not unique</font>");
      } else {
        out.print("<a href=\"/$");
        out.print(atEncodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_TASK_DETAILS+
            "&"+
            ITEM_UID+
            "=");
        out.print(encodeUID(taskUID));
        out.print("\" target=\"itemFrame\">");
        out.print(taskUID);
        out.print("</a>");
      }
    }

    /**
     * printLinkToPlanElement.
     * <p>
     * PlanElements stay in their cluster
     */
    private void printLinkToPlanElement(PlanElement pe)
    {
      UID peU;
      String peUID;
      if (pe == null) {
        out.print("<font color=red>null</font>\n");
      } else if (((peU = pe.getUID()) == null) ||
          ((peUID = peU.toString()) == null)) {
        out.print("<font color=red>not unique</font>\n");
      } else {
        out.print("<a href=\"/$");
        out.print(support.getEncodedAgentName());
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_PLAN_ELEMENT_DETAILS+
            "&"+
            ITEM_UID+
            "=");
        out.print(encodeUID(peUID));
        out.print("\" target=\"itemFrame\">");
        out.print(peUID);
        out.print("</a>");
      }
    }

    /**
     * printLinkToLocalAsset.
     * <p>
     * Asset that is in the local blackboard and has a UID.  Treat this
     * as an Asset attached to itself.
     **/
    private void printLinkToLocalAsset(Asset asset)
    {
      printLinkToAttachedAsset(
          asset, 
          asset,
          MODE_ASSET_DETAILS);
    }

    /**
     * printLinkToTaskDirectObject.
     **/
    private void printLinkToTaskDirectObject(Task task)
    {
      printLinkToAttachedAsset(
          task, 
          ((task != null) ? task.getDirectObject() : null),
          MODE_TASK_DIRECT_OBJECT_DETAILS);
    }

    /**
     * printLinkToAssetTransferAsset.
     **/
    private void printLinkToAssetTransferAsset(AssetTransfer atrans)
    {
      printLinkToAttachedAsset(
          atrans, 
          ((atrans != null) ? atrans.getAsset() : null),
          MODE_ASSET_TRANSFER_ASSET_DETAILS);
    }

    /**
     * printLinkToAttachedAsset.
     *
     * @see #printLinkToTaskDirectObject
     * @see #printLinkToAssetTransferAsset
     **/
    private void printLinkToAttachedAsset(
        UniqueObject baseObj, Asset asset, 
        int baseMode)
    {
      UID baseObjU;
      String baseObjUID;
      if ((baseObj == null) ||
          (asset == null)) {
        out.print("<font color=red>null</font>");
      } else if (((baseObjU = baseObj.getUID()) == null) ||
          ((baseObjUID = baseObjU.toString()) == null)) {
        out.print("<font color=red>not unique</font>");
      } else {
        out.print("<a href=\"/$");
        out.print(support.getEncodedAgentName());
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            baseMode+
            "&"+
            ITEM_UID+
            "=");
        out.print(encodeUID(baseObjUID));
        out.print("\" target=\"itemFrame\">");
        String assetName;
        if (asset == baseObj) {
          // asset it it's own base
          assetName = baseObjUID;
        } else {
          UID assetU;
          // asset attached to the base UniqueObject
          if (((assetU = asset.getUID()) == null) ||
              ((assetName = assetU.toString()) == null)) {
            if (asset instanceof AggregateAsset) {
              assetName = "Non-UID Aggregate";
            } else if (asset instanceof AssetGroup) {
              assetName = "Non-UID Group";
            } else {
              assetName = "Non-UID "+asset.getClass().getName();
            }
          }
        }
        out.print(assetName);
        out.print("</a>");
      }
    }

    /**
     * printLinkToXML.
     * <p>
     * XML objects stay in cluster.
     **/
    private void printLinkToXML(
        UniqueObject uo, boolean asHTML)
    {
      if (uo instanceof XMLizable) {
        // link to HTML-encoded XML view
        printLinkToAttachedXML(
            uo,
            (XMLizable)uo,
            asHTML);
      } else if (uo == null) {
        out.print("<font color=red>null</font>");
      } else {
        // uniqueObject not XMLizable
        out.print("<font color=red>No XML for ");
        UID uoU;
        String uoUID;
        if (((uoU = uo.getUID()) != null) &&
            ((uoUID = uoU.toString()) != null)) {
          out.print(uoUID);
        } else {
          out.print("null");
        }
        out.print("</font>");
      }
    }

    /**
     * printLinkToXML.
     * <p>
     * XML objects stay in cluster.
     **/
    private void printLinkToXML(
        XMLizable xo, boolean asHTML)
    {
      if (xo instanceof UniqueObject) {
        // link to HTML-encoded XML view
        printLinkToAttachedXML(
            (UniqueObject)xo,
            xo,
            asHTML);
      } else if (xo == null) {
        out.print("<font color=red>null</font>");
      } else {
        // asset not XMLizable
        out.print("<font color=red>");
        out.print(xo.getClass().getName());
        out.print(" not a UniqueObject</font>");
      }
    }

    /**
     * printLinkToAttachedXML.
     **/
    private void printLinkToAttachedXML(
        UniqueObject baseObj, XMLizable xo, 
        boolean asHTML)
    {
      UID baseObjU;
      String baseObjUID;
      if ((xo == null) ||
          (baseObj == null) ||
          ((baseObjU = baseObj.getUID()) == null) ||
          ((baseObjUID = baseObjU.toString()) == null)) {
        if (asHTML) {
          out.print("<font color=red>Unable to view XML</font>\n");
        } else {
          out.print("<font color=red>Raw XML unavailable</font>\n");
        }
      } else {
        out.print("<a href=\"/$");
        out.print(support.getEncodedAgentName());
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "=");
        int mode =
          ((xo == baseObj) ?
           (asHTML ? 
            MODE_XML_HTML_DETAILS :
            MODE_XML_RAW_DETAILS) :
           (asHTML ? 
            MODE_XML_HTML_ATTACHED_DETAILS :
            MODE_XML_RAW_ATTACHED_DETAILS));
        out.print(mode);
        out.print(
            "&"+
            ITEM_UID+
            "=");
        String encBaseObjUID = encodeUID(baseObjUID);
        out.print(encBaseObjUID);
        out.print("\" target=\"xml_");
        out.print(encBaseObjUID);
        out.print("_page\">");
        String xoName;
        if (xo == baseObj) {
          xoName = baseObjUID;
        } else {
          if (xo instanceof UniqueObject) {
            UID xoU;
            if (((xoU = ((UniqueObject)xo).getUID()) == null) ||
                ((xoName = xoU.toString()) == null)) {
              if (xo instanceof AggregateAsset) {
                xoName = "Non-UID Aggregate";
              } else if (xo instanceof AssetGroup) {
                xoName = "Non-UID Group";
              } else {
                xoName = "Non-UID "+xo.getClass().getName();
              }
            }
          } else {
            xoName = "Non-UniqueObject "+xo.getClass().getName();
          }
        }
        if (asHTML) {
          out.print("View XML for ");
          out.print(xoName);
        } else {
          out.print("Raw XML for ");
          out.print(xoName);
        }
        out.print("</a>\n");
      }
    }

    /** END PRINTLINK ROUTINES **/

    /** BEGIN UTILITY PARSERS **/

    /**
     * printObject.
     * <p>
     * Currently used to print Preposition.getIndirectObject()
     * <p>
     * recursive for AssetGroups!
     */
    private void printObject(Object io)
    {
      try {
        if (io == null) {
          out.print("<font color=red>null</font>");
        } else if (io instanceof String) {
          out.print((String)io);
        } else if (io instanceof Location) {
          out.print("Location: \"");
          out.print(io.toString());
          out.print("\"");
        } else if (io instanceof Asset) {
          Asset as = (Asset)io;
          out.print("Asset: \"");
          TypeIdentificationPG tipg;
          String tiNomen;
          if (((tipg = as.getTypeIdentificationPG()) != null) &&
              ((tiNomen = tipg.getNomenclature()) != null)) {
            out.print(tiNomen);
          }
          out.print("(asset type=");
          out.print(as.getClass().getName());
          out.print(", asset uid=");
          UID asu;
          String uid;
          if (((asu = as.getUID()) != null) &&
              ((uid = asu.toString()) != null)) {
            out.print(uid);
          } else {
            out.print("None");
          }
          out.print(")\"");
        } else if (io instanceof Schedule) {
          out.print(io.getClass().getName());
        } else if (io instanceof MessageAddress) {
          out.print("CID: \"");
          out.print(((MessageAddress)io).toString());
          out.print("\"");
        } else if (io instanceof AssetTransfer) {
          out.print("AssetTransfer: \"");
          out.print(((AssetTransfer)io).getAsset().getName());
          out.print("\"");
        } else if (io instanceof AssetAssignment) {
          out.print("AssetAssignment: \"");
          out.print(((AssetAssignment)io).getAsset().getName());
          out.print("\"");
        } else if (io instanceof AssetGroup) {
          out.print("AssetGroup: \"[");
          List assets = ((AssetGroup)io).getAssets();
          for (int i = 0; i < assets.size(); i++) {
            Asset as = (Asset)assets.get(i);
            // recursive!
            printObject(as);
          }
          out.print("]\"");
        } else if (io instanceof AbstractMeasure) {
          String clName = ((AbstractMeasure)io).getClass().getName();
          int i = clName.lastIndexOf('.');
          if (i > 0) {
            clName = clName.substring(i+i);
          }
          out.print(clName);
          out.print(": ");
          out.print(io.toString());
        } else { 
          out.print(io.getClass().getName()); 
          out.print(": ");
          out.print(io.toString());
        }
      } catch (Exception e) {
        out.print("<font color=red>invalid</font>");
      }
    }

    /** END UTILITY PARSERS **/

    /** BEGIN BLACKBOARD SEARCHERS **/

    private UnaryPredicate getUniqueObjectWithUIDPred(
        final String uidFilter) 
    {
      final UID findUID = UID.toUID(uidFilter);
      return new UnaryPredicate() {
        public boolean execute(Object o) {
          if (o instanceof UniqueObject) {
            UID u = ((UniqueObject)o).getUID();
            return 
              findUID.equals(u);
          }
          return false;
        }
      };
    }

    private UnaryPredicate getTaskPred() 
    {
      return new UnaryPredicate() {
        public boolean execute(Object o) {
          return (o instanceof Task);
        }
      };
    }

    private UnaryPredicate getTaskWithVerbPred(final Verb v) 
    {
      return new UnaryPredicate() {
        public boolean execute(Object o) {
          return ((o instanceof Task) &&
              v.equals(((Task)o).getVerb()));
        }
      };
    }

    private UnaryPredicate getPlanElementPred() 
    {
      return new UnaryPredicate() {
        public boolean execute(Object o) {
          return (o instanceof PlanElement);
        }
      };
    }

    private UnaryPredicate getAssetPred() 
    {
      return new UnaryPredicate() {
        public boolean execute(Object o) {
          return (o instanceof Asset);
        }
      };
    }

    private UnaryPredicate getUniqueObjectPred() 
    {
      return new UnaryPredicate() {
        public boolean execute(Object o) {
          return (o instanceof UniqueObject);
        }
      };
    }

    private Collection searchUsingPredicate(
        UnaryPredicate pred) 
    {
      Collection col = support.queryBlackboard(pred);
      if (sortByUID && 
          (col.size() > 1)) {
        Object[] a = col.toArray();
        Arrays.sort(a, THE_ONLY_UID_COMPARATOR);
        return Arrays.asList(a);
      } else {
        return col;
      }
    }

    private static final Comparator THE_ONLY_UID_COMPARATOR = new UIDComparator ();

    private static class UIDComparator implements Comparator {
      public int compare (Object first, Object second) {
	if (first instanceof UniqueObject) {
	  if (second instanceof UniqueObject) {
	    // return the usual UID compare
	    UID u1 = ((UniqueObject) first).getUID();
	    UID u2 = ((UniqueObject) second).getUID();
	    return u1.compareTo(u2);
	  } else {
	    return -1;
	  }
	} else if (second instanceof UniqueObject) {
	  return 1;
	} else {
	  return 0;
	}
      }
    }

    private UniqueObject findUniqueObjectWithUID(
        final String itemUID)
    {
      if (itemUID == null) {
        // missing UID
        return null;
      }
      Collection col = 
        searchUsingPredicate(
            getUniqueObjectWithUIDPred(itemUID));
      if (col.size() < 1) {
        // item not found
        return null;
      }
      // take first match
      Iterator iter = col.iterator();
      UniqueObject uo = (UniqueObject)iter.next();
      if (DEBUG) {
        if (iter.hasNext()) {
          System.err.println("Multiple matches for "+itemUID+"?");
        }
      }
      return uo;
    }

    private Collection findAllTasks()
    {
      return 
        searchUsingPredicate(getTaskPred());
    }

    private Collection findTasksWithVerb(
        final String verbFilter)
    {
      if (verbFilter == null) {
        // missing verb
        return null;
      }
      Verb v = Verb.getVerb(verbFilter);
      return 
        searchUsingPredicate(
            getTaskWithVerbPred(v));
    }

    private Collection findAllPlanElements()
    {
      return 
        searchUsingPredicate(
            getPlanElementPred());
    }

    private Collection findAllAssets()
    {
      return 
        searchUsingPredicate(
            getAssetPred());
    }

    private Collection findAllUniqueObjects()
    {
      return 
        searchUsingPredicate(
            getUniqueObjectPred());
    }

    /** END BLACKBOARD SEARCHERS **/

    /** BEGIN MISC UTILITIES **/

    /**
     * Item type codes to show interface name instead of "*Impl".
     **/
    private static final int ITEM_TYPE_ALLOCATION     = 0;
    private static final int ITEM_TYPE_EXPANSION      = 1;
    private static final int ITEM_TYPE_AGGREGATION    = 2;
    private static final int ITEM_TYPE_DISPOSITION    = 3;
    private static final int ITEM_TYPE_ASSET_TRANSFER = 4;
    private static final int ITEM_TYPE_TASK           = 5;
    private static final int ITEM_TYPE_ASSET          = 6;
    private static final int ITEM_TYPE_WORKFLOW       = 7;
    private static final int ITEM_TYPE_OTHER          = 8;
    private static String[] ITEM_TYPE_NAMES;
    static {
      ITEM_TYPE_NAMES = new String[(ITEM_TYPE_OTHER+1)];
      ITEM_TYPE_NAMES[ITEM_TYPE_ALLOCATION     ] = "Allocation";
      ITEM_TYPE_NAMES[ITEM_TYPE_EXPANSION      ] = "Expansion";
      ITEM_TYPE_NAMES[ITEM_TYPE_AGGREGATION    ] = "Aggregation";
      ITEM_TYPE_NAMES[ITEM_TYPE_DISPOSITION    ] = "Disposition";
      ITEM_TYPE_NAMES[ITEM_TYPE_ASSET_TRANSFER ] = "AssetTransfer";
      ITEM_TYPE_NAMES[ITEM_TYPE_TASK           ] = "Task";
      ITEM_TYPE_NAMES[ITEM_TYPE_ASSET          ] = "Asset";
      ITEM_TYPE_NAMES[ITEM_TYPE_WORKFLOW       ] = "Workflow";
      ITEM_TYPE_NAMES[ITEM_TYPE_OTHER          ] = null;
    }

    /**
     * getItemType.
     * <p>
     * Replace with synchronized hashmap lookup on obj.getClass()?
     **/
    private static int getItemType(Object obj) {
      if (obj instanceof PlanElement) {
        if (obj instanceof Allocation) {
          return ITEM_TYPE_ALLOCATION;
        } else if (obj instanceof Expansion) {
          return ITEM_TYPE_EXPANSION;
        } else if (obj instanceof Aggregation) {
          return ITEM_TYPE_AGGREGATION;
        } else if (obj instanceof Disposition) {
          return ITEM_TYPE_DISPOSITION;
        } else if (obj instanceof AssetTransfer) {
          return ITEM_TYPE_ASSET_TRANSFER;
        } else {
          return ITEM_TYPE_OTHER;
        }
      } else if (obj instanceof Task) {
        return ITEM_TYPE_TASK;
      } else if (obj instanceof Asset) {
        return ITEM_TYPE_ASSET;
      } else if (obj instanceof Workflow) {
        return ITEM_TYPE_WORKFLOW;
      } else {
        return ITEM_TYPE_OTHER;
      }
    }

    /**
     * SummaryInfo.
     * <p>
     * Counter holder
     **/
    private static class SummaryInfo {
      public int counter;
      public SummaryInfo() {
        counter = 0;
      }
      public static final Comparator LARGEST_COUNTER_FIRST_ORDER = 
        new Comparator() {
          public final int compare(Object o1, Object o2) {
            int c1 = ((SummaryInfo)o1).counter;
            int c2 = ((SummaryInfo)o2).counter;
            return ((c1 > c2) ? -1 : ((c1 == c2) ? 0 : 1));
          }
        };
    }

    /**
     * SummaryInfo.
     */
    private static class VerbSummaryInfo extends SummaryInfo {
      public Verb verb;
      public VerbSummaryInfo(Verb vb) {
        super();
        verb = vb;
      }
    }

    /**
     * Dates are formatted to "month_day_year_hour:minute[AM|PM]"
     */
    private static SimpleDateFormat myDateFormat;
    private static Date myDateInstance;
    private static java.text.FieldPosition myFieldPos;
    static {
      myDateFormat = new SimpleDateFormat("MM_dd_yyyy_h:mma");
      myDateInstance = new Date();
      myFieldPos = new java.text.FieldPosition(SimpleDateFormat.YEAR_FIELD);
    }

    /**
     * getTimeString.
     * <p>
     * Formats time to String.
     */
    private static String getTimeString(long time) {
      synchronized (myDateFormat) {
        myDateInstance.setTime(time);
        return 
          myDateFormat.format(
              myDateInstance,
              new StringBuffer(20), 
              myFieldPos
                             ).toString();
      }
    }

    /**
     * bit[] based upon URLEncoder.
     */
    static boolean[] DONT_NEED_ENCODING;
    static {
      DONT_NEED_ENCODING = new boolean[256];
      for (int i = 'a'; i <= 'z'; i++) {
        DONT_NEED_ENCODING[i] = true;
      }
      for (int i = 'A'; i <= 'Z'; i++) {
        DONT_NEED_ENCODING[i] = true;
      }
      for (int i = '0'; i <= '9'; i++) {
        DONT_NEED_ENCODING[i] = true;
      }
      DONT_NEED_ENCODING['-'] = true;
      DONT_NEED_ENCODING['_'] = true;
      DONT_NEED_ENCODING['.'] = true;
      DONT_NEED_ENCODING['*'] = true;

      // special-case to not encode "/"
      DONT_NEED_ENCODING['/'] = true;
    }

    /**
     * Saves some String allocations.
     */
    private static String encodeUID(String s) {
      int n = s.length();
      for (int i = 0; i < n; i++) {
        int c = (int)s.charAt(i);
        if (!(DONT_NEED_ENCODING[i])) {
          try {
            return URLEncoder.encode(s, "UTF-8");
          } catch (Exception e) {
            throw new IllegalArgumentException(
                "Unable to encode URL ("+s+")");
          }
        }
      }
      return s;
    }

    /**
     * XMLtoHTMLOutputStream.
     * <p>
     * Filter which converts XML to simple HTML.
     * Assumes &lt;pre&gt; tag surrounds this call, e.g.
     * <pre><code>
     *   String xml = "&gt;tag&lt;value&gt;/tag&lt;";
     *   PrintStream out = System.out;
     *   XMLtoHTMLOutputStream xout = new XMLtoHTMLOutputStream(out);
     *   out.print("&lt;pre&gt;\n");
     *   xout.print(xml);
     *   xout.flush();
     *   out.print("\n&lt;/pre&gt;");
     * </code></pre>
     * This keeps the spacing uniform and saves some writing.
     */
    public static class XMLtoHTMLOutputStream extends FilterWriter 
    {
      private static final char[] LESS_THAN;
      private static final char[] GREATER_THAN;
      static {
        LESS_THAN = "<font color=green>&lt;".toCharArray();
        GREATER_THAN = "&gt;</font>".toCharArray();
      }

      public XMLtoHTMLOutputStream(Writer w) {
        super(w);
      }

      public void write(String str, int off, int len) throws IOException 
      {
        int n = off+len;
        for (int i = off; i < n; i++) {
          write(str.charAt(i));
        }
      }

      public void write(char cbuf[], int off, int len) throws IOException 
      {
        int n = off+len;
        for (int i = off; i < n; i++) {
          write(cbuf[i]);
        }
      }

      public void write(int c) throws IOException {
        //
        // NOTE: "this.out" is *not* the PlanViewer's "out"!
        //
        if (c == '<') {
          this.out.write(LESS_THAN);
        } else if (c == '>') {
          this.out.write(GREATER_THAN);
        } else {
          this.out.write(c);
        }
      }
    }

    /** END MISC UTILITIES **/
  }
}

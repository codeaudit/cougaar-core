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

package org.cougaar.core.topology;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.Servlet;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.servlet.BaseServletComponent;

/**
 * Servlet that provides access to the TopologyReaderService.
 * <p>
 * The path of the servlet is "/topology".
 * <p>
 * The URL parameters to this servlet are:
 * <ul><p>
 *   <li><tt>method=STRING</tt><br>
 *       Required method for the topology read operation.  
 *       If not specified then a general "usage" HTML 
 *       page is generated.
 *       <p>
 *       The valid values are:
 *       <ul>
 *         <li>all</li>
 *         <li>get</li>
 *         <li>entry</li>
 *         <li>entries</li>
 *       </ul>
 *       </li><p>
 *   <li><tt>type=STRING</tt><br>
 *       Used for method "all" to specify one of these
 *       types:
 *       <ul>
 *         <li>agent</li>
 *         <li>node</li>
 *         <li>host</li>
 *         <li>site</li>
 *         <li>enclave</li>
 *       </ul>
 *       </li><p>
 *   <li><tt>child=STRING</tt><br>
 *       Type for method "get", which must match one of 
 *       the types listed in the "type" parameter above.
 *       </li><p>
 *   <li><tt>parent=STRING</tt><br>
 *       Type for method "get", which must match one of 
 *       the types listed in the "type" parameter above.
 *       </li><p>
 *   <li><tt>for=BOOLEAN</tt><br>
 *       Direction option for "get", which should be
 *       "true" if searching from child to single parent.
 *       </li><p>
 *   <li><tt>name=STRING</tt><br>
 *       Name for "get" and "entry" methods.
 *       </li><p>
 *   <li><tt>argA=STRING</tt><br>
 *       <tt>argN=STRING</tt><br>
 *       <tt>argH=STRING</tt><br>
 *       <tt>argS=STRING</tt><br>
 *       <tt>argE=STRING</tt><br>
 *       Optional parameters for the "entries" method,
 *       where the last letter of the parameter name 
 *       indicates the type.
 *       </li><p>
 *   <li><tt>format=STRING</tt><br>
 *       Format for the response, which defaults to "html".
 *       <p>
 *       The valid values are:
 *       <ul>
 *         <li>html  <i>(html page)</i></li>
 *         <li>csv   <i>(comma-separate text lines)</i></li>
 *         <li>text  <i>(same as "csv")</i></li>
 *         <li>csvdata <i>(similar to "csv", but as a 
 *             serialized Collection of Strings for each 
 *             line)</i>
 *         <li>data  <i>(serialized data with the 
 *             original data types)</i></li>
 *       </ul>
 *       </li><p>
 * </ul>
 */
// poor man's RPC:
public class TopologyReaderServlet
extends BaseServletComponent
{

  // valid types
  public static final String[] VALID_TYPES = 
    new String[] {
      "agent",
      "node",
      "host",
      "site",
      "enclave",
    };

  // int constants to match valid types
  public static final int[] TYPE_CODES = 
    new int[] {
      TopologyReaderService.AGENT,
      TopologyReaderService.NODE,
      TopologyReaderService.HOST,
      TopologyReaderService.SITE,
      TopologyReaderService.ENCLAVE,
    };

  // valid formats
  public static final String[] VALID_FORMATS = 
    new String[] {
      "html",
      "csv",
      "text",
      "csvdata",
      "data",
    };

  // valid args for "entries" method
  public static final String[] ARG_PARAMS = 
    new String[] {
      "argA",
      "argN",
      "argH",
      "argS",
      "argE",
    };

  private MessageAddress agentId;
  private MessageAddress nodeId;

  private AgentIdentificationService agentIdService;
  private NodeIdentificationService nodeIdService;
  private TopologyReaderService topologyReaderService;

  protected String getPath() {
    return "/topology";
  }

  protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
  }

  // aquire services:
  public void load() {

    // get the agentId
    this.agentIdService = (AgentIdentificationService)
      serviceBroker.getService(
          this,
          AgentIdentificationService.class,
          null);
    if (agentIdService == null) {
      throw new RuntimeException(
          "Unable to obtain AgentIdentificationService for \""+
          getPath()+"\" servlet");
    }
    this.agentId = agentIdService.getMessageAddress();
    if (agentId == null) {
      throw new RuntimeException(
          "Unable to obtain agent's id? for \""+
          getPath()+"\" servlet");
    }

    // get the nodeId
    this.nodeIdService = (NodeIdentificationService)
      serviceBroker.getService(
          this,
          NodeIdentificationService.class,
          null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to obtain NodeIdentificationService for \""+
          getPath()+"\" servlet");
    }
    this.nodeId = nodeIdService.getMessageAddress();
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node's id? for \""+
          getPath()+"\" servlet");
    }

    // get the topology reader
    this.topologyReaderService = (TopologyReaderService)
      serviceBroker.getService(
          this,
          TopologyReaderService.class,
          null);
    if (topologyReaderService == null) {
      throw new RuntimeException(
          "Unable to obtain TopologyReaderService for \""+
          getPath()+
          "\" servlet");
    }

    super.load();
  }

  // release services:
  public void unload() {
    super.unload();
    if (topologyReaderService != null) {
      serviceBroker.releaseService(
          this, TopologyReaderService.class, topologyReaderService);
      topologyReaderService = null;
    }
    if (nodeIdService != null) {
      serviceBroker.releaseService(
          this, NodeIdentificationService.class, nodeIdService);
      nodeIdService = null;
    }
    if (agentIdService != null) {
      serviceBroker.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
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

      public static final String FORMAT_PARAM = "format";
      public int format;

      // see TopologyReaderService
      public static final String METHOD_PARAM = "method";
      public String method;

      public static final String TYPE_PARAM = "type";
      public int type;

      public static final String PARENT_TYPE_PARAM = "parent";
      public int parentType;

      public static final String IS_FOR_PARAM = "for";
      public boolean isFor;

      public static final String CHILD_TYPE_PARAM = "child";
      public int childType;

      public static final String NAME_PARAM = "name";
      public String name;

      // see ARG_PARAMS
      public String[] args;

      // timeouts
      public static final String TIMEOUT_PARAM = "timeout";
      public long timeout;

      public static final String USE_STALE_PARAM = "useStale";
      public boolean useStale;

      // add QoS options?  for now we'll assume "get*".

      // worker constructor:
      public MyWorker(
          HttpServletRequest request,
          HttpServletResponse response) {
        this.request = request;
        this.response = response;
      }

      // handle a request:
      public void execute() throws IOException {
        try {
          parseParams();
        } catch (Exception e) {
          writeFailure(e);
          return;
        }
        if (method == null) {
          writeUsage();
          return;
        }
        Object ret;
        try {
          ret = invokeMethod();
        } catch (Exception e) {
          writeFailure(e);
          return;
        }
        writeSuccess(ret);
      }

      private void parseParams() throws IOException {
        // get format
        String sformat = request.getParameter(FORMAT_PARAM);
        if ((sformat == null) || 
            (sformat.length() <= 0)) {
          format = 0; // html
        } else {
          for (int i = 0; ; i++) {
            if (i >= VALID_FORMATS.length) {
              throw new MyIllegalArgumentException(
                  "Invalid format \""+sformat+"\"");
            }
            if (sformat.equals(VALID_FORMATS[i])) {
              format = i;
              break;
            }
          }
        }
        // get method
        method = request.getParameter(METHOD_PARAM);
        // get name
        name = request.getParameter(NAME_PARAM);
        // get is-for
        isFor = "true".equals(request.getParameter(IS_FOR_PARAM));
        // get types
        type = parseType(request.getParameter(TYPE_PARAM));
        childType = parseType(request.getParameter(CHILD_TYPE_PARAM));
        parentType = parseType(request.getParameter(PARENT_TYPE_PARAM));
        // get args
        for (int i = 0; i < ARG_PARAMS.length; i++) {
          String si = request.getParameter(ARG_PARAMS[i]);
          if ((si != null) &&
              (si.length() > 0)) {
            if (args == null) {
              args = new String[ARG_PARAMS.length];
            }
            args[i] = URLDecoder.decode(si, "UTF-8");
          }
        }
        // get timeout
        String stimeout = request.getParameter(TIMEOUT_PARAM);
        timeout = 
          (stimeout != null ?  Long.parseLong(stimeout) : -1);
        String suseStale = request.getParameter(USE_STALE_PARAM);
        useStale = 
          (suseStale != null ? "true".equals(suseStale) : true);
      }
      
      private int parseType(String s) {
        if (s == null) {
          return -1;
        }
        for (int i = 0; i < VALID_TYPES.length; i++) {
          if (s.equals(VALID_TYPES[i])) {
            return TYPE_CODES[i];
          }
        }
        throw new MyIllegalArgumentException(
            "Invalid type \""+s+"\"");
      }

      private Object invokeMethod() {
        // set the timeout preferences on our topology request.
        //
        // we should lock the service, since multiple simultaneous
        // topology servlet requests could overwrite their timeout
        // preferences.  The downside is that a non-timed request
        // would block a simulateous timed request, which defeats
        // the purpose of this timer...
        //
        // this servlet's timeout option is *not documented*
        // and only here for testing.  Let's error on the side
        // of safety and not lock the timeout preference 
        // changes.
        Object ret;
        if (timeout == -1 && useStale) {
          // typical wait-forever behavior
          ret = timedInvokeMethod(topologyReaderService);
        } else {
          // don't lock, simply overwrite the preferences
          topologyReaderService.setTimeout(timeout);
          topologyReaderService.setUseStale(useStale);
          try {
            ret = timedInvokeMethod(topologyReaderService);
          } catch (TopologyReaderService.TimeoutException te) {
            if (te.hasStale()) {
              ret = timedInvokeMethod(te.withStale());
            } else {
              throw new MyIllegalArgumentException(
                  "No stale value available");
            }
          } finally {
            // put things back
            topologyReaderService.setTimeout(-1);
            topologyReaderService.setUseStale(true);
          }
        }
        return ret;
      }

      private Object timedInvokeMethod(TopologyReaderService trs) {
        // could use reflection, but raises security issues
        //
        // here I'll do the equivalent:
        // could use a map for faster lookup.
        //
        if (method == null) {
          throw new MyIllegalArgumentException(
              "Null method name");
        } else if (method.equals("all")) {
          if (type < 0) {
            throw new MyIllegalArgumentException(
                "Must specify a \""+TYPE_PARAM+
                "\" parameter");
          }
          return trs.getAll(type);
        } else if (method.equals("get")) {
          if (parentType < 0) {
            throw new MyIllegalArgumentException(
                "Must specify a \""+PARENT_TYPE_PARAM+
                "\" parameter");
          }
          if (childType < 0) {
            throw new MyIllegalArgumentException(
                "Must specify a \""+CHILD_TYPE_PARAM+
                "\" parameter");
          }
          if (name == null) {
            throw new MyIllegalArgumentException(
                "Must specify a \""+NAME_PARAM+
                "\" parameter");
          }
          if (childType >= parentType) {
            throw new MyIllegalArgumentException(
                "Child \""+
                VALID_TYPES[childType]+
                "\" is not contained by parent \""+
                VALID_TYPES[parentType]+
                "\"");
          }
          if (isFor) {
            return 
              trs.getParentForChild(
                  parentType,
                  childType,
                  name);
          } else {
            return
              trs.getChildrenOnParent(
                  childType,
                  parentType,
                  name);
          }
        } else if (method.equals("entry")) {
          if (name == null) {
            throw new MyIllegalArgumentException(
                "Must specify a \""+NAME_PARAM+
                "\" parameter");
          }
          return trs.getEntryForAgent(
              name);
        } else if (method.equals("entries")) {
          if (args != null) {
            return trs.getAllEntries(
                args[0], args[1], args[2], args[3], args[4]);
          } else {
            return trs.getAllEntries(
                null, null, null, null, null);
          }
        } else {
          throw new MyIllegalArgumentException(
              "Unknown method name: \""+method+"\"");
        }
      }

      private void writeUsage() throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.print(
            "<html><head><title>"+
            "Topology reader"+
            "</title></head>"+
            "<body>\n"+
            "<h2>Topology Reader Servlet</h2>\n"+
            "Please select one of the options below:\n");
        writeForms(out);
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

        // build up response
        out.print(
            "<html><head><title>"+
            "Topology \""+method+
            "\" failed"+
            "</title></head>"+
            "<body>\n"+
            "<center><h1>"+
            "Topology \""+
            method+
            "\" failed"+
            "</h1></center>"+
            "<p>\n");
        if (e instanceof MyIllegalArgumentException) {
          out.print("<b>");
          out.print(e.getMessage());
          out.print("</b>");
        } else {
          out.print("<pre>\n");
          e.printStackTrace(out);
          out.print("\n</pre>\n");
        }
        out.print(
            "<p>"+
            "Please double-check these parameters:\n");
        writeForms(out);
        out.print(
            "</body></html>\n");
        out.close();
      }

      private void writeSuccess(Object ret) throws IOException {
        switch (format) {
          default:
            // fall-through:
          case 0: 
            // html or default
            writeHtmlSuccess(ret);
            break;
          case 1:
            // fall-through:
          case 2: 
            // text or csv
            writeTextSuccess(ret); 
            break;
          case 3:
            // csv data
            writeCsvDataSuccess(ret);
            break;
          case 4:
            // data
            writeDataSuccess(ret);
            break;
        }
      }

      private void writeHtmlSuccess(Object ret) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.print(
            "<html><head><title>"+
            "Topology Response" +
            "</title></head>"+
            "<body>\n"+
            "<center><h1>Topology Response"+
            "</h1></center><p>\n");
        if (ret == null) {
          out.print("<b>Null response.</b>");
        } else if (ret instanceof String) {
          out.print("<b>Response: "+ret+"</b>");
        } else if (ret instanceof TopologyEntry) {
          startHtmlTable(out);
          writeHtmlEntry(out, (TopologyEntry) ret);
          endHtmlTable(out);
        } else if (ret instanceof Set) {
          Set s = (Set) ret;
          int n = s.size();
          if (n <= 0) {
            out.print("<b>Empty response set.</b>");
          } else {
            Iterator iter = s.iterator();
            Object x = iter.next();
            if (x instanceof String) {
              out.print("String["+n+"]:<pre>");
              while (true) {
                out.print("\n  ");
                out.print(x);
                if (--n <= 0) break;
                x = iter.next();
              }
              out.print("</pre><p>");
            } else if (x instanceof TopologyEntry) {
              out.print("TopologyEntry["+n+"]:<p>");
              startHtmlTable(out);
              while (true) {
                TopologyEntry te = (TopologyEntry) x;
                writeHtmlEntry(out, te);
                if (--n <= 0) break;
                x = iter.next();
              }
              endHtmlTable(out);
            } else {
              out.print(
                  "<b>Invalid response: "+
                  ((x != null) ? x.getClass().getName() : "null")+
                  "</b>");
            }
          }
        } else {
          out.print(
              "<b>Invalid response: "+
              ret.getClass().getName()+
              "</b>");
        }
        writeForms(out);
        out.print("</body></html>\n");
        out.close();
      }

      private void writeTextSuccess(Object ret) throws IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        if (ret == null) {
          out.print("");
        } else if (ret instanceof String) {
          out.print(ret);
          out.print("\n");
        } else if (ret instanceof TopologyEntry) {
          writeTextEntry(out, (TopologyEntry) ret);
        } else if (ret instanceof Set) {
          Set s = (Set) ret;
          int n = s.size();
          if (n <= 0) {
            out.print("");
          } else {
            Iterator iter = s.iterator();
            Object x = iter.next();
            if (x instanceof String) {
              while (true) {
                out.print(x);
                out.print("\n");
                if (--n <= 0) break;
                x = iter.next();
              }
            } else if (x instanceof TopologyEntry) {
              while (true) {
                TopologyEntry te = (TopologyEntry) x;
                writeTextEntry(out, te);
                if (--n <= 0) break;
                x = iter.next();
              }
            } else {
              out.print("");
            }
          }
        } else {
          out.print("");
        }
        out.close();
      }

      private void writeCsvDataSuccess(Object ret) throws IOException {
        Set retSet;
        if (ret == null) {
          retSet = Collections.EMPTY_SET;
        } else if (ret instanceof String) {
          retSet = Collections.singleton(ret);
        } else if (ret instanceof TopologyEntry) {
          String s = createCsvDataEntry((TopologyEntry) ret);
          retSet = Collections.singleton(s);
        } else if (ret instanceof Set) {
          Set s = (Set) ret;
          int n = s.size();
          if (n <= 0) {
            retSet = Collections.EMPTY_SET;
          } else {
            Iterator iter = s.iterator();
            Object x = iter.next();
            if (x instanceof String) {
              retSet = s;
            } else if (x instanceof TopologyEntry) {
              retSet = new HashSet(n);
              while (true) {
                TopologyEntry te = (TopologyEntry) x;
                String si = createCsvDataEntry(te);
                retSet.add(si);
                if (--n <= 0) break;
                x = iter.next();
              }
            } else {
              retSet = Collections.EMPTY_SET;
            }
          }
        } else {
          retSet = Collections.EMPTY_SET;
        }
        writeDataSuccess(retSet);
      }

      private void writeDataSuccess(Object ret) throws IOException {
        OutputStream os = response.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(ret);
        oos.close();
      }

      private void startHtmlTable(PrintWriter out) {
        out.print(
            "<table align=center border=1 cellpadding=1\n"+
            " cellspacing=1 width=75%\n"+
            " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
            "<tr>\n"+
            "<td colspan=9>"+
            "<font size=+1 color=mediumblue><b>Topology Entries</b></font>"+
            "</td>\n"+
            "</tr>\n"+
            "<tr>\n"+
            "<td><font color=mediumblue><b>Agent</b></font></td>\n"+
            "<td><font color=mediumblue><b>Node</b></font></td>\n"+
            "<td><font color=mediumblue><b>Host</b></font></td>\n"+
            "<td><font color=mediumblue><b>Site</b></font></td>\n"+
            "<td><font color=mediumblue><b>Enclave</b></font></td>\n"+
            "<td><font color=mediumblue><b>Incarnation</b></font></td>\n"+
            "<td><font color=mediumblue><b>MoveId</b></font></td>\n"+
            "<td><font color=mediumblue><b>Type</b></font></td>\n"+
            "<td><font color=mediumblue><b>Status</b></font></td>\n"+
            "</tr>\n");
      }

      private void writeHtmlEntry(PrintWriter out, TopologyEntry te) {
        out.print("<tr><td>");
        out.print(te.getAgent());
        out.print("</td><td>");
        out.print(te.getNode());
        out.print("</td><td>");
        out.print(te.getHost());
        out.print("</td><td>");
        out.print(te.getSite());
        out.print("</td><td>");
        out.print(te.getEnclave());
        out.print("</td><td>");
        out.print(te.getIncarnation());
        out.print("</td><td>");
        out.print(te.getMoveId());
        out.print("</td><td>");
        out.print(te.getTypeAsString());
        out.print("</td><td>");
        out.print(te.getStatusAsString());
        out.print("</td></tr>\n");
      }

      private void endHtmlTable(PrintWriter out) {
        out.print("</table>\n");
      }

      private void writeTextEntry(PrintWriter out, TopologyEntry te) {
        out.print(te.getAgent());
        out.print(", ");
        out.print(te.getNode());
        out.print(", ");
        out.print(te.getHost());
        out.print(", ");
        out.print(te.getSite());
        out.print(", ");
        out.print(te.getEnclave());
        out.print(", ");
        out.print(te.getIncarnation());
        out.print(", ");
        out.print(te.getMoveId());
        out.print(", ");
        out.print(te.getTypeAsString());
        out.print(", ");
        out.print(te.getStatusAsString());
        out.print("\n");
      }

      private String createCsvDataEntry(TopologyEntry te) {
        return
          te.getAgent()+
          ", "+
          te.getNode()+
          ", "+
          te.getHost()+
          ", "+
          te.getSite()+
          ", "+
          te.getEnclave()+
          ", "+
          te.getIncarnation()+
          ", "+
          te.getMoveId()+
          ", "+
          te.getTypeAsString()+
          ", "+
          te.getStatusAsString()+
          "\n";
      }

      private void writeForms(
          PrintWriter out) throws IOException {
        out.print("<p><hr><p>");
        writeEntriesForm(out);
        writeAllForm(out);
        writeRelationForm(out, true);
        writeRelationForm(out, false);
        writeEntryForm(out);
      }

      private void writeAllForm(
          PrintWriter out) throws IOException {
        out.print(
            "<form method=\"GET\" action=\"");
        out.print(request.getRequestURI());
        out.print(
            "\">\n"+
            "<input type=\"hidden\" name=\""+
            METHOD_PARAM+
            "\" value=\"all\">"+
            "List all "+
            "<select name=\""+
            TYPE_PARAM+
            "\">");
        writeTypeOptions(out, type, true);
        out.print("</select>\n");
        out.print(
            "<input type=\"submit\" value=\"Submit\">"+
            "</form>\n");
      }

      private void writeRelationForm(
          PrintWriter out, boolean asFor) throws IOException {
        out.print(
            "<form method=\"GET\" action=\"");
        out.print(request.getRequestURI());
        out.print(
            "\">\n"+
            "<input type=\"hidden\" name=\""+
            METHOD_PARAM+
            "\" value=\"get\">");
        out.print(asFor ? "Find" : "List all");
        out.print(
            " <select name=\"");
        out.print(asFor ? PARENT_TYPE_PARAM : CHILD_TYPE_PARAM);
        out.print("\">");
        writeTypeOptions(
            out, 
            (asFor ? parentType : childType),
            (!(asFor)));
        out.print(
            "</select>\n"+
            "<input type=\"hidden\" name=\""+
            IS_FOR_PARAM+
            "\" value=\"");
        out.print(asFor);
        out.print("\">");
        out.print(
            (asFor ? " for " : " on "));
        out.print("<select name=\"");
        out.print(asFor ? CHILD_TYPE_PARAM : PARENT_TYPE_PARAM);
        out.print("\">");
        writeTypeOptions(
            out, 
            (asFor ? childType : parentType),
            false);
        out.print(
            "</select>\n"+
            "<input type=\"text\" name=\""+
            NAME_PARAM+
            "\" size=20>"+
            "<input type=\"submit\" value=\"Submit\">"+
            "</form>\n");
      }

      private void writeEntryForm(
          PrintWriter out) throws IOException {
        // all method:
        out.print(
            "<form method=\"GET\" action=\"");
        out.print(request.getRequestURI());
        out.print(
            "\">\n"+
            "<input type=\"hidden\" name=\""+
            METHOD_PARAM+
            "\" value=\"entry\">"+
            "Find the entry for agent "+
            "<input type=\"text\" name=\""+
            NAME_PARAM+
            "\">"+
            "<input type=\"submit\" value=\"Submit\">"+
            "</form>\n");
      }

      private void writeEntriesForm(
          PrintWriter out) throws IOException {
        // all method:
        out.print(
            "<table>"+
            "<form method=\"GET\" action=\"");
        out.print(request.getRequestURI());
        out.print(
            "\">\n"+
            "<input type=\"hidden\" name=\""+
            METHOD_PARAM+
            "\" value=\"entries\">"+
            "<tr><td colspan=");
        out.print(VALID_TYPES.length);
        out.print(
            ">"+
            "List all agent entries that match these optional parameters,"+
            "where the default is to list <i><b>all</b></i> agent entries:<p>\n"+
            "</td></tr>"+
            "<tr>");
        for (int i = 0; i < VALID_TYPES.length; i++) {
          out.print("<td><font color=mediumblue>");
          out.print(VALID_TYPES[i]);
          out.print(":</font></td>\n");
        }
        out.print("</tr><tr>");
        for (int i = 0; i < ARG_PARAMS.length; i++) {
          out.print("<td><input type=\"text\" name=\"");
          out.print(ARG_PARAMS[i]);
          if ((args != null) && (args[i] != null)) {
            out.print("\" value=\"");
            out.print(args[i]);
          }
          out.print("\"></td>\n");
        }
        out.print(
            "</tr><br>"+
            "<tr><td colspan=");
        out.print(VALID_TYPES.length);
        out.print(
            ">"+
            "<input type=\"submit\" value=\"Submit\">"+
            "</td></tr>"+
            "</form></table>\n");
      }


      private void writeTypeOptions(
          PrintWriter out, int matchType, boolean plural) {
        for (int i = 0; i < VALID_TYPES.length; i++) {
          String vi = VALID_TYPES[i];
          out.print(
              "<option value=\"");
          out.print(vi);
          out.print("\"");
          if (TYPE_CODES[i] == matchType) {
            out.print(" selected");
          }
          out.print(">");
          out.print(vi);
          if (plural) {
            out.print("s");
          }
          out.print("</option>");
        }
      }
    }
  }

  private static class MyIllegalArgumentException 
    extends IllegalArgumentException {
      public MyIllegalArgumentException(String s) {
        super(s);
      }
    }
      

}

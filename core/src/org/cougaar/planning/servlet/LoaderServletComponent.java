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
 */

package org.cougaar.planning.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.cougaar.core.agent.AgentIdentificationService;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.node.ComponentMessage;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.node.NodeIdentifier;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.util.StringUtility;

/**
 * Servlet that allows the client to add Components into the
 * servlet's agent (eg plugin) or node (eg agent).
 * <p>
 * The path of the servlet is "/load".
 * <p>
 * The URL parameters are:
 * <ul><p>
 *   <li><tt>into=STRING</tt><br>
 *       Optional name of where to add this component;
 *       this should be either "agent" or "node", where the
 *       default is "agent".  This must agree with the
 *       "insertionPoint".</li><p>
 *   <li><tt>insertionPoint=STRING</tt><br>
 *       Insertion point for the component; the default is:<br>
 *       <tt>Node.AgentManager.Agent.PluginManager.Plugin</tt>.
 *       </li><p>
 *   <li><tt>classname=STRING</tt><br>
 *       Name of the component class to load.<br>
 *       If this parameter is missing then a "usage" HTML
 *       page is generated.</li><p>
 *   <li><tt>parameters=STRING1,STRING2,..,STRINGN</tt> 
 *       Optional list of string parameters to pass to the
 *       component.  Defaults to null.</li><p>
 *   <li><tt>codebase=STRING</tt><br>
 *       Optional codebase URL for locating the class file(s).
 *       The default is to use the node's classpath.</li><p>
 * </ul>
 * <p>
 * Note the <b>SECURITY</b> issues in loading an arbitrary
 * Component!
 */
public class LoaderServletComponent
extends BaseServletComponent
{
  protected ClusterIdentifier agentId;
  protected NodeIdentifier nodeId;

  protected MessageTransportService mts;
  protected NodeIdentificationService nodeIdService;

  protected String getPath() {
    return "/load";
  }

  protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
  }

  // aquire services:
  public void load() {
    // FIXME need AgentIdentificationService
    org.cougaar.core.plugin.PluginBindingSite pbs =
      (org.cougaar.core.plugin.PluginBindingSite) bindingSite;
    this.agentId = pbs.getAgentIdentifier();

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
    this.nodeId = nodeIdService.getNodeIdentifier();
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node's id? for \""+
          getPath()+"\" servlet");
    }

    // create a dummy message transport client
    MessageTransportClient mtc =
      new MessageTransportClient() {
        public void receiveMessage(Message message) {
          // never
        }
        public MessageAddress getMessageAddress() {
          return agentId;
        }
      };

    // get the message transport
    this.mts = (MessageTransportService)
      serviceBroker.getService(
          mtc,
          MessageTransportService.class,
          null);
    if (mts == null) {
      throw new RuntimeException(
          "Unable to obtain MessageTransportService for \""+
          getPath()+
          "\" servlet");
    }

    super.load();
  }

  // release services:
  public void unload() {
    super.unload();
    if (mts != null) {
      serviceBroker.releaseService(
          this, MessageTransportService.class, mts);
      mts = null;
    }
    if (nodeIdService != null) {
      serviceBroker.releaseService(
          this, NodeIdentificationService.class, nodeIdService);
      nodeIdService = null;
    }
    // release agentIdService
  }

  // helper method:
  private void loadComponent(
      String addTarget,
      ComponentDescription desc) {
    // select destination
    MessageAddress destAddr;
    if ("node".equalsIgnoreCase(addTarget)) {
      destAddr = nodeId;
    } else {
      destAddr = agentId;
    }
    // create an ADD ComponentMessage
    ComponentMessage addMsg =
      new ComponentMessage(
          agentId,  // from
          destAddr, // to
          ComponentMessage.ADD,
          desc,
          null);    // state
    // send message
    mts.sendMessage(addMsg);
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

      public static final String ADD_TARGET_PARAM = "into";
      public String addTarget;

      public static final String INSERTION_POINT_PARAM = "insertionPoint";
      public String insertionPoint;

      public static final String CLASSNAME_PARAM = "classname";
      public String classname;

      public static final String PARAMETERS_PARAM = "params";
      public List parameters;

      public static final String CODEBASE_PARAM = "codebase";
      public String codebase;

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
        // set defaults
        addTarget = "agent";
        insertionPoint =
          "Node.AgentManager.Agent.PluginManager.Plugin";
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
          value = URLDecoder.decode(value);

          // save parameters
          if (name.equals(ADD_TARGET_PARAM)) {
            addTarget = value;
          } else if (name.equals(INSERTION_POINT_PARAM)) {
            insertionPoint = value;
          } else if (name.equals(CLASSNAME_PARAM)) {
            classname = value;
          } else if (name.equals(PARAMETERS_PARAM)) {
            // parse (s1, s2, .., sN)
            parameters = StringUtility.parseCSV(value);
          } else if (name.equals(CODEBASE_PARAM)) {
            codebase = value;
          } else {
          }
        }
      }

      private void writeResponse() throws IOException {
        if (classname != null) {
          ComponentDescription desc =
            createComponentDescription();
          try {
            // add security-check here
            loadComponent(addTarget, desc);
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
        out.print(
            "<html><head><title>"+
            "Component loader"+
            "</title></head>"+
            "<body>\n"+
            "<h2>Component Loader Servlet</h2>\n"+
            "Please fill in these parameters:\n");
        writeParameters(out);
        out.print(
            "</body></html>\n");
        out.close();
      }

      private void writeFailure(Exception e) throws IOException {
        response.setContentType("text/html");
        // send error code
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(baos);
        out.print(
            "<html><head><title>"+
            "Unable to load component"+
            "</title></head>"+
            "<body>\n"+
            "<h2>Unable to send \"AddComponent\" message:</h2>"+
            "<p><pre>\n");
        e.printStackTrace(out);
        out.print(
            "\n</pre><p>"+
            "Please double-check these parameters:\n");
        writeParameters(out);
        out.print(
            "</body></html>\n");
        out.close();
        response.sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            new String(baos.toByteArray()));
      }

      private void writeSuccess() throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.print(
            "<html><head><title>"+
            "Component Loaded"+
            "</title></head>"+
            "<body>\n"+
            "<h2>Sent \"AddComponent\" message:</h2>\n");
        writeParameters(out);
        out.print(
            "Check standard-error to verify that the"+
            " component was loaded...\n"+
            "</body></html>\n");
        out.close();
      }

      private void writeParameters(
          PrintWriter out) throws IOException {
        out.print(
            "<form method=\"GET\" action=\"");
        out.print(request.getRequestURI());
        out.print(
            "\">\n"+
            "<table>\n"+
            "<tr><td>"+
            "Insert into"+
            "</td><td>\n"+
            "<select name=\""+
            ADD_TARGET_PARAM+
            "\">"+
            "<option value=\"agent\"");
        if ("agent".equalsIgnoreCase(addTarget)) {
          out.print(" selected");
        }
        out.print(
            ">agent ");
        out.print(agentId);
        out.print(
            "</option>"+
            "<option value=\"node\"");
        if ("node".equalsIgnoreCase(addTarget)) {
          out.print(" selected");
        }
        out.print(
            ">node ");
        out.print(nodeId);
        out.print(
            "</option>"+
            "</select>\n"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Insertion point"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            INSERTION_POINT_PARAM+
            "\" size=70");
        if (insertionPoint != null) {
          out.print(" value=\"");
          out.print(insertionPoint);
          out.print("\"");
        }
        out.print(
            "> <i>(required)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Classname"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            CLASSNAME_PARAM+
            "\" size=70");
        if (classname != null) {
          out.print(" value=\"");
          out.print(classname);
          out.print("\"");
        }
        out.print(
            "> <i>(required)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Parameters"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            PARAMETERS_PARAM+
            "\" size=70");
        if (parameters != null) {
          out.print(" value=\"");
          int n = (parameters.size() - 1);
          for (int i = 0; i <= n; i++) {
            out.print(parameters.get(i));
            if (i < n) {
              out.print(", ");
            }
          }
          out.print("\"");
        }
        out.print(
            "> <i>(optional comma-separated list)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Codebase URL"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            CODEBASE_PARAM+
            "\"");
        if (codebase != null) {
          out.print(" value=\"");
          out.print(codebase);
          out.print("\"");
        }
        out.print(
            " size=70>  <i>(optional; see bug 1029)</i>"+
            "</td></tr>\n"+
            "<tr><td colwidth=2>"+
            "<input type=\"submit\" value=\"Add Component\">"+
            "</td></tr>\n"+
            "</table>\n"+
            "</form>\n");
      }

      private ComponentDescription createComponentDescription() {
        // convert codebase to url
        URL codebaseURL;
        if (codebase != null) {
          try {
            codebaseURL = new URL(codebase);
          } catch (MalformedURLException badUrlE) {
            throw new IllegalArgumentException(
                "Illegal codebase URL: "+badUrlE);
          }
        } else {
          codebaseURL = null;
        }

        // create a new ComponentDescription
        ComponentDescription desc =
          new ComponentDescription(
              classname, // name
              insertionPoint,
              classname,
              codebaseURL,
              parameters,
              null,  // certificate
              null,  // lease
              null); // policy
        return desc;
      }
    }
  }
}

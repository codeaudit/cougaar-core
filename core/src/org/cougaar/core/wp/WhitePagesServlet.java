/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.*;
import javax.servlet.http.*;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.*;
import org.cougaar.core.service.wp.*;
import org.cougaar.core.servlet.*;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * An option servlet for viewing and altering the white pages.
 * <p>
 * Load into any agent:<pre>
 *   plugin = org.cougaar.core.wp.WhitePagesServlet(/wp)
 * </pre>
 * <p>
 * FIXME: document URL parameters.
 * <p>
 * Maybe repackage this to a new home...
 */
public class WhitePagesServlet extends ComponentServlet {

  // default "getAll" depth-first recursion limit
  private static final int DEFAULT_LIMIT = 3;

  private LoggingService log;
  private WhitePagesService wps;

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  public void doGet(
      HttpServletRequest sreq,
      HttpServletResponse sres) throws IOException {
    // create a new handler per request, so we don't mangle our
    // per-request variables
    MyHandler h = new MyHandler(getEncodedAgentName(), log, wps);
    h.execute(sreq, sres);  
  }

  private static class MyHandler {

    private final String localAgent;
    private final LoggingService log;
    private final WhitePagesService wps;

    private HttpServletRequest sreq;
    private PrintWriter out;

    private String action;
    private String s_timeout;
    private long timeout;
    private boolean async;
    private String s_limit;
    private String name;
    private String scheme;
    private String addr;
    private String s_app;
    private Application app;
    private boolean rel_ttl;
    private String s_ttl;
    private long now;

    public MyHandler(
        String localAgent,
        LoggingService log,
        WhitePagesService wps) {
      this.localAgent = localAgent;
      this.log = log;
      this.wps = wps;
    }

    public void execute(
        HttpServletRequest sreq, 
        HttpServletResponse sres) throws IOException
      //, ServletException 
    {
      this.sreq = sreq;
      this.out = sres.getWriter();
      parseParams();
      printHeader();
      printForm();
      performRequest();
      printFooter();
    }

    private void parseParams() {
      action = param("action");
      s_timeout = param("timeout");
      if (s_timeout != null) {
        timeout = Long.parseLong(s_timeout);
      }
      async = "true".equals(param("async"));
      s_limit = param("limit");
      name = param("name");
      scheme = param("scheme");
      addr = param("addr");
      s_app = param("app");
      if (s_app != null) {
        app = Application.getApplication(s_app);
      }
      rel_ttl = (!"false".equals(param("rel_ttl")));
      s_ttl = param("ttl");
      now = System.currentTimeMillis();
    }

    private void printHeader() {
      out.println(
          "<html>\n"+
          "<head>\n"+
          "<title>Cougaar White Pages</title>\n"+
          "<script language=\"JavaScript\">\n"+
          "<!--\n"+
          "function selectOp() {\n"+
          "  var i = document.f.action.selectedIndex;\n"+
          "  var s = document.f.action.options[i].text;\n"+
          "  var noLimit = (s != \"getAll\");\n"+
          "  var noApp  = (s == \"get\" || s == \"list\");\n"+
          "  var noSch  = noApp;\n"+
          "  var noAddr = (s != \"bind\" && s != \"rebind\");\n"+
          "  var noCert = noAddr;\n"+
          "  var noRTTL = noCert;\n"+
          "  var noTTL  = noRTTL;\n"+
          "  disable(noLimit, document.f.limit)\n"+
          "  disable(noApp,   document.f.app)\n"+
          "  disable(noSch,   document.f.scheme)\n"+
          "  disable(noAddr,  document.f.addr)\n"+
          "  disable(noCert,  document.f.cert)\n"+
          "  disable(noRTTL,  document.f.rel_ttl)\n"+
          "  disable(noTTL,   document.f.ttl)\n"+
          "}\n"+
        "function disable(b, txt) {\n"+
        "  txt.disabled=b;\n"+
        "  txt.style.background=(b?\"grey\":\"white\");\n"+
        "}\n"+
        "// -->\n"+
        "</script>\n"+
        "</head>\n"+
        "<body onLoad=selectOp()>"+
        "<h2>Cougaar White Pages Servlet</h2>"+
        "<p>\n"+
        "Agent: "+localAgent+
        "<p>\n"+
        "Time:  "+now+" ("+(new Date(now))+")"+
        "<p>");
    }

    private void printForm() {
      out.println(
        "<table border=1>\n"+
        "<form name=\"f\" method=\"GET\" action=\""+
        sreq.getRequestURI()+
        "\">\n"+
        "<tr><th colspan=2>Request</th></tr>\n"+
        "<tr><td>Mode</td><td>"+
        "<select name=\"async\">"+
        "<option value=\"false\""+
        (async ? "" : " selected") +
        ">Wait for response</option>\n"+
        "<option value=\"true\""+
        (async ? " selected" : "") +
        ">Don't wait (async)</option>\n"+
        "</select>\n"+
        "</td></tr>\n"+
        "<tr><td>Action</td><td>"+
        "<select name=\"action\" onChange=\"selectOp()\">\n"+
        option("get", action)+
        option("getAll", action)+
        option("list", action)+
        option("refresh", action)+
        option("bind", action)+
        option("rebind", action)+
        option("unbind", action)+
        "</select>\n"+
        "</td><tr>\n"+
        "<tr><td>Timeout</td><td>"+
        input("timeout", s_timeout)+
        "</td></tr>\n"+
        "<tr><td>Recursion limit</td><td>\n"+
        input("limit", s_limit, 4)+
        "&nbsp;<i>(&lt; 0 is none, default is "+
        DEFAULT_LIMIT+
        ")</i>"+
        "</td><tr>\n"+
        "<tr><td>Entry</td><td>\n"+
        "<table border=1>\n"+
        "<tr><td>Name</td><td>"+
        input("name", name)+
        "</td></tr>\n<tr><td>App</td><td>"+
        input("app", s_app)+
        "</td></tr>\n<tr><td>URI</td><td>"+
        input("scheme", scheme, 7)+
        "://"+
        input("addr", addr, 30)+
        "</td></tr>\n<tr><td>Cert</td><td>"+
        "<select name=\"cert\">\n"+
        "<option name=\"null\" selected>null</option>\n"+
        "</select>\n"+
        "</td></tr>\n<tr><td>TTL</td><td>"+
        "<select name=\"rel_ttl\">"+
        "<option value=\"true\""+
        (rel_ttl ? " selected" : "") +
        ">submit_time+</option>\n"+
        "<option value=\"false\""+
        (rel_ttl ? "" : " selected") +
        ">exact millis</option>\n"+
        "</select>\n"+
        input("ttl", s_ttl, 26)+
        "</td></tr>\n</table>\n"+
        "</td></tr>\n"+
        "<tr><td>"+
        "<input type=\"submit\" value=\"Submit\">\n"+
        "</td><td>"+
        "<input type=\"reset\" value=\"Reset\">\n"+
        "</td></tr>\n"+
        "</form>\n"+
        "</table>");
    }

    private void performRequest() {
      try {
        Request req = null;
        if ("get".equals(action)) {
          if (name == null) {
            out.println(
                "<font color=\"red\">Please specify a name</font>");
          } else {
            req = new Request.Get(name, timeout);
          }
        } else if ("getAll".equals(action)) {
          String suffix = (name == null ? "." : name);
          int limit = DEFAULT_LIMIT;
          if (s_limit != null) {
            limit = Integer.parseInt(s_limit);
          }
          submitGetAll(suffix, limit);
        } else if ("list".equals(action)) {
          String tmp = (name == null ? "" : name);
          req = new Request.List(tmp, timeout);
        } else if ("refresh".equals(action)) {
          AddressEntry ae = parseEntry();
          if (ae != null) {
            req = new Request.Refresh(ae, timeout);
          }
        } else if ("bind".equals(action)) {
          AddressEntry ae = parseEntry();
          if (ae != null) {
            req = new Request.Bind(ae, timeout);
          }
        } else if ("rebind".equals(action)) {
          AddressEntry ae = parseEntry();
          if (ae != null) {
            req = new Request.Rebind(ae, timeout);
          }
        } else if ("unbind".equals(action)) {
          AddressEntry ae = parseEntry();
          if (ae != null) {
            req = new Request.Unbind(ae, timeout);
          }
        } else if (action != null) {
          out.println(
              "<font color=\"red\">Unknown action: "+action+"</font>");
        }
        if (req != null) {
          submit(req);
        }
      } catch (Exception e) {
        e.printStackTrace(out);
      }
    }
    
    private void printFooter() {
      out.println("</body></html>");
    }

    private String param(String n) {
      String s = sreq.getParameter(n);
      if (s==null || s.length()==0) {
        s = null;
      }
      return s;
    }

    private static String option(String n, String v) {
      return 
        "<option value=\""+n+"\""+
        ((v!=null && v.equals(n)) ? " selected" : "")+
        ">"+n+"</option>";
    }

    private static String input(String n, String v) {
      return input(n, v, 40);
    }

    private static String input(String n, String v, int size) {
      return
        "<input type=\"text\" size="+size+" name=\""+n+"\""+
        (v==null?"":" value=\""+v+"\"")+">";
    }

    private AddressEntry parseEntry() throws Exception {
      String x_addr = addr;
      String x_ttl = s_ttl;
      if ("refresh".equals(action) || "unbind".equals(action)) {
        x_addr = "ignored";
        x_ttl = "0";
      }
      if (name==null || s_app==null || x_addr==null || x_ttl==null) {
        out.println("<font color=\"red\">Missing required field</font>");
        return null;
      }
      URI uri = new URI(scheme+"://"+x_addr);
      Cert cert = Cert.NULL;
      long ttl = Long.parseLong(x_ttl);
      if (rel_ttl) {
        ttl += now;
      }
      AddressEntry ae = new AddressEntry(
          name, app, uri, cert, ttl);
      return ae;
    }

    private void submitGetAll(
        String suffix, int limit) throws Exception {
      out.println("<p><hr><p>");
      if (wps == null) {
        out.println(
            "<font color=\"red\">No WhitePagesService?</font>");
        return;
      }
      printTableStart();
      recurseGetAll(suffix, 0, limit);
      printTableEnd();
    }

    // recursive!
    private int recurseGetAll(
        String suffix, int idx, int limit) throws Exception {
      int newIdx = idx;
      if (limit == 0) {
        ++newIdx;
        out.println(
            "<tr>"+
            "<td align=right>"+newIdx+"&nbsp;</td>"+
            "<td colspan=4><a href=\""+
            sreq.getRequestURI()+
            "?action=getAll"+
            "&name="+suffix+
            (s_app == null ? "" : ("&app="+s_app))+
            (scheme == null ? "" : ("&scheme="+scheme))+
            (s_timeout == null ? "" : ("&timeout="+s_timeout))+
            (s_limit == null ? "" : ("&limit="+s_limit))+
            "\">"+suffix+"</a></td>"+
            "</tr>");
        return newIdx;
      }
      Set names = wps.list(suffix, timeout);
      int n = (names == null ? 0 : names.size());
      if (n <= 0) {
        ++newIdx;
        out.println(
            "<tr>"+
            "<td align=right>"+newIdx+"&nbsp;</td>"+
            "<td colspan=4>None listed for \""+suffix+"\"</td>"+
            "</tr>");
        return newIdx;
      }
      List l = new ArrayList(names);
      Collections.sort(l);
      for (int i = 0; i < n; i++) {
        String s = (String) l.get(i);
        if (s == null) {
        } else if (s.length() > 0 && s.charAt(0) == '.') {
          newIdx = recurseGetAll(s, newIdx, (limit - 1));
        } else {
          AddressEntry[] a = wps.get(s, timeout);
          newIdx = print(a, newIdx, app, scheme);
        }
      }
      return newIdx;
    }

    private void submit(Request req) {
      out.println("<p><hr><p>");
      if (wps == null) {
        out.println(
            "<font color=\"red\">No WhitePagesService?</font>");
        return;
      }
      Response res = wps.submit(req);
      if (async) {
        Callback c = new Callback() {
          public void execute(Response res) {
            if (log != null && log.isInfoEnabled()) {
              log.info(localAgent+": "+res);
            }
          }
        };
        res.addCallback(c);
        out.println("Submitted asynchronous request");
        return;
      }
      try {
        res.waitForIsAvailable(req.getTimeout());
      } catch (InterruptedException ie) {
        out.println(
            "<font color=\"red\">Interruped</font>");
      }
      print(res);
    }

    private void print(Response res) {
      out.print("<b>"+action+":</b><br>");
      if (res instanceof Response.Get) {
        if (!res.isAvailable()) {
          out.println("Not available yet");
        } else if (res.isSuccess()) {
          AddressEntry[] a =
            ((Response.Get) res).getAddressEntries();
          print(a);
        } else if (res.isTimeout()) {
          out.print("Timeout");
        } else {
          print(res.getException());
        }
      } else if (res instanceof Response.List) {
        if (!res.isAvailable()) {
          out.println("Not available yet");
        } else if (res.isSuccess()) {
          Set names = ((Response.List) res).getNames();
          print(names);
        } else if (res.isTimeout()) {
          out.print("Timeout");
        } else {
          print(res.getException());
        }
      } else if (res instanceof Response.Refresh) {
        if (!res.isAvailable()) {
          out.println("Not available yet");
        } else if (res.isSuccess()) {
          out.print("Old Entry:");
          Request req = res.getRequest();
          AddressEntry oldAE = 
            ((Request.Refresh) req).getOldEntry();
          print(oldAE);
          out.print("New Entry:");
          AddressEntry newAE = 
            ((Response.Refresh) res).getNewEntry();
          print(newAE);
        } else if (res.isTimeout()) {
          out.print("Timeout");
        } else {
          print(res.getException());
        }
      } else {
        AddressEntry ae;
        if (!res.isAvailable()) {
          out.println("Not available yet");
        } else if (res.isSuccess()) {
          out.println("Success");
          Request req = res.getRequest();
          if (req instanceof Request.Bind) {
            ae = ((Request.Bind) req).getAddressEntry();
          } else if (req instanceof Request.Rebind) {
            ae = ((Request.Rebind) req).getAddressEntry();
          } else if (req instanceof Request.Unbind) {
            ae = ((Request.Unbind) req).getAddressEntry();
          } else {
            out.println(res);
            return;
          }
          print(ae);
        } else if (res.isTimeout()) {
          out.println("Timeout");
        } else {
          print(res.getException());
        }
      }
    }

    private void printTableStart() {
      out.print(
          "<table border=1>\n"+
          "<tr>"+
          "<th>&nbsp;</th>"+
          "<th>Name</th>"+
          "<th>App</th>"+
          "<th>URI</th>"+
          "<th>Cert</th>"+
          "<th>TTL</th>"+
          "</tr>\n");
    }

    private void printTableEnd() {
      out.println("</table>");
    }

    private void print(AddressEntry[] a) {
      printTableStart();
      print(a, 0, null, null);
      printTableEnd();
    }

    private void print(AddressEntry ae) {
      printTableStart();
      print(ae, 0, null, null);
      printTableEnd();
    }

    private int print(
        AddressEntry[] a,
        int idx,
        Application appFilter,
        String schemeFilter) {
      int ret = idx;
      int n = (a == null ? 0 : a.length);
      for (int i = 0; i < n; i++) {
        AddressEntry ae = a[i];
        if (print(ae, ret, appFilter, schemeFilter)) {
          ++ret;
        }
      }
      return ret;
    }

    private boolean print(
        AddressEntry ae,
        int idx,
        Application appFilter,
        String schemeFilter) {
      if (ae != null &&
          (appFilter == null ||
           appFilter.equals(ae.getApplication())) &&
          (schemeFilter == null ||
           schemeFilter.equals(ae.getAddress().getScheme()))) {
        out.print(
            "<tr>"+
            "<td align=right>"+(idx+1)+"&nbsp;</td>"+
            "<td align=right>"+ae.getName()+"</td>"+
            "<td align=right>"+ae.getApplication()+"</td>"+
            "<td>"+ae.getAddress()+"</td>"+
            "<td>"+ae.getCert()+"</td>"+
            "<td>"+ae.getTTL()+"</td>"+
            "</td></tr>\n");
        return true;
      }
      return false;
    }

    private void print(Set names) {
      int n = (names==null ? 0 : names.size());
      out.println("Names["+n+"]:<br>");
      if (n <= 0) {
        return;
      }
      out.println("<table border=0>\n<li>");
      List l = new ArrayList(names);
      Collections.sort(l);
      for (int i = 0; i < n; i++) {
        String ni = (String) l.get(i);
        out.print(
            "<tr><td align=right>"+
            (i+1)+
            ".&nbsp;</td>"+
            "<td align=right>");
        if (ni == null) {
          out.print("<i>null?</i>");
        } else if (ni.length() == 0) {
          out.print("<i>empty?</i>");
        } else if (ni.charAt(0) != '.') {
          out.print(
              "<a href=\""+
              sreq.getRequestURI()+
              "?action=get&name="+
              ni+"\">"+ni+"</a>");
        } else {
          out.print(
              "<a href=\""+
              sreq.getRequestURI()+
              "?action=list&name="+
              ni+"\">"+ni+"</a>");
        }
        out.println("</td></tr>\n");
      }
      out.println("</table>");
    }

    private void print(Exception e) {
      out.println("Failure</td><td>");
      if (e == null) {
        out.print("null");
        return;
      }
      out.println("<pre>");
      e.printStackTrace(out);
      out.println("</pre>");
    }
  }
}

/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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

package org.cougaar.core.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.core.wp.ListAllAgents;

/**
 * An optional servlet for viewing the host/node assignment of an
 * agent (or agents) according to the White Pages Service.
 * <p>
 * Load into any agent:<pre>
 *   plugin = org.cougaar.core.util.LocateAgentServlet(/locate_agent)
 * </pre>
 * <p>
 * Takes argument agent=<agent name or comma separated list of names>
 * If you specify additional argument format=XML, then you get results
 * as an XML document.
 *
 * TODO: 
 * Clean up XML format?
 * Allow checkbox selection of agents? 
 * Allow all agents?
 */
public class LocateAgentServlet extends ComponentServlet {

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

    private static final String HTML_FORMAT = "HTML";
    private static final String XML_FORMAT = "XML";
    private String format = HTML_FORMAT;
    private String[] agents;
    private String lastAgent;

    public MyHandler(
        String localAgent,
        LoggingService log,
        WhitePagesService wps) {
      this.localAgent = localAgent;
      this.lastAgent = localAgent;
      this.log = log;
      this.wps = wps;
    }

    public void execute(
        HttpServletRequest sreq, 
        HttpServletResponse sres) throws IOException
      //, ServletException 
    {
      this.sreq = sreq;
      parseParams();
      if (format == XML_FORMAT) {
	// Print XML header
	sres.setContentType("text/xml");
	this.out = sres.getWriter();
	out.println("<?xml version='1.0'?>");

	performXMLRequest();
      } else {
	// Doing HTML page
	this.out = sres.getWriter();
	printHeader();
	performRequest();
	printForm();
	printFooter();
      }
      this.out.flush();
    }

    // Get parameters from the URI
    private void parseParams() {
      String form = paramSingle("format");

      // Only use the specified format if it is XML.
      // Else use default (HTML)
      if (XML_FORMAT.equalsIgnoreCase(form))
	format = XML_FORMAT;
      
      agents = param("agent");
      if (agents != null) {
	if (agents.length < 1)
	  agents = null;
	else 
	  lastAgent = agents[0];
      }

      // Parse a possible CSV separated list of agents
      if (agents != null && agents.length == 1) {
	// If agents is a comma separated list then split them now
	if (agents[0].indexOf(',') != -1) {
	  String oldags = agents[0].trim();
	  List newAgs = new ArrayList();
	  while (oldags.indexOf(',') != -1) {
	    String nxtString = oldags.substring(0, oldags.indexOf(','));
	    nxtString = nxtString.trim();
	    if (nxtString.length() > 0)
	      newAgs.add(nxtString);
	    if (oldags.length() <= oldags.indexOf(',') + 1) {
	      oldags = "";
	      break;
	    }
	    oldags = oldags.substring(oldags.indexOf(',') + 1);
	  }
	  oldags = oldags.trim();
	  if (oldags.length() > 0) 
	    newAgs.add(oldags);

	  // turn list to an array
	  agents = new String[newAgs.size()];
	  agents = (String []) newAgs.toArray(agents);
	}
      }
    }

    // Header of the HTML page
    private void printHeader() {
      long now = System.currentTimeMillis();
      out.println(
          "<html>\n"+
          "<head>\n"+
          "<title>Cougaar Locate Agent</title>\n"+
        "</head>\n"+
        "<body>"+
        "<h2>Cougaar Locate Agent Servlet</h2>"+
        "<p>\n"+
        "Agent: "+localAgent+
        "<p>\n"+
        "Time:  "+now+" ("+(new Date(now))+")"+
        "<p>");
    }

    // Start of HTML form for query generation
    private void printForm() {
      // FIXME: Support multiple agent names via checkboxes or some such
      out.println(
        "<table border=1>\n"+
        "<form name=\"f\" method=\"GET\" action=\""+
        sreq.getRequestURI()+
        "\">\n"+
        "<tr><th colspan=2>Find Location</th></tr>\n"+
        "<tr><td>Agent Name</td><td>"+
        input("agent", lastAgent)+
        "</td></tr>\n"+
        "<tr><td>"+
        "<input type=\"submit\" value=\"Locate\">\n"+
        "</td><td>"+
        "<input type=\"reset\" value=\"Reset\">\n"+
        "</td></tr>\n"+
        "</form>\n"+
        "</table>");
    }

    // Use the WP to get the agent host and node for given agent
    // return null if no agent name given
    // Return empty Node if no WP entry found, with a descriptive
    // Host name
    // First string returned is Host, 2nd is Node
    private String[] getAgentInfo(String theAgentName) {
      String[] ret = new String[2];
      ret[0] = "NOT FOUND IN WP?";
      ret[1] = "";

      if (theAgentName == null || theAgentName.length() == 0)
	return null;

      // timeout after ten seconds, use "-1" for no timeout
      long timeout = 10000;
      
      // do the lookup:
      AddressEntry ae;
      try {
	ae = wps.get(
				   theAgentName,
				   "topology",
				   timeout);
      } catch (Exception e) {
	// probably a timeout
	ae = null;
      }

      if (ae == null) {
	// not listed in the white pages?
      } else {
	URI uri = ae.getURI();
	// the uri looks like "node://HOST/NODE"
	ret[0] = uri.getHost();
	ret[1] = uri.getPath().substring(1);
      }
      return ret;
    }

    // Do the users request (for HTML page)
    private void performRequest() {
      if (agents == null)
	return;
      if (wps == null)
	return;

      printTableStart();
      for (int i = 0; i < agents.length; i++) {
	String theAgentName = agents[i];
	String[] ans = getAgentInfo(theAgentName);

	if (ans == null)
	  continue;

	// Print the results
	out.println(resultRowHTML(theAgentName, ans[0], ans[1]));
      } // end of for loop

      printTableEnd();
      out.println("<p><hr><p>");
    }
    
    // Do the users request (for XML return)
    private void performXMLRequest() {
      if (agents == null)
	return;
      if (wps == null)
	return;

      out.println("<society>");

       for (int i = 0; i < agents.length; i++) {
	String theAgentName = agents[i];
	String[] ans = getAgentInfo(theAgentName);

 	if (ans == null)
 	  continue;

 	// Print the results
 	out.println(resultRowXML(theAgentName, ans[0], ans[1]));
       } // end of for loop

       out.println("</society>");
    }
    
    // Print bottom of the HTML page
    private void printFooter() {
      out.println("</body></html>");
    }

    // Parse a multi-valued parameter (decoding it if necc.)
    private String[] param(String n) {
      String[] s = sreq.getParameterValues(n);
      if (s==null || s.length==0) {
        s = null;
      } else {
	for (int i = 0; i < s.length; i++) {
	  s[i] = URLDecoder.decode(s[i]);
	}
      }
      return s;
    }

    // Parse a single-valued parameter (decoding it if necc.)
    private String paramSingle(String n) {
      String s = sreq.getParameter(n);
      if (s==null || s.length()==0) {
        s = null;
      } else
	s = URLDecoder.decode(s);

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

    // Print an HTML row of output
    private static String resultRowHTML(String agent, String host, String node) {
      return "<tr> <td align=right>" + agent + "&nbsp;</td> " +
	"<td align=right>" + node + "&nbsp;</td>" + 
	"<td align=right>" + host + "&nbsp;</td></tr>";
    }

    // Print an XML row of output
    private static String resultRowXML(String agent, String host, String node) {
      return "  <agent name=\'" + agent + "\'\n" +
	     "    node=\'" + node + "\'" + 
	     "    host=\'" + host + "\'/>";
    }

    private void printTableStart() {
      out.print(
          "<table border=1>\n"+
          "<tr>"+
          "<th>Agent</th>"+
          "<th>Node</th>"+
          "<th>Host</th>"+
          "</tr>\n");
    }

    private void printTableEnd() {
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

    ///// For later use
    protected List getAllEncodedAgentNames() {
      try {
	// do full WP list (deprecated!)
	Set s = ListAllAgents.listAllAgents(wps);
	// URLEncode the names and sort
	List l = ListAllAgents.encodeAndSort(s);
	return l;
      } catch (Exception e) {
	throw new RuntimeException(
				   "List all agents failed", e);
      }
    }
    
    protected List getAllAgentNames() {
      try {
	// do full WP list (deprecated!)
	List result = new ArrayList(ListAllAgents.listAllAgents(wps));
	Collections.sort(result);
	return result;
      } catch (Exception e) {
	throw new RuntimeException(
				   "List all agents failed", e);
      }
    }
    
  } // end of MyHandler definition
}

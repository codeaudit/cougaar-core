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

package org.cougaar.core.qos.metrics;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Set;


import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.servlet.ServletService;

public class MetricsServlet extends HttpServlet implements Constants
{
    private final String myPath = "/qosmetrics";
    private final String ignoreColor = "<font color=\"#999999\">";
    private final String normalColor = "<font color=\"#000000\">";
    private final String highlightColor = "<font color=\"#cc00cc\">";
    private final String endColor = "</font>";

    private TopologyReaderService topologyService;
    private MetricsService metricsService;
    private String nodeID;
    private DecimalFormat formatter;

    public MetricsServlet(ServiceBroker sb) {
	ServletService servletService = (ServletService)
	    sb.getService(this, ServletService.class, null);
	if (servletService == null) {
	    throw new RuntimeException("Unable to obtain ServletService");
	}

	topologyService = (TopologyReaderService)
	    sb.getService(this, TopologyReaderService.class, null);
	if (servletService == null) {
	    throw new RuntimeException("Unable to obtain Topology service");
	}

	metricsService = (MetricsService)
	    sb.getService(this, MetricsService.class, null);

	NodeIdentificationService node_id_svc = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
	nodeID = node_id_svc.getNodeIdentifier().toString();
	

	// register our servlet
	try {
	    servletService.register(myPath, this);
	} catch (Exception e) {
	    throw new RuntimeException("Unable to register servlet at path <"
				       +myPath+ ">: " +e.getMessage());
	}

	formatter = new DecimalFormat("00.00");
    }

    private String loadColor(double value) {
	    if (value == 0) return ignoreColor;
	    if (value < 1.0 ) return  normalColor;
	    return  highlightColor;
    }

    private String credColor(double value) {
	    if (value == SECOND_MEAS_CREDIBILITY ) return ignoreColor;
	    return  highlightColor;
    }

    private void dumpTable(PrintWriter out) {
	Set matches = null;
	try {
	    matches = topologyService.getAllEntries(null,  // Agent
						    nodeID,
						    null, // Host
						    null, // Site
						    null); // Enclave
	} catch (Exception ex) {
	    // Node hasn't finished initializing yet
	    return;
	}
	if (matches == null) return;
	Iterator itr = matches.iterator();
	out.print("<table border=1>\n");
	while (itr.hasNext()) {
	    TopologyEntry entry = (TopologyEntry) itr.next();
	    if ((entry.getType() & TopologyReaderService.AGENT) == 0) continue;

	    String name = entry.getAgent();
	    String path = "Agent(" +name+ ")"
		+PATH_SEPR+ ONE_SEC_LOAD_AVG;
	    Metric metric = metricsService.getValue(path);
	    double value = metric.doubleValue();
	    double cred = metric.getCredibility();
	      
	    String formattedValue = formatter.format(value);
	    out.print("<tr><td><b>");
	    out.print(name);
	    out.print(" </b></td><td>");
	    out.print(loadColor(value));
	    out.print(formattedValue);
	    out.print("</font></td><td>");
	    out.print(credColor(cred));
	    out.print(cred);
	    out.print("</font></td></tr>\n");

	}
	out.print("</table>");
    }


    public void doGet(HttpServletRequest request,
		      HttpServletResponse response) 
	throws java.io.IOException 
    {

	String refresh = request.getParameter("refresh");
	int refreshSeconds = 
	    ((refresh != null) ?
	     Integer.parseInt(refresh) :
	     0);

	response.setContentType("text/html");
	PrintWriter out = response.getWriter();

	out.print("<html><HEAD>");
	if (refreshSeconds > 0) {
	    out.print("<META HTTP-EQUIV=\"refresh\" content=\"");
	    out.print(refreshSeconds);
	    out.print("\">");
	}
	out.print("<TITLE> Agent Load for Node ");
	out.print(nodeID);
	out.print("</TITLE></HEAD><body>");
	out.print("<H1> Agent Load for Node ");
	out.print(nodeID);
	out.print("</H1>");

	out.print("Date: ");
	out.print(new java.util.Date());

	dumpTable(out);
	out.print("<p><p><br>RefreshSeconds: ");
	out.print(refreshSeconds);
	out.print("</body></html>\n");

	out.close();
    }
}
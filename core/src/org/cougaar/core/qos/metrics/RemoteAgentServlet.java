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

public class RemoteAgentServlet extends HttpServlet implements Constants
{
    private final String myPath = "/metrics/remote/agent";

    private TopologyReaderService topologyService;
    private MetricsService metricsService;
    private String nodeID;
    private DecimalFormat f4_2,f6_3,f3_0;

    public RemoteAgentServlet(ServiceBroker sb) {
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

	f4_2 = new DecimalFormat("#0.00");
	f6_3 = new DecimalFormat("##0.000");
	f3_0 = new DecimalFormat("##0");
    }


    private void dumpTable(PrintWriter out) {
	Set matches = null;
	try {
	    matches = topologyService.getAllEntries(null,  // Agent
						    null,  // Node
						    null,  // Host
						    null,  // Site
						    null); // Enclave
	} catch (Exception ex) {
	    // Node hasn't finished initializing yet
	    return;
	}
	if (matches == null) return;

	out.print("<table border=1>\n");
	out.print("<tr><b>");
	out.print("<td><b>AGENTS</b></td>");
	out.print("<td><b>Heard</b></td>");
	out.print("<td><b>Spoke</b></td>");
	out.print("<td><b>Queue</b></td>");
	out.print("<td><b>MsgTo</b></td>");
	out.print("<td><b>MsgFrom</b></td>");
	out.print("<td><b>eMJIPS</b></td>");
	out.print("<td><b>Mbps</b></td>");
	out.print("</b></tr>");

	Iterator itr = matches.iterator();
	while (itr.hasNext()) {
	    TopologyEntry entry = (TopologyEntry) itr.next();
	    if ((entry.getType() & TopologyReaderService.AGENT) == 0) continue;

	    String name = entry.getAgent();
	    String agentPath = "Agent(" +name+ ")"+PATH_SEPR;
	    Metric eMJIPS = metricsService.getValue(agentPath+
						    "EffectiveMJips");
	    Metric queue = metricsService.getValue(agentPath+
						    "AvgQueueLength");
	    if(queue ==null) 
		queue= new MetricImpl(new Double(0.00), 0,"units","test");
	    Metric heard = metricsService.getValue(agentPath+
						    "HeardTime");
	    if(heard ==null) 
		heard = new MetricImpl(new Double(0.00), 0,"units","test");
	    Metric spoke = metricsService.getValue(agentPath+
						    "SpokeTime");
	    if(spoke ==null) 
		spoke = new MetricImpl(new Double(0.00), 0,"units","test");
	    Metric msgTo = metricsService.getValue(agentPath+
						    "MsgTo");
	    if(msgTo ==null) 
		msgTo = new MetricImpl(new Double(0.00), 0,"units","test");
	    Metric msgFrom=metricsService.getValue(agentPath+
						    "MsgFrom");
	    if(msgFrom ==null) 
		msgFrom = new MetricImpl(new Double(0.00), 0,"units","test");
	    Metric mbps=metricsService.getValue(agentPath+
						    "Mbps");
	    if(mbps ==null) 
		mbps = new MetricImpl(new Double(0.00), 0,"units","test");

	    //subtract now from spoke and heard 
	    long now = System.currentTimeMillis();

	    out.print("<tr><td><b>");
	    out.print(name);
	    out.print(" </b></td>");
	    out.print(Color.valueTable(spoke, 0.0, 30.0, true, f3_0));
	    out.print(Color.valueTable(heard, 0.0, 30.0, true, f3_0));
	    out.print(Color.valueTable(queue, 0.0, 1.0, true,  f4_2));
	    out.print(Color.valueTable(msgTo, 0.0, 1.0, true, f4_2));
	    out.print(Color.valueTable(msgFrom, 0.0, 1.0, true, f4_2));
	    out.print(Color.valueTable(eMJIPS, 10.0, 400.0, false, f3_0));
	    out.print(Color.valueTable(mbps, 0.0, 0.10, false, f6_3));
	    out.print("</tr>\n");

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
	out.print("<TITLE> Remote Agent Status for Node ");
	out.print(nodeID);
	out.print("</TITLE></HEAD><body>");
	out.print("<H1> Remote Agent Status for Node  ");
	out.print(nodeID);
	out.print("</H1>");

	out.print("Date: ");
	out.print(new java.util.Date());

	dumpTable(out);
	out.print("<p><p><br>RefreshSeconds: ");
	out.print(refreshSeconds);
	out.print("<p><p><b>Color key</b>");
	Color.colorTest(out);

	out.print("</body></html>\n");

	out.close();
    }
}

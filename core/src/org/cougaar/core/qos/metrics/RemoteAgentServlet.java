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

import java.util.Iterator;
import java.io.PrintWriter;
import java.util.Set;

import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyReaderService;


public class RemoteAgentServlet
    extends MetricsServlet
    implements Constants
{

    public RemoteAgentServlet(ServiceBroker sb) {
	super(sb);
    }



    protected String myPath() {
	return "/metrics/remote/agent";
    }

    protected String myTitle () {
	return "Remote Agent Status for Node " + nodeID;
    }

    protected void outputPage(PrintWriter out) {
	// Get list of All Agents in society
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

	//Header Row
	out.print("<table border=1>\n");
	out.print("<tr><b>");
	out.print("<td><b>AGENTS</b></td>");
	out.print("<td><b>Spoke To</b></td>");
	out.print("<td><b>Heard From</b></td>");
	out.print("<td><b>Queue</b></td>");
	out.print("<td><b>MsgTo</b></td>");
	out.print("<td><b>MsgFrom</b></td>");
	out.print("<td><b>eMJIPS</b></td>");
	out.print("<td><b>Mbps</b></td>");
	out.print("</b></tr>");

	long now = System.currentTimeMillis();
	//Rows
	Iterator itr = matches.iterator();
	while (itr.hasNext()) {
	    // Get Agent
	    TopologyEntry entry = (TopologyEntry) itr.next();
	    if ((entry.getType() & TopologyReaderService.AGENT) == 0) continue;
	    String name = entry.getAgent();
	    String agentPath = "Agent(" +name+ ")"+PATH_SEPR;
	    // Get Metrics
	    Metric eMJIPS = metricsService.getValue(agentPath+
						    "EffectiveMJips");
	    Metric queue = metricsService.getValue(agentPath+
						   "AvgQueueLength");
	    if(queue ==null) 
		queue= new MetricImpl(new Double(0.00), 0,"units","test");


	    Metric heard = metricsService.getValue(agentPath+
						   "LastHeard");
	    if (heard ==null) {
		heard = new MetricImpl(new Double(0.00), 0,"units","test");
	    }

	    Metric spoke = metricsService.getValue(agentPath+
						   "LastSpoke");
	    if (spoke ==null) {
		spoke = new MetricImpl(new Double(0.00), 0,"units","test");
	    } 

	    Metric msgTo = metricsService.getValue(agentPath+
						   "MsgTo");
	    if (msgTo ==null) 
		msgTo = new MetricImpl(new Double(0.00), 0,"units","test");

	    Metric msgFrom=metricsService.getValue(agentPath+
						   "MsgFrom");
	    if (msgFrom ==null) 
		msgFrom = new MetricImpl(new Double(0.00), 0,"units","test");

	    Metric mbps=metricsService.getValue(agentPath+
						"Mbps");
	    if (mbps ==null) 
		mbps = new MetricImpl(new Double(0.00), 0,"units","test");



	    //output Row
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
}

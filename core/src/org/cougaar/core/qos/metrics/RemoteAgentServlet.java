/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Set;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Application;

public class RemoteAgentServlet
    extends MetricsServlet
    implements Constants
{

    private static final Application TOPOLOGY = 
	Application.getApplication("topology");
    private static final String SCHEME = "node";
    private static String localHost;

    static {
	try {
	    InetAddress localAddr = InetAddress.getLocalHost();
	    localHost = localAddr.getHostAddress();
	} catch (java.net.UnknownHostException uhe) {
	    localHost = "127.0.0.1";
	}
    }

    private AgentStatusService agentStatusService; 

    public RemoteAgentServlet(ServiceBroker sb) {
	super(sb);

	agentStatusService = (AgentStatusService)
	    sb.getService(this, AgentStatusService.class, null);
    }



    protected String myPath() {
	return "/metrics/remote/agents";
    }

    protected String myTitle () {
	return "Remote Agent Status for Node " + nodeID;
    }

    private String canonicalizeAddress(String hostname) {
	try {
	    InetAddress addr = InetAddress.getByName(hostname);
	    return addr.getHostAddress();
	} catch (java.net.UnknownHostException uhe) {
	    return hostname;
	}
    }

    protected void outputPage(PrintWriter out) {
	// Get list of All Agents in society
	Set matches = agentStatusService.getRemoteAgents();
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
	out.print("<td><b>mMbps</b></td>");
	out.print("<td><b>eMbps</b></td>");
	out.print("</b></tr>");

	long now = System.currentTimeMillis();
	//Rows
	Iterator itr = matches.iterator();
	while (itr.hasNext()) {
	    MessageAddress agent = (MessageAddress) itr.next();
	    String name = agent.getAddress();

	    // Hack!  Skip the MTS internal pseudo Agent.
	    if (name.endsWith("(MTS)")) continue;


	    // Warning: Agents or Nodes whose names include parens or
	    // commas will confuse the RSS parser. Fix this in RSS.
		

	    AgentStatusService.AgentState state = 
		agentStatusService.getRemoteAgentState(agent);
	    String agentHost = null;
	    try {
		AddressEntry entry = wpService.get(name, TOPOLOGY, SCHEME);
		if (entry == null) {
		    agentHost = localHost;
		} else {
		    agentHost = entry.getAddress().getPath().substring(1);
		}
		agentHost = canonicalizeAddress(agentHost);
	    } catch (Exception ex1) {
	    }
	    
	    String agentPath = "Agent(" +name+ ")"+PATH_SEPR;
	    String destPath="Node("+nodeID+")"+PATH_SEPR+
		"Destination("+name+")"+PATH_SEPR;
	    String ipFlowPath="IpFlow(" +localHost+ "," +agentHost+ ")"
		+PATH_SEPR;
	    // Get Metrics
	    Metric eMJIPS = metricsService.getValue(agentPath+
						    "EffectiveMJips");

	    int qLength = 0;
	    double qCredibility = NO_CREDIBILITY;
	    String qProvenance = "none";
	    if (state != null) {
		qLength = state.queueLength;
		qCredibility = SECOND_MEAS_CREDIBILITY;
		qProvenance = "AgentStatusService";
	    }

	    Metric queue = new MetricImpl(new Integer(qLength), 
					  qCredibility,
					  "none",
					  qProvenance);

	    Metric msgTo = metricsService.getValue(destPath+
						   MSG_TO_10_SEC_AVG);
	    Metric msgFrom = metricsService.getValue(destPath+
						   MSG_FROM_10_SEC_AVG);
	    Metric bytesTo= metricsService.getValue(destPath+
						       BYTES_TO_10_SEC_AVG);
	    Metric bytesFrom = metricsService.getValue(destPath+
						   BYTES_FROM_10_SEC_AVG);
	    Metric eMbps = metricsService.getValue(ipFlowPath+
						   "CapacityUnused");
	    Metric mMbps = metricsService.getValue(ipFlowPath+
						   "CapacityMax");

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

	    if (msgTo ==null) 
		msgTo = new MetricImpl(new Double(0.00), 0,"units","test");

	    if (msgFrom ==null) 
		msgFrom = new MetricImpl(new Double(0.00), 0,"units","test");

	    if (eMbps ==null) 
		eMbps = new MetricImpl(new Double(0.00), 0,"units","test");

	    if (mMbps ==null) 
		mMbps = new MetricImpl(new Double(0.00), 0,"units","test");



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
	    out.print(Color.valueTable(mMbps, 0.0, 0.10, false, f6_3));
	    out.print(Color.valueTable(eMbps, 0.0, 0.10, false, f6_3));
	    out.print("</tr>\n");
	}
	out.print("</table>");
    }
}

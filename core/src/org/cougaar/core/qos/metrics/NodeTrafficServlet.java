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
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;

public class NodeTrafficServlet
    extends org.cougaar.core.mts.BaseServlet // MetricsServlet
    implements Constants
{
    private static final Metric NO_VALUE = MetricImpl.UndefinedMetric;
    private static final String AGENT_PARAM = "agent";

    // Temporary, until this class extends MetricsServlet again.
    private final java.text.DecimalFormat f4_2 = 
	new java.text.DecimalFormat("#0.00");
    private MetricsService metricsService;


    private String agent;
    private AgentStatusService agentStatusService;

    public NodeTrafficServlet(ServiceBroker sb) {
	super(sb);
    
	// Temporary, until this class extends MetricsServlet again.
	metricsService = (MetricsService)
	    sb.getService(this, MetricsService.class, null);


	agentStatusService = (AgentStatusService)
	    sb.getService(this, AgentStatusService.class, null);
    }
  
    public String getPath() {
	return "/metrics/agent/traffic";
    }
  
    public String getTitle () {
	return "Detailed Traffic for Agent " + agent;
    }
  
    private void printRow(String rowAgent,PrintWriter out) {
       
	String toFlowPath = "AgentFlow("+agent+ ","  +rowAgent+ ")"+PATH_SEPR;
	String fromFlowPath = "AgentFlow(" +rowAgent+ ","  +agent+ ")"+PATH_SEPR;
	// Get Metrics
	Metric msgTo = metricsService.getValue(toFlowPath+
					       MSG_RATE_100_SEC_AVG);
	if (msgTo==null) msgTo=NO_VALUE;

	Metric msgFrom = metricsService.getValue(fromFlowPath+
						 MSG_RATE_100_SEC_AVG);
	if (msgFrom==null) msgFrom=NO_VALUE;

	Metric bytesTo= metricsService.getValue(toFlowPath+
						BYTE_RATE_100_SEC_AVG);
	if (bytesTo==null) bytesTo=NO_VALUE;

	Metric bytesFrom = metricsService.getValue(fromFlowPath+
						   BYTE_RATE_100_SEC_AVG);      
	if (bytesFrom==null) bytesFrom=NO_VALUE;

	//output Row
	out.print("<tr><td><b>");
	out.print(rowAgent);
	out.print(" </b></td>");
	ServletUtilities.valueTable(msgTo, 0.0, 1.0, true,  f4_2, out);
	ServletUtilities.valueTable(bytesTo, 0.0, 1000.0, true, f4_2, out);
	ServletUtilities.valueTable(msgFrom, 0.0, 1.0, true, f4_2, out);
	ServletUtilities.valueTable(bytesFrom, 0.0, 1000.0, true, f4_2, out);
	out.print("</tr>\n");
    }

    // agent needs to be set before the BaseServlet doGet runs
    public void doGet(HttpServletRequest request,
		      HttpServletResponse response) 
	throws java.io.IOException 
    {
    	agent = request.getParameter(AGENT_PARAM); // getNodeID();
	if (agent == null) agent = getNodeID().getAddress(); // default
	super.doGet(request, response);
    }


    public void printPage(HttpServletRequest request, PrintWriter out) {
	// Get list of All Agents in society
	Set matches = agentStatusService.getRemoteAgents();
	if (matches == null) return;

	//Header Row
	out.print("<table border=1>\n");
	out.print("<tr>");
	out.print("<th rowspan=\"2\">AGENTS</th>");
	out.print("<th colspan=\"2\">");
	out.print("From Agent ");
	out.print(agent);
	out.print("</th>");
	out.print("<th colspan=\"2\">");
	out.print("To Agent ");
	out.print(agent);
	out.print("</th>");
	out.print("</tr>");
	out.print("<tr>");
	out.print("<th>To Msg/Sec</th>");
	out.print("<th>To Bytes/Sec</th>");
	out.print("<th>From Msg/Sec</th>");
	out.print("<th>From Bytes/Sec</th>");
	out.print("</tr>");

    
	//Rows
	Iterator itr = matches.iterator();
	while (itr.hasNext()) {
	    MessageAddress interlocuter = (MessageAddress) itr.next();
	    String name = interlocuter.getAddress();
	    printRow(name,out);
	}
	out.print("</table>");

	out.println("<p>To change the Agent or auto-refresh use cgi parameters: ?agent=agentname&refresh=seconds<p>");
    }
}

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
import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;

public class AgentLoadServlet 
    extends MetricsServlet
    implements Constants
{
    private AgentStatusService agentStatusService; 

    public AgentLoadServlet(ServiceBroker sb) {
	super(sb);

	agentStatusService = (AgentStatusService)
	    sb.getService(this, AgentStatusService.class, null);
    }

    public String getPath() {
	return "/metrics/agent/load";
    }

    public String getTitle () {
	return "Agent Load for Node " + getNodeID();
    }

    public void printPage(HttpServletRequest request, PrintWriter out) {
	// Get list of All Agents On this Node
	java.util.Set localAgents = agentStatusService.getLocalAgents();
	if (localAgents == null) return;

	//Header Row
	out.print("<table border=1>\n");
	out.print("<tr><b>");
	out.print("<td><b>AGENT</b></td>");
	out.print("<td><b>CPUloadAvg</b></td>");
	out.print("<td><b>CPUloadMJIPS</b></td>");
	out.print("<td><b>MsgIn</b></td>");
	out.print("<td><b>MsgOut</b></td>");
	out.print("<td><b>BytesIn</b></td>");
	out.print("<td><b>BytesOut</b></td>");
	out.print("<td><b>PersistSize</b></td>");
	out.print("</b></tr>");

	//Rows
	for (Iterator itr = localAgents.iterator(); itr.hasNext(); ) {
	    // Get Agent
	    MessageAddress addr = (MessageAddress) itr.next();
	    String name = addr.getAddress();
	    String agentPath = "Agent(" +name+ ")"+PATH_SEPR;

	    // Get Metrics
	    Metric cpuLoad = metricsService.getValue(agentPath
						     + CPU_LOAD_AVG_10_SEC_AVG);

	    Metric cpuLoadJips = 
		metricsService.getValue(agentPath
					+ CPU_LOAD_MJIPS_10_SEC_AVG);
	    Metric msgIn = metricsService.getValue(agentPath+
						   MSG_IN_10_SEC_AVG);
	    Metric msgOut = metricsService.getValue(agentPath+
						   MSG_OUT_10_SEC_AVG);
	    Metric bytesIn = metricsService.getValue(agentPath+
						   BYTES_IN_10_SEC_AVG);
	    Metric bytesOut = metricsService.getValue(agentPath+
						   BYTES_OUT_10_SEC_AVG);
	    Metric persistSize = metricsService.getValue(agentPath+
						  PERSIST_SIZE_LAST );


	    //output Row
	    out.print("<tr><td><b>");
	    out.print(name);
	    out.print(" </b></td>");
	    ServletUtilities.valueTable(cpuLoad, 0.0, 1.0,true, f4_2, out);
	    ServletUtilities.valueTable(cpuLoadJips, 0.0, 200,true, f6_3, out);
	    ServletUtilities.valueTable(msgIn, 0.0, 1.0, true, f4_2, out);
	    ServletUtilities.valueTable(msgOut, 0.0, 1.0, true, f4_2, out);
	    ServletUtilities.valueTable(bytesIn, 0.0, 10000, true, f7_0, out);
	    ServletUtilities.valueTable(bytesOut, 0.0, 10000, true, f7_0, out);
	    ServletUtilities.valueTable(persistSize, 0.0, 10000, true, f7_0, out);
	    out.print("</tr>\n");

	}
	out.print("</table>");

	//Service table

	out.print("<h2>Services</h2>\n");
	//Header Row
	out.print("<table border=1>\n");
	out.print("<tr>");
	out.print("<th>AGENT</th>");
	out.print("<th>CPUloadAvg</th>");
	out.print("<th>CPUloadMJIPS</th>");
	out.print("</tr>");

	out.print("<tr><td><b>MTS</b></td>");
	String mtsLoadPath = "Service(MTS)" +PATH_SEPR+ CPU_LOAD_AVG_10_SEC_AVG;
	Metric mtsCpuLoad = metricsService.getValue(mtsLoadPath);
	ServletUtilities.valueTable(mtsCpuLoad, 0.0, 1.0,true, f4_2, out);
	String mtsMJIPSPath = "Service(MTS)" +PATH_SEPR+ 
	    CPU_LOAD_MJIPS_10_SEC_AVG;
	Metric mtsCpuMJIPS = metricsService.getValue(mtsMJIPSPath);
	ServletUtilities.valueTable(mtsCpuMJIPS, 0.0, 500.0,true, f6_3, out);
	out.print("</tr>\n");

	out.print("<tr><td><b>Metrics</b></td>");
	String metricLoadPath = "Service(Metrics)" +PATH_SEPR+ 
	    CPU_LOAD_AVG_10_SEC_AVG;
	Metric metricCpuLoad = metricsService.getValue(metricLoadPath);
	ServletUtilities.valueTable(metricCpuLoad, 0.0, 1.0,true, f4_2, out);
	String metricMJIPSPath = "Service(Metrics)" +PATH_SEPR+ 
	    CPU_LOAD_MJIPS_10_SEC_AVG;
	Metric metricCpuMJIPS = metricsService.getValue(metricMJIPSPath);
	ServletUtilities.valueTable(metricCpuMJIPS, 0.0, 500.0,true, f6_3, out);

	out.print("</tr>\n");

	out.print("</table>");

    }
}

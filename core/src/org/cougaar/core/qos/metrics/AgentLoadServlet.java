/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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
	if (agentStatusService == null) return;
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

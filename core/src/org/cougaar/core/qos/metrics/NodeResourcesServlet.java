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
import java.util.Iterator;
import java.util.Set;
import java.text.DecimalFormat;


import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyReaderService;


public class NodeResourcesServlet 
    extends MetricsServlet
    implements Constants
{
    public NodeResourcesServlet(ServiceBroker sb) {
	super(sb);
    }
    protected String   myPath() {
	return "/metrics/resources";
    }

    protected String myTitle () {
	return "Resources for Node " + nodeID;
    }


    private void outputMetric(PrintWriter out,
			      String path,
			      String name,
			      double ignore,
			      double highlight,
			      boolean greater,
			      DecimalFormat formatter) 
    {
	Metric metric = metricsService.getValue(path+name);
	if (metric == null)
	    metric= new MetricImpl(new Double(0.00), 0,"units","test");
	out.print("<tr><td><b>");
	out.print(name);
	out.print(" </b></td>");
	out.print(Color.valueTable(metric, ignore, 
				   highlight,greater,formatter));
	out.print(Color.credTable(metric));
	out.print("</tr>\n");	
    }

    protected void outputPage(PrintWriter out) {
	String nodePath = "Agent(" +nodeID+ ")"+PATH_SEPR;
	
	//Header Row
	out.print("<table border=1>\n");
	out.print("<tr><b>");
	out.print("<td><b>RESOURCE</b></td>");
	out.print("<td><b>Value</b></td>");
	out.print("<td><b>Cred</b></td>");
	out.print("</b></tr>");

	//Rows
	outputMetric(out,nodePath,"EffectiveMJips",
		     10.0, 400.0, false, f3_0);
	outputMetric(out,nodePath,"TcpInUse",
		     0.0, 20.0, true, f3_0);
	outputMetric(out,nodePath,"UdpInUse",
		     0.0, 20.0, true, f3_0);
	outputMetric(out,nodePath,"TotalMemory",
		     0.0, 256000.0, false, f7_0);
	outputMetric(out,nodePath,"FreeMemory",
		     0.0, 32000, false, f7_0);
	outputMetric(out,nodePath,"Cache",
		     0.0, 20.0, true, f7_0);


	out.print("</table>");
    }
}

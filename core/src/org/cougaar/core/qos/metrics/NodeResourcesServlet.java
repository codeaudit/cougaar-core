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
import java.text.DecimalFormat;
import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;

public class NodeResourcesServlet 
    extends MetricsServlet
    implements Constants
{
    public NodeResourcesServlet(ServiceBroker sb) {
	super(sb);
    }

    public String getPath() {
	return "/metrics/resources";
    }

    public String getTitle () {
	return "Resources for Node " + getNodeID();
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
	Color.valueTable(metric, ignore, 
			 highlight,greater,formatter, out);
	out.print("</tr>\n");	
    }

    public void printPage(HttpServletRequest request, PrintWriter out) {
	String nodePath = "Agent(" +getNodeID()+ ")"+PATH_SEPR;
	
	//Header Row
	out.print("<table border=1>\n");
	out.print("<tr>");
	out.print("<th>RESOURCE</th>");
	out.print("<th>Value</th>");
	out.print("</tr>");

	//Rows
	outputMetric(out,nodePath,"EffectiveMJips",
		     10.0, 400.0, false, f2_0);
	outputMetric(out,nodePath,"LoadAverage",
		     0.0, 4.0, true, f4_2);
	outputMetric(out,nodePath,"Count",
		     1.0, 5.0, true, f2_0);
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

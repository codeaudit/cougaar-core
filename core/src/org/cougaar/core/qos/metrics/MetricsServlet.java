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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Date;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.ServletService;
import org.cougaar.core.service.TopologyReaderService;

public abstract class MetricsServlet extends HttpServlet implements Constants
{

    protected TopologyReaderService topologyService;
    protected MetricsService metricsService;
    protected String nodeID;
    protected DecimalFormat f4_2,f6_3,f3_0,f7_0;

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
 	nodeID = node_id_svc.getMessageAddress().toString();
	

	// register our servlet
	try {
	    servletService.register(myPath(), this);
	} catch (Exception e) {
	    throw new RuntimeException("Unable to register servlet at path <"
				       +myPath()+ ">: " +e.getMessage());
	}

	f4_2 = new DecimalFormat("#0.00");
	f6_3 = new DecimalFormat("##0.000");
	f3_0 = new DecimalFormat("##0");
	f7_0 = new DecimalFormat("#######0");
    }
    protected abstract String myPath();
    protected abstract String myTitle();
    protected abstract void outputPage(PrintWriter out);


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
	out.print("<TITLE>");
	out.print(myTitle());
	out.print("</TITLE></HEAD><body><H1>");
	out.print(myTitle());
	out.print("</H1>");

	out.print("Date: ");
	out.print(new java.util.Date());
	
	outputPage(out);
	out.print("<p><p><br><h2>KEYS</h2>RefreshSeconds: ");	
	out.print(refreshSeconds);
	out.print("<p><p><b>Color key</b>");
	Color.colorTest(out);

	out.print("</body></html>\n");

	out.close();
    }
}

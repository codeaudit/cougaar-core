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

package  org.cougaar.core.qos.metrics;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.SerializationUtils;
import org.cougaar.core.service.ServletService;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;


public class MetricsWriterServlet
    extends HttpServlet
    implements Constants
{
    private MetricsUpdateService metricsUpdateService;
  
    public MetricsWriterServlet(ServiceBroker sb) {
	// Register our servlet with servlet service
	ServletService servletService = (ServletService)
	    sb.getService(this, ServletService.class, null);
	if (servletService == null) {
	    throw new RuntimeException("Unable to obtain ServletService");
	}
	try {
	    servletService.register(getPath(), this);
	} catch (Exception e) {
	    throw new RuntimeException("Unable to register servlet at path <"
				       +getPath()+ ">: " +e.getMessage());
	}
   
	// get metrics service
	try {
	    metricsUpdateService = (MetricsUpdateService)
		sb.getService(this, MetricsUpdateService.class, null);
	} catch (Exception e) {
	    throw new RuntimeException("Unable to get MetricsUpdateService: "
				       +e.getMessage());
	}
    }
  
    public String getPath() 
    {
	return "/metrics/writer";
    }
    
    public String getTitle () 
    {
	return "Remote Metrics Writer for Node";
    }
    
    /*
     * Parses params, and send out either a propertylist of
     * metrics(java version), or a string of xml-formatted text,
     * through a serialized byte array.
     */
    public void printPage(HttpServletRequest request, PrintWriter out) 
    {
	try {		  
	    
	    // parses params
	    String uri = request.getRequestURI();	    
	    String key = request.getParameter("key");
	    String value = request.getParameter("value");
	    
	    if (key==null || value==null) {
		out.print("Key or Value is null");
		return;
	    }
	    
	    Metric metric = new MetricImpl(Double.parseDouble(value),
					   USER_DEFAULT_CREDIBILITY,
					   null,
					   request.getRemoteHost());
	    metricsUpdateService.updateValue(key, metric);
	    out.print("Key: " + key + ", Value: " + value+ 
		      "From: " + request.getRemoteHost() +"\n");
	} catch(Exception e) {
	}
    }
    
    // servlet requirement - pass to our print method to handle
    public void doGet(HttpServletRequest request,
		      HttpServletResponse response) 
	throws java.io.IOException 
    {
	PrintWriter out = response.getWriter();
	printPage(request, out);
    }
}

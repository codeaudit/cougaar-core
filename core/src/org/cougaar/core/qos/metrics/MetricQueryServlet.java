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

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.ServletService;

/*
 * Servlet to allow remote access into the metrics service.
 * Takes url arguments(paths) separated by the '|' key. 
 * Usage: 
 * http://localhost:8800/$3-69-ARBN/metrics/query?format=xml&paths=Agent($3-69-ARBN):Jips|Agent($3-69-ARBN):CPULoadJips10SecAvg
 * Optional 'format' argument, but if left out defaults to xml return of metric data to the browser
 * Run the core/examples/org/cougaar/core/examples/metrics/ExampleMetricQueryClient for java version
 */

public class MetricQueryServlet
    extends HttpServlet
{
  private MetricsService metricsService;
  
  public MetricQueryServlet(ServiceBroker sb) {
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
    metricsService = (MetricsService)
      sb.getService(this, MetricsService.class, null);
    } catch (Exception e) {
      throw new RuntimeException("Unable to get MetricsService at path <"
				 +getPath()+ ">: " +e.getMessage());
    }
  }
  
  public String getPath() {
    return "/metrics/query";
  }

  public String getTitle () {
    return "Remote Metrics Access for Node";
  }
  
  /*
   * Parses params, and send out either a propertylist of metrics(java version),
   * or a string of xml-formatted text, through a serialized byte array. 
   */
  public void printPage(HttpServletRequest request, OutputStream out) {
    
    String metrics = null;
    HashMap propertylist = null;
    
    try {
      // parses params
      String paths = request.getParameter("paths");
      String format = "xml";
      format = request.getParameter("format");
      
      // default format is a string of xml
      if(format != null && format.equals("java"))
	{
	  // parse paths and send out serialized data    
	  propertylist = build_propertylist(paths);
	}
      else  // it's xml
	{
	  metrics = build_string(paths);
	  out.write(metrics.getBytes());
	  return;
	}
      
      ObjectOutputStream oos = null;
      
      // serialize and send out java propertylist of metric data
      try {
	oos = new ObjectOutputStream(out);
	oos.writeObject(propertylist);
	
	// don't close, that will end the stream, we don't want to do that
	//oos.close(); 
	
      } catch(Exception e) {
	// log here eventuually
	System.out.println("Error writing metrics data " + e);
      }
    } catch(Exception e) {
      // also log here
      System.out.println("Exception: " + e);
      e.printStackTrace();
    }
  }
  
  public String build_string(String paths) 
  {
      if (paths == null) return
	  "<?xml version='1.0'?>\n"+
	  "<!-- Null Metrics Query -->\n"+
	  "<!-- Usage: /metrics/query/?paths=Host(Foo):Jips|Host(Foo):LoadAverage -->\n"+
	  "<paths></paths>\n";

    StringTokenizer st = new StringTokenizer(paths, "|");
    
    /* If xml, then parse into xml string and return 
       Calls ServletUtilities.XMLString(Metric) for easy xml print format
       It looks like (without the carriage return):
       <paths>
       <path>
       <name>pathname</name>
       <value>metricvalue</value>
       <units>unitvalue</units>
       <credibility>credibilityvalue</credibility>
       <provenance>no idea what this is</provenance>
       <timestamp>timestamp</timestamp>
       <halflife>no idea what this is either</halflife>
       </path>
       </paths>
    */
    
    // build string of xml metrics
    String metrics = new String("<?xml version='1.0'?>");
    metrics = metrics+"<paths>";
    
    // build property list
    while (st.hasMoreTokens()) {
      String path = st.nextToken();
      metrics = metrics+"<path>";
      metrics = metrics+"<name>";
      metrics = metrics+path;
      metrics = metrics+"</name>";
      
      try {
	Metric pathMetric = metricsService.getValue(path);
	// here we call XMLString(Metric), which is an xml toString() for Metric
	metrics = metrics+ServletUtilities.XMLString(pathMetric);
      } catch(Exception e) {
	  metrics=metrics+ServletUtilities.XMLString(null);
      }
      metrics = metrics+"</path>";
    }
    metrics = metrics+"</paths>";
    return metrics;
  }
  
  /*
   * Build a java HashMap, instead of xml
   * Each element in the list will have the form: 'path|metric'
   */
  public HashMap build_propertylist(String paths) 
  {
      if (paths == null) return null;

    StringTokenizer st = new StringTokenizer(paths, "|");
    
    // build propertylist 
    // element has the form: 'Query|Metric' 
    HashMap propertylist = new HashMap();
    
    // build property list
    while (st.hasMoreTokens()) {
      String path = st.nextToken();
      Metric pathMetric=null;
      try {
	  pathMetric = metricsService.getValue(path);
      } catch(Exception e) {
	  // Bad path spec Leave pathMetric as null or "Undefined"
      }
      propertylist.put(path, pathMetric);
    }
    return propertylist; 
  }
  
  
  // servlet requirement - pass to our print method to handle
  public void doGet(HttpServletRequest request,
		      HttpServletResponse response) 
    throws java.io.IOException 
  {
    OutputStream out = response.getOutputStream();
    printPage(request, out);
  }
}

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

/* Colors picks a color for displaying a Metric. The both metric value
 * and metric credibility a taken into account when choosing the
 * color.  Nine color bins are defined, with each dimension having
 * three places. The metric value can has thresholds for Ignore,
 * Normal, Highlight and the credibility has thresholds for Default,
 * Config, Measured
 *
 * The current color scheme tries to make Highlight standout with a
 * bright purple and hide Ignore with a yellow. Lower credibility will
 * tend to fade out.
 */
public class Color  implements Constants {
    private static final DecimalFormat f2_1 = new DecimalFormat("0.0");

    private static final String PALE_YELLOW = "\"#ffffee\"";
    private static final String PALE_GREEN = "\"#eeffee\"";
    private static final String PALE_PINK = "\"#ffeeee\"";

    private static final String LIGHT_GRAY = "\"#cccccc\"";
    private static final String MEDIUM_GRAY = "\"#888888\"";
    private static final String BLACK = "\"#000000\"";

    private static String brightness(Metric metric) 
    {
	double credibility = metric.getCredibility();
	if (credibility <=  DEFAULT_CREDIBILITY) {
	    return LIGHT_GRAY;
	} else if (credibility <=  SYS_DEFAULT_CREDIBILITY) {
	    return MEDIUM_GRAY;
	} else {
	    return BLACK;
	}
    }

    private static String bgcolor(Metric metric,
				  double uninteresting_value,
				  double threshold,
				  boolean increasing)
    {
	double value = metric.doubleValue();
	if (value == uninteresting_value) {
	    return  PALE_YELLOW;
	} else if (increasing && value >= threshold ||
		   !increasing && value <= threshold) {
	    return PALE_PINK;
	} else {
	    return PALE_GREEN;
	}
    }


    private static String mouse_doc(Metric metric)
    {
	return metric.toString();
    }
				  

    public static void valueTable(Metric metric, 
				  double uninteresting_value, // the SPECIAL! one
				  double threshold,
				  boolean increasing, // polarity of comparison
				  DecimalFormat formatter,
				  PrintWriter out) 
    {
	String brightness = brightness(metric);
	String bgcolor = bgcolor(metric, uninteresting_value, threshold, increasing);
	String mouse_doc = mouse_doc(metric);
	String value_text = formatter.format(metric.doubleValue());

	out.print("<td");
	out.print(" onmouseover=\"window.status='");
	out.print(mouse_doc);
	out.print("'; return true;\"");

	out.print(" onmouseout=\"window.status=''; return true;\"");

	out.print(" bgcolor=");
	out.print(bgcolor);

	out.print(">");

	out.print("<font color=");
	out.print(brightness);
	out.print(">");

	out.print(value_text);

	// end <font>
	out.print("</font>");


	out.print("</td>");
    }

    
    public static void colorTest(PrintWriter out) {
	Metric metric;

	out.print("<table border=1>\n <tr>");

	out.print("<th>VALUE \\ CRED</th>");
	out.print("<th>Default</th>");
	out.print("<th>Config</th>");
	out.print("<th>Measured</th>");
	out.print("</tr>");
	
	// row "ignore"
	out.print("<tr><td><b>Ignore</b></td>");

	metric = new MetricImpl(new Double(0.00), DEFAULT_CREDIBILITY,
				"units","test");
	valueTable(metric,0.0,1.0,true,f2_1, out);

	metric = new MetricImpl(new Double(0.00),SYS_DEFAULT_CREDIBILITY,
				"units","test");
	valueTable(metric,0.0,1.0,true,f2_1, out);

	metric = new MetricImpl(new Double(0.00),SECOND_MEAS_CREDIBILITY,
				"units","test");
	valueTable(metric,0.0,1.0,true,f2_1, out);
	out.print("</tr>");

	// row "Normal"
	out.print("<tr><td><b>Normal</b></td>");

	metric = new MetricImpl(new Double(0.50), DEFAULT_CREDIBILITY,
				"units","test");
	valueTable(metric,0.0,1.0,true,f2_1, out);

	metric = new MetricImpl(new Double(0.50),SYS_DEFAULT_CREDIBILITY,
				"units","test");
	valueTable(metric,0.0,1.0,true,f2_1, out);

	metric = new MetricImpl(new Double(0.50),SECOND_MEAS_CREDIBILITY,
				"units","test");
	valueTable(metric,0.0,1.0,true,f2_1, out);
	out.print("</tr>");

	// row "highlight"
	out.print("<tr><td><b>Highlight</b></td>");

	metric = new MetricImpl(new Double(1.00), DEFAULT_CREDIBILITY,
				"units","test");
	valueTable(metric,0.0,1.0,true,f2_1, out);

	metric = new MetricImpl(new Double(1.00),SYS_DEFAULT_CREDIBILITY,
				"units","test");
	valueTable(metric,0.0,1.0,true,f2_1, out);

	metric = new MetricImpl(new Double(1.00),SECOND_MEAS_CREDIBILITY,
				"units","test");
	valueTable(metric,0.0,1.0,true,f2_1, out);
	out.print("</tr>");
	
	out.print("</table>");
    }

}

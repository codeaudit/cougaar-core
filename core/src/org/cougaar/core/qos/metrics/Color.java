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
    // Colors are hand picked, since there are only 9 colors 
    //ignore is a yellow-orange
    public static final String ignoreMeas    = "<font color=\"#FFaa00\">"; 
    public static final String ignoreConfig  = "<font color=\"#FFDD55\">"; 
    public static final String ignoreDefault = "<font color=\"#FFDDBB\">"; 
    // normal is black
    public static final String normalMeas    = "<font color=\"#000000\">";
    public static final String normalConfig  = "<font color=\"#888888\">";
    public static final String normalDefault = "<font color=\"#cccccc\">";
    // highlight is Purple
    public static final String highlightMeas    = "<font color=\"#cc00cc\">";
    public static final String highlightConfig  = "<font color=\"#e077e0\">";
    public static final String highlightDefault = "<font color=\"#ffaaff\">";

    private static final String unknown= "<font color=\"#e7e7e7\">";

    private static final String endColor = "</font>";
    private static final DecimalFormat f2_1 = new DecimalFormat("0.0");

    /* Metric values above or equal to highlight-threshold will be
     * highlighted. Values below the threshold will be normal, Also,
     * Ignore a specific value.
     */
    public static String valueColor(Metric metric, 
				  double ignore, 
				  double highlight) {
	double value=metric.doubleValue();
	double cred=metric.getCredibility();
	if (cred == 0) return unknown;
	else if (cred <=  DEFAULT_CREDIBILITY){
	    if(value == ignore) return ignoreDefault;
	    if(value >=highlight) return highlightDefault;
	    return normalDefault;
	}
	else if (cred <=  SYS_DEFAULT_CREDIBILITY){
	    if(value == ignore) return ignoreConfig;
	    if(value >=highlight) return highlightConfig;
	    return normalConfig;
	}
	else{ // measured
	    if(value == ignore) return ignoreMeas;
	    if(value >=highlight) return highlightMeas;
	    return normalMeas;
	}
    }
    public static String valueTable(Metric metric, 
				    double ignore, 
				    double highlight,
				    DecimalFormat formatter) {
	return "<td>" + 
	    valueColor(metric,ignore,highlight) +
	    formatter.format(metric.doubleValue()) +
	    endColor +
	    "</td>";
    }

    
    /* We expect high credibility, so we highlight lower values 
     *
     */
    public static String credColor(Metric metric){
	double cred=metric.getCredibility();
	if (cred == 0) return highlightMeas;
	if (cred <=  DEFAULT_CREDIBILITY) return highlightConfig;
	if (cred <=  SYS_DEFAULT_CREDIBILITY) return normalMeas;
	return ignoreMeas;
    }

    public static String credTable(Metric metric){
	return "<td>" + 
	    credColor(metric) +
	    f2_1.format(metric.getCredibility()) +
	    endColor +
	    "</td>";
    }

    public static void colorTest(PrintWriter out) {
	Metric metric;

	out.print("<table border=1>\n <tr>");

	out.print("<td><b>VALUE \\ CRED</b></td>");
	out.print("<td><b>Default</b></td>");
	out.print("<td><b>Config</b></td>");
	out.print("<td><b>Measured</b></td>");
	out.print("</tr>");
	
	// row "ignore"
	out.print("<tr><td><b>Ignore</b></td>");

	metric = new MetricImpl(new Double(0.00), DEFAULT_CREDIBILITY,
				"units","test");
	out.print(valueTable(metric,0.0,1.0,f2_1));

	metric = new MetricImpl(new Double(0.00),SYS_DEFAULT_CREDIBILITY,
				"units","test");
	out.print(valueTable(metric,0.0,1.0,f2_1));

	metric = new MetricImpl(new Double(0.00),SECOND_MEAS_CREDIBILITY,
				"units","test");
	out.print(valueTable(metric,0.0,1.0,f2_1));
	out.print("</tr>");

	// row "Normal"
	out.print("<tr><td><b>Normal</b></td>");

	metric = new MetricImpl(new Double(0.50), DEFAULT_CREDIBILITY,
				"units","test");
	out.print(valueTable(metric,0.0,1.0,f2_1));

	metric = new MetricImpl(new Double(0.50),SYS_DEFAULT_CREDIBILITY,
				"units","test");
	out.print(valueTable(metric,0.0,1.0,f2_1));

	metric = new MetricImpl(new Double(0.50),SECOND_MEAS_CREDIBILITY,
				"units","test");
	out.print(valueTable(metric,0.0,1.0,f2_1));
	out.print("</tr>");

	// row "highlight"
	out.print("<tr><td><b>Highlight</b></td>");

	metric = new MetricImpl(new Double(1.00), DEFAULT_CREDIBILITY,
				"units","test");
	out.print(valueTable(metric,0.0,1.0,f2_1));

	metric = new MetricImpl(new Double(1.00),SYS_DEFAULT_CREDIBILITY,
				"units","test");
	out.print(valueTable(metric,0.0,1.0,f2_1));

	metric = new MetricImpl(new Double(1.00),SECOND_MEAS_CREDIBILITY,
				"units","test");
	out.print(valueTable(metric,0.0,1.0,f2_1));
	out.print("</tr>");
	
	out.print("</table>");
    }

}

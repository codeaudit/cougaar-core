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



public class DeltaValueQualifier 
    implements MetricNotificationQualifier, Constants
{
    private double min_delta;
    private Metric last_qualified;

    public DeltaValueQualifier(double min_delta) {
	this.min_delta = min_delta;
    }

    public boolean shouldNotify(Metric metric) {
	if (metric.getCredibility() <= SYS_DEFAULT_CREDIBILITY)
	    return false;

	if (last_qualified == null) {
	    last_qualified = metric;
	    return true;
	}

	double old_value = last_qualified.doubleValue();
	double new_value = metric.doubleValue();
	if (Math.abs(new_value-old_value) > min_delta) {
	    last_qualified = metric;
	    return true;
	} else {
	    return false;
	}
	
    }

}


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
import java.util.Properties;
import java.util.Observer;

/* Null Metric Sevice that returns only Undefined Metrics
 */
public class NullMetricsServiceImpl
    implements MetricsService,DataFeedRegistrationService
{
    public Metric getValue(String path) {
	return MetricImpl.UndefinedMetric;
    }

    public Metric getValue(String path, Properties qos_tags) {
	return MetricImpl.UndefinedMetric;
    }

    public Object subscribeToValue(String path, 
				   Observer observer) 
    {
	return null;
    }

    public Object subscribeToValue(String path, 
				   Observer observer,
				   MetricNotificationQualifier qualifier) 
    {
	return null;
    }

    public void unsubscribeToValue(Object subscription_handle)
    {
    }

    public boolean registerFeed(Object feed, String name) {
	return false;
    }

    public void populateSites(String sitesURLString) {
    }

		    
};

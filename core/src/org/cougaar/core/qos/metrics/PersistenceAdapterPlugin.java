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


import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.PersistenceMetricsService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;

// Make PersistenceMetrics look like (real) Metrics
public class PersistenceAdapterPlugin
    extends ComponentPlugin
    implements Runnable, Constants
{
    private PersistenceMetricsService  pms;
    private LoggingService loggingService;
    private MetricsUpdateService mus;
    private Schedulable schedulable;
    private String key;

    public void load() {
	super.load();
	
	ServiceBroker sb = getServiceBroker();
	
	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);

	pms = (PersistenceMetricsService)
	    sb.getService(this, PersistenceMetricsService.class, null);
	if (pms == null) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Couldn't get PersistenceMetricsService");
	    return;
	} 

	mus = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	if (mus == null) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Couldn't get MetricsUpdateService");
	    return;
	} 

	ThreadService tsvc = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	if (tsvc == null) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Couldn't get ThreadService");
	    return;
	} 


	key = "Agent" +KEY_SEPR+ getAgentIdentifier() +KEY_SEPR+
	    PERSIST_SIZE_LAST;

	schedulable = tsvc.getThread(this, this, "PersistenceAdapter");
	schedulable.schedule(0, 10000);
	
	sb.releaseService(this, ThreadService.class, tsvc);
			       

    }
	

    // Runnable
    public void run() {
	PersistenceMetricsService.Metric[] metrics =
	    pms.getAll(PersistenceMetricsService.FULL);
	long maxSize = 0;
	for (int i = 0, n = metrics.length; i < n; i++) {
	    maxSize = Math.max(maxSize, metrics[i].getSize());
	}

	if (maxSize > 0) {
	    Metric metric = new MetricImpl(maxSize, 
					   SECOND_MEAS_CREDIBILITY,
					   "bytes",
					   "PersistenceMetricsService");
	    mus.updateValue(key, metric);
	    if (loggingService.isDebugEnabled())
		loggingService.debug("Updating " +key+ " to " +metric);
	} else {
	    if (loggingService.isDebugEnabled())
		loggingService.debug(key + " is still 0 ");
	}
    }

    // Plugin methods
    protected void setupSubscriptions() {
	// None in this example
    }

    public void execute() {
	// Not relevant in this example
    }


}


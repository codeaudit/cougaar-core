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

package org.cougaar.core.thread;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.qos.metrics.Constants;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

public class LoadWatcher
    implements ThreadListener, Constants
{
    private static final String UNITS = "cpusec/sec";
    private static final double CREDIBILITY = SECOND_MEAS_CREDIBILITY;

    private class ConsumerRecord {
	String name;
	String key;
	int outstanding;
	long timestamp;
	double accumulator;
	long snapshot_timestamp;
	double snapshot_accumulator;
	double rate;

	ConsumerRecord(String name) {
	    this.name = extractObjectName(name);
	    key = "Agent" +KEY_SEPR+ this.name
		+KEY_SEPR+ "OneSecondLoadAvg";
	}


	private String extractObjectName(String rawName) {
	    if (rawName.startsWith("Node")) {
		return rawName.replace(' ', '_');
	    } else if (rawName.startsWith("Agent")){
		// We assume 'Agent <Cluster name>'
		int start = rawName.lastIndexOf(' ');
		int end = rawName.length();
		return rawName.substring(start+1, end-1);
	    } else {
		return rawName;
	    }
	}	
	
	synchronized void snapshot() {
	    long now = System.currentTimeMillis();
	    rate = (accumulator-snapshot_accumulator)/
		(now-snapshot_timestamp);
	    snapshot_timestamp = now;
	    snapshot_accumulator = accumulator;
	    // System.out.println(name+ " rate=" +rate);
 	    Metric metric = new MetricImpl(new Double(rate),
					   CREDIBILITY,
					   UNITS,
					   "LoadWatcher");
 	    metricsUpdateService.updateValue(key, THREAD_SENSOR, metric);
	}


	synchronized void accumulate() {
	    long now = System.currentTimeMillis();
	    if (timestamp > 0) {
		double deltaT = now - timestamp;
		accumulator += deltaT * outstanding;
	    } 
	    timestamp = now;
	}
    }

    private class SnapShotter extends TimerTask {
	public void run() {
	    synchronized (records) {
		Iterator itr = records.values().iterator();
		while (itr.hasNext()) {
		    ConsumerRecord rec = (ConsumerRecord) itr.next();
		    rec.snapshot();
		}
	    }
	}
    }

    private class Checker extends TimerTask {
	public void run() {
	    synchronized (records) {
		Iterator itr = records.values().iterator();
		while (itr.hasNext()) {
		    ConsumerRecord rec = (ConsumerRecord) itr.next();
 		    String path = "Agent(" +rec.name+ ")"
			+PATH_SEPR+ ONE_SEC_LOAD_AVG;
		    Metric metric = metricsService.getValue(path);
		    System.out.println(rec.name+ "=" +metric);
		}
	    }
	}
    }

    private HashMap records = new HashMap();
    private int total = 0;
    private MetricsUpdateService metricsUpdateService;
    private MetricsService metricsService;


    public LoadWatcher(ServiceBroker sb) {
	metricsUpdateService = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	metricsService = (MetricsService)
	    sb.getService(this, MetricsService.class, null);
	ThreadListenerService tls = (ThreadListenerService)
	    sb.getService(this, ThreadListenerService.class, null);
	tls.addListener(this);
	ThreadService ts = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	ts.schedule(new SnapShotter(), 5000, 1000);
	ts.schedule(new Checker(), 7500, 2000);
    }

    ConsumerRecord findRecord(String name) {
	ConsumerRecord rec = null;
	synchronized (records) {
	    rec = (ConsumerRecord) records.get(name);
	    if (rec == null) {
		rec = new ConsumerRecord(name);
		records.put(name, rec);
	    }
	}
	return rec;
    }


    public void threadQueued(Schedulable schedulable, 
			     Object consumer) 
    {
    }
    public void threadDequeued(Schedulable schedulable, 
			       Object consumer)
    {
    }
    public void threadStarted(Schedulable schedulable, 
			      Object consumer)
    {
    }
    public void threadStopped(Schedulable schedulable, 
			      Object consumer)
    {
    }

    public void rightGiven(String consumer) {
	++total;
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	++rec.outstanding;
    }
		
    public void rightReturned(String consumer) {
	--total; 
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	--rec.outstanding;
   }





}

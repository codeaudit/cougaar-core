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

    private int total;

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
	    accumulate();
	    rate = (accumulator-snapshot_accumulator)/
		(timestamp-snapshot_timestamp);
	    snapshot_timestamp = timestamp;
	    snapshot_accumulator = accumulator;
	    // System.out.println(name+ " rate=" +rate);
 	    Metric metric = new MetricImpl(new Double(rate),
					   CREDIBILITY,
					   UNITS,
					   "LoadWatcher");
 	    metricsUpdateService.updateValue(key, metric);
	}


	synchronized void incrementOutstanding() {
	    accumulate();
	    ++outstanding;
	    if (outstanding >10) 
		System.err.println("*** Agent outstanding =" +total+ 
				   " when rights given for " +name);

	}

	synchronized void decrementOutstanding() {
	    accumulate();
	    --outstanding;
	    if (outstanding < 0) 
		System.err.println("*** Agent outstanding =" +total+ 
				   " when rights returned for " +name);
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


    private HashMap records = new HashMap();
    private MetricsUpdateService metricsUpdateService;


    public LoadWatcher(ServiceBroker sb) {
	metricsUpdateService = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	ThreadListenerService tls = (ThreadListenerService)
	    sb.getService(this, ThreadListenerService.class, null);
	tls.addListener(this);
	ThreadService ts = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	ts.schedule(new SnapShotter(), 5000, 1000);
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

    public synchronized void rightGiven(String consumer) {
	ConsumerRecord rec = findRecord(consumer);
	rec.incrementOutstanding();
	++total;
	if (total >10)
	    System.err.println("*** total outstanding =" +total+ 
			       " when rights given for " +consumer);
    }
		
    public synchronized void rightReturned(String consumer) {
	ConsumerRecord rec = findRecord(consumer);
	rec.decrementOutstanding();
	--total;
	if (total < 0) 
	    System.err.println("*** total outstanding =" +total+ 
			       " when rights returnd for " +consumer);
   }





}

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

public class SchedulerWatcher
    implements ThreadListener, Constants
{
    private static final double CREDIBILITY = SECOND_MEAS_CREDIBILITY;
    private static final String PROVENANCE = "SchedulerWatcher";

    private long totalLoad;
    private long loadTimestamp;
    private double sumTotalLoad;
    private String agentName;

    private class ConsumerRecord {
	// Static
	Object consumer;
	String name;
	String prefix;
	//instantanous
	int outstanding;  
	int pending;
	//Accumalators
	long accumalate_timestamp;
	long count;    // accumalator counts 
	long queued;   // exit counts
	long ran;      // exit counts
	// Integrators
	double sumPending;
	double sumOutstanding;
	//Rate Snapshots
	long  snapshot_timestamp;
	double snapshot_sumOutstanding;
	double snapshot_sumPending;
	double snapshot_count;
	double snapshot_ran;
	double snapshot_queued;
	//Rates
	double utilization;
	double runs_per_sec;
	double avg_cpu_per_run;
	double avg_latency_per_run;
	double avg_wait_per_run;

	ConsumerRecord(Object consumer) {
	    // System.err.println("%%%% new ConsumerRecord for " +consumer);
	    this.consumer = consumer;
	    this.name = consumer.toString();
	    this.prefix = "Agent" +KEY_SEPR+ agentName
		+KEY_SEPR+ "Plugin"+this.name +KEY_SEPR;
	}


	
	synchronized void snapshot() {
	    // Calculate Deltas
	    long now = System.currentTimeMillis();
	    double deltaSumOutstanding = (sumOutstanding 
					  - snapshot_sumOutstanding);
	    double deltaSumPending = (sumPending - snapshot_sumPending);
	    double deltaRuns = (ran - snapshot_ran);
	    double deltaQueued = (queued - snapshot_queued);
	    double deltaTime = (now - snapshot_timestamp);
	    double deltaLatency = deltaSumPending + deltaSumOutstanding;


	    // Calculate Rates
	    utilization = deltaSumOutstanding/deltaTime;
	    runs_per_sec = 1000 *( deltaRuns/deltaTime);

	    avg_latency_per_run = 0;
	    avg_cpu_per_run = 0;
	    avg_wait_per_run = 0;
	    if(deltaRuns > 0) {
		avg_latency_per_run = deltaLatency/deltaRuns;
		avg_cpu_per_run = deltaSumOutstanding/deltaRuns;
	    }
	    if(deltaQueued > 0) {
		avg_wait_per_run = deltaSumPending/deltaQueued;
	    }

	    // Save SnapShot
	    snapshot_timestamp = now;
	    snapshot_sumOutstanding = sumOutstanding;
	    snapshot_sumPending = sumPending;
	    snapshot_count = count;
	    snapshot_ran = ran;
	    snapshot_queued = queued;

	    sendData(utilization, "utilization");
	    sendData(runs_per_sec, "runspersec");
	    sendData(avg_cpu_per_run, "avgcpuperrun");
	    sendData(avg_latency_per_run, "avglatencyperrun");
	    sendData(avg_wait_per_run, "avgwaitperrun");
	}

	private void sendData(double value, String tag) {
 	    Metric metric = new MetricImpl(new Double(value),
					   CREDIBILITY,
					   "",
					   PROVENANCE);
// 	    metricsUpdateService.updateValue(prefix+tag, PROVENANCE, metric);
 	    metricsUpdateService.updateValue(prefix+tag, metric);
	}


	synchronized void accumulate() {
	    ++count;
	    long now = System.currentTimeMillis();
	    if (accumalate_timestamp > 0) {
		double deltaT = now - accumalate_timestamp;
		sumOutstanding += deltaT * outstanding;
		sumPending += deltaT * pending;
	    } 
	    accumalate_timestamp = now;
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


    public SchedulerWatcher(ServiceBroker sb, String agent) {
	agentName = agent;
	metricsUpdateService = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	ThreadListenerService tls = (ThreadListenerService)
	    sb.getService(this, ThreadListenerService.class, null);
	tls.addListener(this);
	ThreadService ts = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	ts.schedule(new SnapShotter(), 5000, 1000);
    }

    ConsumerRecord findRecord(Object consumer) {
	ConsumerRecord rec = null;
	synchronized (records) {
	    rec = (ConsumerRecord) records.get(consumer);
	    if (rec == null) {
		rec = new ConsumerRecord(consumer);
		records.put(consumer, rec);
	    }
	}
	return rec;
    }


    public void threadQueued(Schedulable schedulable, 
			     Object consumer) 
    {
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	++rec.pending;
    }

    public void threadDequeued(Schedulable schedulable, 
			       Object consumer)
    {
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	--rec.pending;
	++rec.queued;
    }

    public void threadStarted(Schedulable schedulable, 
			      Object consumer)
    {
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	++rec.outstanding;
    }

    public void threadStopped(Schedulable schedulable, 
			      Object consumer)
    {
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	--rec.outstanding;
	++rec.ran;
    }

    public void rightGiven(String consumer) {
    }
		
    public void rightReturned(String consumer) {
   }





}

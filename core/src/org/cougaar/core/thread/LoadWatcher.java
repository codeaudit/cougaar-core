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
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.qos.metrics.DecayingHistory;
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

    private class ConsumerSnapShot extends DecayingHistory.SnapShot {
	double loadAvgIntegrator;
	double loadMjipsIntegrator;
	
	ConsumerSnapShot(double loadAvgIntegrator,
			 double loadMjipsIntegrator)
	{
	    this.loadAvgIntegrator = loadAvgIntegrator;
	    this.loadMjipsIntegrator = loadMjipsIntegrator;
	}

    }

    private class ConsumerRecord extends DecayingHistory {
	String name;
	String key;
	int outstanding;
	long timestamp;
	double loadAvgIntegrator;
	double loadMjipsIntegrator;

	ConsumerRecord(String name) {
	    super(10, 3);
	    this.name = extractObjectName(name);
	    if (this.name.startsWith("Service"))
		key = this.name +KEY_SEPR+ "CPULoad";
	    else
		key = "Agent" +KEY_SEPR+ this.name +KEY_SEPR+ "CPULoad";
	}


	void addToHistory() {
	    ConsumerSnapShot snap = null;
	    synchronized (this) {
		accumulate();
		snap = new ConsumerSnapShot(loadAvgIntegrator,
					    loadMjipsIntegrator);
	    }
	    add(snap);
	}

	public void newAddition(String period, 
				DecayingHistory.SnapShot now_raw,
				DecayingHistory.SnapShot last_raw) 
	{
	    ConsumerSnapShot now = (ConsumerSnapShot) now_raw;
	    ConsumerSnapShot last = (ConsumerSnapShot) last_raw;
	    double deltaT = now.timestamp -last.timestamp;
	    double deltaLoad = now.loadAvgIntegrator-last.loadAvgIntegrator;
	    double deltaMJips = 
		now.loadMjipsIntegrator-last.loadMjipsIntegrator;

	    String lavgKey = key + "Avg" + period;
 	    Metric lavg = new MetricImpl(new Double( deltaLoad/deltaT),
					 CREDIBILITY,
					 "threads/sec",
					 "LoadWatcher");
 	    metricsUpdateService.updateValue(lavgKey, lavg);

	    String mjipsKey = key + "Jips" + period;
 	    Metric mjips = new MetricImpl(new Double( deltaMJips/deltaT),
					  CREDIBILITY,
					  "mjips/sec",
					  "LoadWatcher");
 	    metricsUpdateService.updateValue(mjipsKey, mjips);
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
		return "Service" +KEY_SEPR+ rawName;
	    }
	}	
	
	synchronized void incrementOutstanding() {
	    accumulate();
	    ++outstanding;
	    // Can't get the specific max, so no applicable test
	}

	synchronized boolean decrementOutstanding() {
	    // The given consumer Scheduler may have running threads when
	    // this listener starts listening.  When those threads stop,
	    // the count will go negative.  Ignore those.
	    if (outstanding == 0) return false;

	    accumulate();
	    --outstanding;
	    if (outstanding < 0) 
		loggingService.debug("Agent outstanding =" +outstanding+ 
				   " when rights returned for " +name);
	    return true;
	}

	synchronized void accumulate() {
	    long now = System.currentTimeMillis();
	    if (timestamp > 0) {
		double deltaT = now - timestamp;
		loadMjipsIntegrator += deltaT* outstanding *effectiveMJIPS();
		loadAvgIntegrator += deltaT * outstanding;
	    } 
	    timestamp = now;
	}
    }

    private class HistoryPoller extends TimerTask {
	public void run() {
	    synchronized (records) {
		Iterator itr = records.values().iterator();
		while (itr.hasNext()) {
		    ConsumerRecord rec = (ConsumerRecord) itr.next();
		    rec.addToHistory();
		}
	    }
	}
    }



    private int total;
    private HashMap records = new HashMap();

    private MetricsUpdateService metricsUpdateService;
    private MetricsService metricsService;
    private LoggingService loggingService;
    private ThreadControlService controlService;
    private int number_of_cpus;
    private double capacity_mjips;


    public LoadWatcher(ServiceBroker sb) {
	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	metricsUpdateService = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	metricsService = (MetricsService)
	    sb.getService(this, MetricsService.class, null);

	controlService = (ThreadControlService)
	    sb.getService(this, ThreadControlService.class, null);



	NodeIdentificationService nis = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
	MessageAddress my_node = nis.getNodeIdentifier();

	String path = "Node(" +my_node+ ")" +PATH_SEPR+ "Jips";
	Metric m = metricsService.getValue(path);
	capacity_mjips = m.doubleValue()/1000000.0;

	path = "Node(" +my_node+ ")" +PATH_SEPR+ "Count";
	m = metricsService.getValue(path);
	number_of_cpus = m.intValue();


	// Don't start listening for callbacks until everything's
	// ready.
	ThreadListenerService tls = (ThreadListenerService)
	    sb.getService(this, ThreadListenerService.class, null);
	tls.addListener(this);

	ThreadService ts = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	ts.schedule(new HistoryPoller(), 5000, 1000);
    }


    private double effectiveMJIPS() {
	return capacity_mjips / Math.max(1,(total-number_of_cpus));
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
	if (total > controlService.maxRunningThreadCount())
	    loggingService.debug("total outstanding =" +total+ 
			       " when rights given for " +consumer);
    }
		
    public synchronized void rightReturned(String consumer) {
	ConsumerRecord rec = findRecord(consumer);

	// The given consumer Scheduler may have running threads when
	// this listener starts listening.  When those threads stop,
	// the count will go negative.  Ignore those.
	if (rec.decrementOutstanding()) --total;

	// Sanity check
	if (total < 0) 
	    loggingService.debug("total outstanding =" +total+ 
				 " when rights returnd for " +consumer);
   }





}

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

package org.cougaar.core.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricsService;

public class AgentLoadSensorPlugin
    extends ComponentPlugin
    implements ThreadListener, Constants, ServiceProvider
{
    private class ServiceImpl implements AgentLoadService {
	public Collection snapshotRecords() {
	    return snapshot();
	}
    }

    private class ConsumerRecord extends AgentLoadService.AgentLoad {

	ConsumerRecord(String name) {
	    this.name = extractObjectName(name);
	}

	private String extractObjectName(String rawName) {
	    if (rawName.startsWith("Node")) {
		return rawName.replace(' ', '-');
	    } else if (rawName.startsWith("Agent")){
		// We assume 'Agent_AgentName'
		return rawName.substring(6);
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

	synchronized AgentLoadService.AgentLoad snapshot() {
	    accumulate();
	    AgentLoadService.AgentLoad result = 
		new AgentLoadService.AgentLoad();
	    result.name = name;
	    result.outstanding = outstanding;
	    result.loadAvgIntegrator = loadAvgIntegrator;
	    result.loadMjipsIntegrator = loadMjipsIntegrator;
	    return result;
	}
    }



    private static int number_of_cpus;
    private static double capacity_mjips;

    private int total;
    private HashMap records = new HashMap();

    private LoggingService loggingService;
    private AgentLoadService serviceImpl;

    public AgentLoadSensorPlugin() {
    }

    // Component
    public void load() {
	super.load();
	
	ServiceBroker sb = getServiceBroker();

	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);

	NodeIdentificationService nis = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
 	MessageAddress my_node = nis.getMessageAddress();
	sb.releaseService(this, NodeIdentificationService.class, nis);

	MetricsService metricsService = (MetricsService)
	    sb.getService(this, MetricsService.class, null);

	String path = "Node(" +my_node+ ")" +PATH_SEPR+ "Jips";
	Observer mjips_obs = new Observer() {
		public void update (Observable observable, Object value) {
		    Metric m = (Metric) value;
		    capacity_mjips = m.doubleValue()/1000000.0;
		}
	    };
	metricsService.subscribeToValue(path,  mjips_obs);


	path = "Node(" +my_node+ ")" +PATH_SEPR+ "Count";
	Observer cpu_obs = new Observer() {
		public void update (Observable observable, Object value) {
		    Metric m = (Metric) value;
		    // force to be at least 1 cpu
		    number_of_cpus =  Math.max(1,m.intValue());
		}
	    };
	metricsService.subscribeToValue(path, cpu_obs);


	sb.releaseService(this, MetricsService.class, metricsService);

	serviceImpl = new ServiceImpl();
	// We provide AgentLoadService
	sb.addService(AgentLoadService.class, this);


	// We need the root ServiceBroker's ThreadListenerService
	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);

	if (ncs != null) {
	    ServiceBroker rootsb = ncs.getRootServiceBroker();
	    sb.releaseService(this, NodeControlService.class, ncs);

	    ThreadListenerService tls = (ThreadListenerService)
		rootsb.getService(this, ThreadListenerService.class, null);
	    tls.addListener(this);
	    rootsb.releaseService(this, ThreadListenerService.class, tls);
	} else {
	    throw new RuntimeException("AgentLoadSensor can only be used in NodeAgents");
	}

    }


    // Plugin
    protected void setupSubscriptions() {
    }
  
    protected void execute() {
    }


    // ServiceProvider
    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == AgentLoadService.class) {
	    return serviceImpl;
	} else {
	    return null;
	}
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }


    // Local utility methods
    private double effectiveMJIPS() {
	return capacity_mjips / Math.max(1,(total/number_of_cpus));
    }


    private ArrayList snapshot() {
	ArrayList result = new ArrayList();
	synchronized (records) {
	    Iterator itr = records.values().iterator();
	    while (itr.hasNext()) {
		ConsumerRecord record = (ConsumerRecord) itr.next();
		result.add(record.snapshot());
	    }
	}
	return result;
    }

    private ConsumerRecord findRecord(String name) {
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



    // ThreadListener
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
    }
		
    public synchronized void rightReturned(String consumer) {
	ConsumerRecord rec = findRecord(consumer);

	// The given consumer Scheduler may have running threads when
	// this listener starts listening.  When those threads stop,
	// the count will go negative.  Ignore those.
	if (rec.decrementOutstanding()) --total;
   }





}

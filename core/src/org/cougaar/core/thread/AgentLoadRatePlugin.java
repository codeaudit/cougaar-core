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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.DecayingHistory;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.service.ThreadService;

/* Load this Plugin at LOW priority since it needs another plugin's service
 */
public class AgentLoadRatePlugin
    extends ComponentPlugin
    implements Runnable, Constants
{
    private static final int BASE_PERIOD = 10; //10SecAVG

    private class AgentLoadHistory extends DecayingHistory {
	private static final double CREDIBILITY = SECOND_MEAS_CREDIBILITY;

	String agentKey;
	String mjipsKey;
	String loadavgKey;

	AgentLoadHistory(String name) {
	    super(10, 3, BASE_PERIOD);
	    if (name.startsWith("Service")) 	       
		agentKey = name;
	    else
		agentKey = "Agent" +KEY_SEPR+ name ;
	    mjipsKey=(agentKey +KEY_SEPR+ CPU_LOAD_MJIPS).intern();
	    addKey(mjipsKey);
	    loadavgKey=(agentKey +KEY_SEPR+ CPU_LOAD_AVG).intern();
	    addKey(loadavgKey);
	}

	public void newAddition(KeyMap keys, 
				DecayingHistory.SnapShot now_raw,
				DecayingHistory.SnapShot last_raw) 
	{
	    AgentLoadService.AgentLoad now = (AgentLoadService.AgentLoad) 
		now_raw;
	    AgentLoadService.AgentLoad last = (AgentLoadService.AgentLoad)
		last_raw;
	    double deltaT = now.timestamp -last.timestamp;
	    double deltaLoad = now.loadAvgIntegrator-last.loadAvgIntegrator;
	    double deltaMJips = 
		now.loadMjipsIntegrator-last.loadMjipsIntegrator;

	    //Must match the Metrics Constants for CPU_LOAD_AVG_1XXX_SEC_AVG
	    String lKey = keys.getKey(loadavgKey);
 	    Metric lData = new MetricImpl(new Double( deltaLoad/deltaT),
					 CREDIBILITY,
					 "threads/sec",
					 "AgentLoadSensor");
 	    metricsUpdateService.updateValue(lKey, lData);

	    //Must match the Metrics Constants for CPU_LOAD_MJIPS_1XXX_SEC_AVG
	    String mKey = keys.getKey(mjipsKey);
 	    Metric mData = new MetricImpl(new Double( deltaMJips/deltaT),
					  CREDIBILITY,
					  "mjips",
					  "AgentLoadSensor");
 	    metricsUpdateService.updateValue(mKey, mData);
	}
	
    }

    private AgentLoadService agentLoadService;
    private MetricsUpdateService metricsUpdateService;
    private Schedulable schedulable;
    private HashMap histories;

    public AgentLoadRatePlugin() {
	histories = new HashMap();
    }

    // Local
    AgentLoadHistory findOrMakeHistory(String agent) {
	AgentLoadHistory history = (AgentLoadHistory) histories.get(agent);
	if (history == null) {
	    history = new AgentLoadHistory(agent);
	    histories.put(agent, history);
	}
	return history;
    }

    // Component
    public void load() {
	super.load();
	
	ServiceBroker sb = getServiceBroker();
	agentLoadService = (AgentLoadService)
	    sb.getService(this, AgentLoadService.class, null);
	if (agentLoadService == null) {
	    throw new RuntimeException("Can't find AgentLoadService. This plugin must be loaded at Low priority");
	}

	metricsUpdateService = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);

	ThreadService tsvc = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	schedulable = tsvc.getThread(this, this, "AgentLoadRate");
	schedulable.schedule(5000, BASE_PERIOD *1000);
	sb.releaseService(this, ThreadService.class, tsvc);
    }


    // Schedulable body
    public void run() {
	Collection agentLoadSnapshot = agentLoadService.snapshotRecords();
        boolean useItr;
        Iterator itr;
        List l;
        if (agentLoadSnapshot instanceof List &&
            agentLoadSnapshot instanceof RandomAccess) {
          useItr = false;
          itr = null;
          l = (List) agentLoadSnapshot;
        } else {
          useItr = true;
          itr = agentLoadSnapshot.iterator();
          l = null;
        }
	for (int i = 0, n = agentLoadSnapshot.size(); i < n; i++) {
	    AgentLoadService.AgentLoad record = (AgentLoadService.AgentLoad)
		(useItr ? itr.next() : l.get(i));
	    String agent = record.name;
	    AgentLoadHistory history = findOrMakeHistory(agent);
	    history.add(record);
	}
    }

    // Plugin
    protected void setupSubscriptions() {
    }
  
    protected void execute() {
    }


}

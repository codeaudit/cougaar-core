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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.TrafficMatrixStatisticsService;
import org.cougaar.core.mts.TrafficMatrix;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.service.LoggingService;

/* Load this Plugin at LOW priority since it needs another plugin's service
 */
public class AgentFlowRatePlugin
  extends ComponentPlugin
  implements Runnable, Constants
{
    private static final int BASE_PERIOD = 10; //10SecAVG
    private static final int COLUMNS=3;
    private LoggingService logging;

    private class AgentFlowHistory extends DecayingHistory {
	private static final double CREDIBILITY = SECOND_MEAS_CREDIBILITY;
	String msgKey;
	String byteKey;
	MessageAddress orig = null;
	MessageAddress target = null;
    
	AgentFlowHistory(MessageAddress orig, MessageAddress target) {
	    super(10, 3, BASE_PERIOD);
	    this.orig = orig;
	    this.target = target;
	    String flowKey=
		"AgentFlow" +KEY_SEPR+ orig +KEY_SEPR+ target +KEY_SEPR;
	    msgKey=(flowKey+ MSG_RATE).intern();
	    addKey(msgKey);
	    byteKey=(flowKey+ BYTE_RATE).intern() ;
	    addKey(byteKey);
	}
    
	// done on records, not the whole matrix
	public void newAddition(KeyMap keys,
				DecayingHistory.SnapShot now_raw,
				DecayingHistory.SnapShot last_raw) 
	{
	    TrafficMatrix.TrafficRecord now = (TrafficMatrix.TrafficRecord) 
		now_raw;
	    TrafficMatrix.TrafficRecord last = (TrafficMatrix.TrafficRecord)
		last_raw;
	    double deltaT = (now.timestamp -last.timestamp) / 1000.0;
	    double deltaMsgs = now.msgCount -last.msgCount;
	    double deltaBytes = now.byteCount -last.byteCount;
      
	    String msgAvgKey =keys.getKey(msgKey);
	    String byteAvgKey =keys.getKey( byteKey);
      
	    Metric msgAvg = new MetricImpl(new Double( deltaMsgs/deltaT),
					   CREDIBILITY,
					   "msg/sec",
					   "AgentFlowRate");
	    metricsUpdateService.updateValue(msgAvgKey, msgAvg);
      
	    Metric byteAvg = new MetricImpl(new Double( deltaBytes/deltaT),
					    CREDIBILITY,
					    "bytes/sec",
					    "AgentFlowRate");
	    metricsUpdateService.updateValue(byteAvgKey, byteAvg);
	    if (logging.isDebugEnabled())
		logging.debug("key="+msgAvgKey+" Value="+msgAvg);
	}
    }

    private TrafficMatrixStatisticsService agentFlowService;
    private MetricsUpdateService metricsUpdateService;
    private Schedulable schedulable;
    private HashMap histories;
  
    public AgentFlowRatePlugin() {
	histories = new HashMap();
    }
  
    // Local
    AgentFlowHistory findOrMakeHistory(MessageAddress orig, MessageAddress target) {
	HashMap submap = (HashMap) histories.get(orig.getPrimary());
	if (submap == null) {
	    submap = new HashMap();
	    histories.put(orig.getPrimary(), submap);
	}
	AgentFlowHistory history = (AgentFlowHistory) submap.get(target.getPrimary());
	if (history == null) {
	    history = new AgentFlowHistory(orig, target);
	    submap.put(target.getPrimary(), history);
	}
	return history;
    }
  
    // Component
    public void load() {
	super.load();
    
	ServiceBroker sb = getServiceBroker();
	agentFlowService = (TrafficMatrixStatisticsService)
	    sb.getService(this, TrafficMatrixStatisticsService.class, null);
	if (agentFlowService == null) {
	    throw new RuntimeException("Can't find TrafficMatrixStatsisticsService. This plugin must be loaded at Low priority");
	}
    
	metricsUpdateService = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);

	logging = (LoggingService)
            sb.getService(this, LoggingService.class, null);

	ThreadService threadService = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	schedulable = threadService.getThread(this, this, "AgentFlowRatePlugin");
	schedulable.schedule(5000, BASE_PERIOD*1000);
	sb.releaseService(this, ThreadService.class, threadService);
    }
  
  
    // Schedulable body
    public void run() {
	int count =0;
	TrafficMatrix agentFlowSnapshot = agentFlowService.snapshotMatrix();
	TrafficMatrix.TrafficIterator itr = agentFlowSnapshot.getIterator();
	while (itr.hasNext()) {
	    count++;  
	    TrafficMatrix.TrafficRecord record = (TrafficMatrix.TrafficRecord)
		itr.next();
	    MessageAddress orig = itr.getOrig();
	    MessageAddress target = itr.getTarget();
	    AgentFlowHistory history = findOrMakeHistory(orig, target);
	    history.add(record);

	}
	if (logging.isDebugEnabled())
	    logging.debug("Processed Traffic Records="+count);
    }
    
    // Plugin
    protected void setupSubscriptions() {
    }
  
    protected void execute() {
    }
}

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

package org.cougaar.core.qos.metrics;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.service.ThreadService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;


public class AgentStatusRatePlugin 
    extends ComponentPlugin
    implements Constants
{
    private AgentStatusService agentStatusService;
    private MetricsUpdateService metricsUpdate;
    private HashMap agentHistories;

    public AgentStatusRatePlugin() {
	super();
	agentHistories = new HashMap();
    }


    private class SnapShot {
	long timestamp ;
	AgentStatusService.AgentState state ;
	SnapShot(AgentStatusService.AgentState state){
	    timestamp = System.currentTimeMillis();
	    this.state= state;
	}
    }


    private static final String[] Periods = 
    {  "10SecAvg","100SecAvg","1000SecAvg"};

    private class AgentHistory implements DecayingHistory.Callback {
	DecayingHistory history;
	MessageAddress agent;

	AgentHistory(MessageAddress address) {
	    this.agent = address;
	    history = new DecayingHistory(10, 3, this);
	    agentHistories.put(address, this);
	}

	public void newAdditionLast(Object nowRaw, Object lastRaw) {
	    handleNewAddition(agent, "1SecAvg",  nowRaw, lastRaw);
	}


	public void newAdditionHistory(int column, Object nowRaw,
				       Object lastRaw) {
	    handleNewAddition(agent, Periods[column], nowRaw, lastRaw);
	}
    }


    private synchronized AgentHistory getAgentHistory(MessageAddress agent) {
	AgentHistory history = (AgentHistory) agentHistories.get(agent);
	if (history != null)
	    return history;
	else
	    return new AgentHistory(agent);
    }


    private void updateMetric(MessageAddress agent,
			      String lable,
			      String period,
			      double value, 
			      String units)
    {
	String key = "Agent" +KEY_SEPR+ agent  +KEY_SEPR +lable + period;
	Metric metric = new MetricImpl(value,
				      SECOND_MEAS_CREDIBILITY,
				      "units",
				      "AgentStatusRatePlugin");
	metricsUpdate.updateValue(key, metric);
    }

  
    private void handleNewAddition(MessageAddress agent,
				   String period,
				   Object nowRaw, 
				   Object lastRaw) 
    {
	SnapShot now = (SnapShot) nowRaw;
	SnapShot last = (SnapShot) lastRaw;
	updateMetric(agent,"MsgIn",period, msgInRate(now,last),"msg/sec");
	updateMetric(agent,"MsgOut",period, msgOutRate(now,last),"msg/sec");
	updateMetric(agent,"BytesIn",period, bytesInRate(now,last),
		     "bytes/sec");
	updateMetric(agent,"BytesOut",period, bytesOutRate(now,last),
		     "bytes/sec");

    }
    

    private double deltaSec(SnapShot now, SnapShot last) {
	return (now.timestamp - last.timestamp)/1000.0;
    }

    private double msgInRate(SnapShot now, SnapShot last) {
	double deltaT=  deltaSec(now,last);
	if (deltaT > 0) {
	    return (now.state.receivedCount - last.state.receivedCount)/deltaT;
	}
	else return 0.0;
    }

    private double msgOutRate(SnapShot now, SnapShot last) {
	double deltaT=  deltaSec(now,last);
	if (deltaT > 0) {
	    return (now.state.deliveredCount - last.state.deliveredCount)
		/deltaT;
	}
	else return 0.0;
    }

    private double bytesOutRate(SnapShot now, SnapShot last) {
	double deltaT=  deltaSec(now,last);
	if (deltaT > 0) {
	    return (now.state.deliveredBytes - last.state.deliveredBytes)
		/deltaT;
	}
	else return 0.0;
    }

    private double bytesInRate(SnapShot now, SnapShot last) {
	double deltaT=  deltaSec(now,last);
	if (deltaT > 0) {
	    return (now.state.receivedBytes - last.state.receivedBytes)
		/deltaT;
	}
	else return 0.0;
    }



    protected  AgentStatusService.AgentState getState(MessageAddress agent)
    {
       	AgentStatusService.AgentState state = null;
	if (agentStatusService!=null) {
	    state = agentStatusService.getLocalAgentState(agent);
	}
	return state;
    }


    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();
	agentStatusService = (AgentStatusService)
	    sb.getService(this, AgentStatusService.class, null);

	metricsUpdate = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);

	//start a 1 second poller
	TimerTask poller = new TimerTask() { public void run() { poll(); } };
	ThreadService threadService = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	threadService.schedule(poller, 0, 1000);
    }

    private void poll() {
	Iterator itr = agentStatusService.getLocalAgents().iterator();
	while (itr.hasNext()) {
	    MessageAddress addr = (MessageAddress) itr.next();
	    AgentStatusService.AgentState state = getState(addr);
	    if (state != null) {
		SnapShot record = new SnapShot(state);
		getAgentHistory(addr).history.add(record);
	    }
	}
    }


    protected void setupSubscriptions() {
    }
  
    protected void execute() {
	//System.out.println("Executed AgentStatusRatePlugin");
    }

}

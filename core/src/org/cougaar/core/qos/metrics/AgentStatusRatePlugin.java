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
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.ThreadService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;


public class AgentStatusRatePlugin 
    extends ComponentPlugin
    implements Constants
{
    private static final int LOCAL = 0;
    private static final int REMOTE = 1;

    private AgentStatusService agentStatusService;
    private MetricsUpdateService metricsUpdate;
    private HashMap agentLocalHistories;
    private HashMap agentRemoteHistories;
    private MessageAddress nodeID;

    public AgentStatusRatePlugin() {
	super();
	agentLocalHistories = new HashMap();
	agentRemoteHistories = new HashMap();
    }


    private static class AgentSnapShot extends DecayingHistory.SnapShot {
	AgentStatusService.AgentState state;

	AgentSnapShot(AgentStatusService.AgentState state) {
	    super();
	    this.state = state;
	}
    }

    private abstract class AgentHistory extends DecayingHistory {
	MessageAddress agent;

	AgentHistory(MessageAddress address, HashMap store) {
	    super(10, 3);
	    this.agent = address;
	    store.put(address, this);
	}


	public void newAddition(String period, 
				DecayingHistory.SnapShot now,
				DecayingHistory.SnapShot last) 
	{
	    handleNewAddition(agent, period, 
			      (AgentSnapShot) now,
			      (AgentSnapShot) last);
	}

	
	abstract void handleNewAddition(MessageAddress agent,
					String period,
					AgentSnapShot now, 
					AgentSnapShot last);
    }

    private class AgentLocalHistory extends AgentHistory {

	AgentLocalHistory(MessageAddress address) {
	    super(address, agentLocalHistories);
	}

	void handleNewAddition(MessageAddress agent,
			       String period,
			       AgentSnapShot now, 
			       AgentSnapShot last) 
	{
	    updateAgentMetric(agent,"MsgIn",period, msgInRate(now,last),
			      "msg/sec");
	    updateAgentMetric(agent,"MsgOut",period, msgOutRate(now,last),
			      "msg/sec");
	    updateAgentMetric(agent,"BytesIn",period, bytesInRate(now,last),
			      "bytes/sec");
	    updateAgentMetric(agent,"BytesOut",period, bytesOutRate(now,last),
			      "bytes/sec");

	}
    }

    private class AgentRemoteHistory extends AgentHistory {
	AgentRemoteHistory(MessageAddress address) {
	    super(address, agentRemoteHistories);
	}

	void handleNewAddition(MessageAddress agent,
			       String period,
			       AgentSnapShot now, 
			       AgentSnapShot last) 
	{
	    updateFlowMetric(agent,"MsgFrom",period, msgInRate(now,last),
			     "msg/sec");
	    updateFlowMetric(agent,"MsgTo",period, msgOutRate(now,last),
			     "msg/sec");
	    updateFlowMetric(agent,"BytesFrom",period, bytesInRate(now,last),
			     "bytes/sec");
	    updateFlowMetric(agent,"BytesTo",period, bytesOutRate(now,last),
			     "bytes/sec");
	    // JAZ ADD QUEUE Metric
	}

    }


    private synchronized AgentHistory getAgentHistory(MessageAddress agent,
						      int kind) 
    {
	HashMap map = 
	    kind == LOCAL ? agentLocalHistories : agentRemoteHistories;
	AgentHistory history = (AgentHistory) map.get(agent);
	if (history != null)
	    return history;
	else if (kind == LOCAL)
	    return new AgentLocalHistory(agent);
	else
	    return new AgentRemoteHistory(agent);
    }



    private void updateAgentMetric(MessageAddress agent,
				   String label,
				   String period,
				   double value, 
				   String units)
    {
	String key = "Agent" +KEY_SEPR+ agent  +KEY_SEPR +label + period;
	Metric metric = new MetricImpl(value,
				       SECOND_MEAS_CREDIBILITY,
				       units,
				       "AgentStatusRatePlugin");
	metricsUpdate.updateValue(key, metric);
    }

  
    
    private void updateFlowMetric(MessageAddress agent,
				  String label,
				  String period,
				  double value, 
				  String units)
    {
	String key = "Node" +KEY_SEPR+ nodeID
	    +KEY_SEPR+ "Destination" +KEY_SEPR+
	    agent  +KEY_SEPR +label + period;
	
	Metric metric = new MetricImpl(value,
				       SECOND_MEAS_CREDIBILITY,
				       units,
				       "AgentStatusRatePlugin");
	metricsUpdate.updateValue(key, metric);

    }



    private double deltaSec(AgentSnapShot now, AgentSnapShot last) 
    {
	return (now.timestamp - last.timestamp)/1000.0;
    }

    private double msgInRate(AgentSnapShot now, AgentSnapShot last) 
    {
	double deltaT=  deltaSec(now,last);
	if (deltaT > 0) {
	    return (now.state.receivedCount - last.state.receivedCount)/deltaT;
	}
	else return 0.0;
    }

    private double msgOutRate(AgentSnapShot now, AgentSnapShot last) 
    {
	double deltaT=  deltaSec(now,last);
	if (deltaT > 0) {
	    return (now.state.deliveredCount - last.state.deliveredCount)
		/deltaT;
	}
	else return 0.0;
    }

    private double bytesOutRate(AgentSnapShot now, AgentSnapShot last) 
    {
	double deltaT=  deltaSec(now,last);
	if (deltaT > 0) {
	    return (now.state.deliveredBytes - last.state.deliveredBytes)
		/deltaT;
	}
	else return 0.0;
    }

    private double bytesInRate(AgentSnapShot now, AgentSnapShot last) 
    {
	double deltaT=  deltaSec(now,last);
	if (deltaT > 0) {
	    return (now.state.receivedBytes - last.state.receivedBytes)
		/deltaT;
	}
	else return 0.0;
    }



    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();
	agentStatusService = (AgentStatusService)
	    sb.getService(this, AgentStatusService.class, null);

	metricsUpdate = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);

	NodeIdentificationService nis = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
	nodeID = nis.getMessageAddress();

	// Start a 1 second poller, if the required services exist.
	if (agentStatusService != null && metricsUpdate != null) {
	    TimerTask poller = new TimerTask() { 
		    public void run() { 
			poll(); 
		    } };
	    ThreadService threadService = (ThreadService)
		sb.getService(this, ThreadService.class, null);
	    threadService.schedule(poller, 0, 1000);
	}
    }

    private void poll() {
	Iterator itr = agentStatusService.getLocalAgents().iterator();
	while (itr.hasNext()) {
	    MessageAddress addr = (MessageAddress) itr.next();
	    AgentStatusService.AgentState state = 
		agentStatusService.getLocalAgentState(addr);
	    if (state != null) {
		AgentSnapShot record = new AgentSnapShot(state);
		getAgentHistory(addr, LOCAL).add(record);
	    }
	}

	itr = agentStatusService.getRemoteAgents().iterator();
	while (itr.hasNext()) {
	    MessageAddress addr = (MessageAddress) itr.next();
	    AgentStatusService.AgentState state = 
		agentStatusService.getRemoteAgentState(addr);
	    if (state != null) {
		AgentSnapShot record = new AgentSnapShot(state);
		getAgentHistory(addr, REMOTE).add(record);
	    }
	}
 
    }


    protected void setupSubscriptions() {
    }
  
    protected void execute() {
	//System.out.println("Executed AgentStatusRatePlugin");
    }

}

/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.qos.metrics;

import java.util.HashMap;
import java.util.Iterator;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;

public class AgentStatusRatePlugin 
    extends ComponentPlugin
    implements Runnable, Constants
{
    private static final int LOCAL = 0;
    private static final int REMOTE = 1;
    private static final int BASE_PERIOD = 10; //10SecAVG

    private AgentStatusService agentStatusService;
    private MetricsUpdateService metricsUpdate;
    private HashMap agentLocalHistories;
    private HashMap agentRemoteHistories;
    private MessageAddress nodeID;
    private Schedulable schedulable;

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
	    super(10, 3, BASE_PERIOD);
	    this.agent = address;
	    store.put(address, this);
	}

    }

    private class AgentLocalHistory extends AgentHistory {
	String msgInKey;
	String msgOutKey;
	String bytesInKey;
	String bytesOutKey;

	AgentLocalHistory(MessageAddress address) {
	    super(address, agentLocalHistories);
	    String agentKey ="Agent" +KEY_SEPR+ agent  +KEY_SEPR ;
	    msgInKey = (agentKey + MSG_IN).intern();
	    addKey( msgInKey);
	    msgOutKey = (agentKey + MSG_OUT).intern();
	    addKey( msgOutKey);
	    bytesInKey = (agentKey + BYTES_IN).intern();
	    addKey( bytesInKey);
	    bytesOutKey = (agentKey + BYTES_OUT).intern();
	    addKey( bytesOutKey);
	}

	public void newAddition(KeyMap keys,
			 DecayingHistory.SnapShot rawNow, 
			 DecayingHistory.SnapShot rawLast) 
	{
	    AgentSnapShot now = (AgentSnapShot) rawNow;
	    AgentSnapShot last = (AgentSnapShot) rawLast;
	    updateMetric(keys.getKey(msgInKey),
			 msgInRate(now,last), "msg/sec");
	    updateMetric(keys.getKey(msgOutKey),
			 msgOutRate(now,last), "msg/sec");
	    updateMetric(keys.getKey(bytesInKey),
			 bytesInRate(now,last),"bytes/sec");
	    updateMetric(keys.getKey(bytesOutKey),
			 bytesOutRate(now,last), "bytes/sec");
	}
    }

    private class AgentRemoteHistory extends AgentHistory {
	String msgFromKey;
	String msgToKey;
	String bytesFromKey;
	String bytesToKey;

	AgentRemoteHistory(MessageAddress address) {
	    super(address, agentRemoteHistories);
	    String agentKey = "Node" +KEY_SEPR+ nodeID
		+KEY_SEPR+ "Destination" +KEY_SEPR+
		agent  +KEY_SEPR;
	    msgFromKey = (agentKey + MSG_FROM).intern();
	    addKey( msgFromKey);
	    msgToKey = (agentKey + MSG_TO).intern();
	    addKey( msgToKey);
	    bytesFromKey = (agentKey + BYTES_FROM).intern();
	    addKey( bytesFromKey);
	    bytesToKey = (agentKey + BYTES_TO).intern();
	    addKey( bytesToKey);
	}

	public void newAddition(KeyMap keys,
			  DecayingHistory.SnapShot rawNow, 
			  DecayingHistory.SnapShot rawLast) 
	{
	    AgentSnapShot now = (AgentSnapShot) rawNow;
	    AgentSnapShot last = (AgentSnapShot) rawLast;
	    updateMetric(keys.getKey(msgFromKey),
			 msgInRate(now,last),"msg/sec");
	    updateMetric(keys.getKey(msgToKey),
			 msgOutRate(now,last),"msg/sec");
	    updateMetric(keys.getKey(bytesFromKey),
			 bytesInRate(now,last),"bytes/sec");
	    updateMetric(keys.getKey(bytesToKey),
			 bytesOutRate(now,last),"bytes/sec");
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



    private void updateMetric(String key,
				   double value, 
				   String units)
    {
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
	    ThreadService tsvc = (ThreadService)
		sb.getService(this, ThreadService.class, null);
	    schedulable = tsvc.getThread(this, this, "AgentStatus");
	    schedulable.schedule(0, BASE_PERIOD*1000);
	    sb.releaseService(this, ThreadService.class, tsvc);
	}
    }

    // The body of the Schedulable
    public void run() {
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

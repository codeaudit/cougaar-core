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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.text.DecimalFormat;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.mts.AgentStatusService;

public class AgentLoadTracePlugin 
    extends ComponentPlugin
    implements Constants
{
    private LoggingService loggingService = null;    
    private AgentContainer agentContainer;
    private MetricsService metricsService;
    private String node;
    private DecimalFormat formatter = new DecimalFormat("###,#00.0#");
    private boolean first_time = true;
    private ArrayList agents;
    private long start;
    private Schedulable schedulable;
    private static final int BASE_PERIOD = 10; //10SecAVG
    private AgentStatusService agentStatusService=null; 
    

    private class Poller implements Runnable {
	public void run() {	    
	    if (first_time) {
		
		collectNames();
		// log data key
		if (loggingService.isInfoEnabled()) {
		    loggingService.info("--- Data Key ---\n" + 
					"Timestamp, " +
					"AgentName, " +
					"CPULoad, " +
					"CPULoadJips, " +
					"MsgIn, " +
					"MsgOut, " +
					"bytesIn, " +
					"BytesOut, " +
					"PersistSize" );
		}	    
	    }
	    else
		dump();
	    first_time = false;
	}
    }

    public AgentLoadTracePlugin() {
	super();
    }
    

    private void collectNames() {
	start = System.currentTimeMillis();
	Set localAgents = getLocalAgents();
	if (localAgents == null) { 
	    return;
	}
	agents = new ArrayList();
	for (Iterator itr = localAgents.iterator(); itr.hasNext(); ) {
            MessageAddress addr = (MessageAddress) itr.next();
	    String name = addr.getAddress();
	    agents.add(name);
	}
    }

    private void logAgentMetrics(String name) {
	if(name == null) {
	    loggingService.debug("Agent is null, cannot retrieve any metrics!");
	}
	String agentPath = "Agent(" +name+ ")"+PATH_SEPR;
	
	Metric cpuLoad = metricsService.getValue(agentPath
						 + CPU_LOAD_AVG_10_SEC_AVG);
	double cpuLoadV = cpuLoad.doubleValue();
	    
	Metric cpuLoadJips = metricsService.getValue(agentPath
						     + CPU_LOAD_MJIPS_10_SEC_AVG);
	double cpuLoadJipsV = cpuLoadJips.doubleValue();

	Metric msgIn = metricsService.getValue(agentPath+
					       MSG_IN_10_SEC_AVG);
	double msgInV = msgIn.doubleValue();

	Metric msgOut = metricsService.getValue(agentPath+
						MSG_OUT_10_SEC_AVG);
	double msgOutV = msgIn.doubleValue();

	Metric bytesIn = metricsService.getValue(agentPath+
						 BYTES_IN_10_SEC_AVG);
	double bytesInV = bytesIn.doubleValue();

	Metric bytesOut = metricsService.getValue(agentPath+
						  BYTES_OUT_10_SEC_AVG);
	double bytesOutV = bytesOut.doubleValue();

	Metric persistSize = metricsService.getValue(agentPath+
						     PERSIST_SIZE_LAST );
	double persistSizeV = persistSize.doubleValue();

	long now =  System.currentTimeMillis();
	    
	if (loggingService.isInfoEnabled()) {
	    loggingService.info(now + ", "+ 
				name  +","+
				formatter.format(cpuLoadV) +","+
				formatter.format(cpuLoadJipsV)  +","+
				formatter.format(msgInV) +","+
				formatter.format(msgOutV) +","+
				formatter.format(bytesInV) +","+
				formatter.format(bytesOutV) +","+
				formatter.format(persistSizeV) );
	}
    }
    
    private long relativeTimeMillis() {
	return System.currentTimeMillis()-start;
    }
    
    private void dump() {
	Iterator itr = agents.iterator();
	while (itr.hasNext()) {
	    String name = (String) itr.next();
	    logAgentMetrics(name);
	}
    }

    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();

	NodeControlService ncs = (NodeControlService)
            sb.getService(this, NodeControlService.class, null);
        if (ncs != null) {
            agentContainer = ncs.getRootContainer();
            sb.releaseService(this, NodeControlService.class, ncs);
        }

	metricsService = (MetricsService)
	    sb.getService(this, MetricsService.class, null);

	NodeIdentificationService nis = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
 	node = nis.getMessageAddress().toString();

	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	
	agentStatusService = (AgentStatusService)
	    sb.getService(this, AgentStatusService.class, null);
	
	ThreadService tsvc = (ThreadService)
	    sb.getService(this, ThreadService.class, null);

	Poller poller = new Poller();
	Schedulable sched = tsvc.getThread(this, 
					   poller,
					   "LoadPoller");
	//sched.schedule(60000, 500);
	sched.schedule(5000, BASE_PERIOD*1000);
	sb.releaseService(this, ThreadService.class, tsvc);
	
    }

    /**
     * @return the message addresses of the agents on this
     * node, or null if that information is not available.
     */
    protected final Set getLocalAgents() {
	//try getting agents from agentstatus service instead
	if(agentStatusService == null) {
	    if(loggingService.isDebugEnabled())
		loggingService.debug("No LocalAgents from AgentStatusService");
	    return null;
	} else {
	    return agentStatusService.getLocalAgents();
	}
	/*
        if (agentContainer == null) {
            return null;
        } else {
            return agentContainer.getAgentAddresses();
        }
	*/
    }

    protected void setupSubscriptions() {
    }
  
    protected void execute() {
    }

}

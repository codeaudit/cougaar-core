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

package org.cougaar.core.qos.monitor;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.NameSupport;
import org.cougaar.core.society.MessageAddress;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;

public class ResourceMonitorServiceImpl 
    extends QosImplBase
    implements ResourceMonitorService
{

    private HashMap nodes, hosts;

    protected ResourceMonitorServiceImpl(NameSupport nameSupport,
					 ServiceBroker sb) 
    {
	super(nameSupport, sb);
	nodes = new HashMap();
	hosts = new HashMap();
    }

    public double getJipsForAgent(MessageAddress agentAddress) {
	return 11.0;
    }

    public Observable getJipsForAgentObservable(MessageAddress agentAddress) {
	return null;
    }

    
   public String lookupHostForAgent(MessageAddress agentAddress) {
	Attributes match = 
	    new BasicAttributes(NameSupport.AGENT_ATTR, agentAddress);
	String attr = NameSupport.HOST_ATTR;
	Iterator result = nameSupport.lookupInTopology(match, attr);
	String host = null;
	int count = 0;
	while (result.hasNext()) {
	    count++;
	    host = (String) result.next();
	}
	if (count == 0) {
	    return null;
	} else if (count == 1) {
	    hosts.put(agentAddress, host);
	    return host;
	} else {
	    // more than one match!
	    throw new RuntimeException("### More than one match for " +
				       agentAddress);
	}
    }


   public String lookupNodeForAgent(MessageAddress agentAddress) {
	Attributes match = 
	    new BasicAttributes(NameSupport.AGENT_ATTR, agentAddress);
	String attr = NameSupport.NODE_ATTR;
	Iterator result = nameSupport.lookupInTopology(match, attr);
	String node = null;
	int count = 0;
	while (result.hasNext()) {
	    count++;
	    node = (String) result.next();
	}
	if (count == 0) {
	    return null;
	} else if (count == 1) {
	    nodes.put(agentAddress, node);
	    return node;
	} else {
	    // more than one match!
	    throw new RuntimeException("### More than one match for " +
				       agentAddress);
	}
    }

    
    public String getNodeForAgent(MessageAddress agentAddress) {
	AgentStatusService.AgentState state = getAgentState(agentAddress);
	long now = System.currentTimeMillis();
	long since = now-state.timestamp;
	String node = (String) nodes.get(agentAddress);
	if (node != null &&
	    state.status == AgentStatusService.ACTIVE &&
	    since <= STALE_TIME) {
	    return node;
	} else {
	    return lookupNodeForAgent(agentAddress);
	}
    }

    public String getHostForAgent(MessageAddress agentAddress) {
	AgentStatusService.AgentState state = getAgentState(agentAddress);
	long now = System.currentTimeMillis();
	long since = now-state.timestamp;
	String host = (String) hosts.get(agentAddress);
	if (host != null &&
	    state.status == AgentStatusService.ACTIVE &&
	    since <= STALE_TIME) {
	    return host;
	} else {
	    return lookupHostForAgent(agentAddress);
	}
    }

}

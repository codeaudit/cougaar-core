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
import org.cougaar.core.mts.MessageAddress;

import java.util.HashMap;
import java.util.Iterator;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

public class QosMonitorServiceImpl 
    extends QosImplBase
    implements QosMonitorService
{
    private HashMap nodes, hosts;

    QosMonitorServiceImpl(NameSupport nameSupport, ServiceBroker sb) {
	super(nameSupport, sb);
	nodes = new HashMap();
	hosts = new HashMap();
    }

    public int lookupAgentStatus(MessageAddress agentAddress) {

	Attributes match = 
	    new BasicAttributes(NameSupport.AGENT_ATTR, agentAddress);
	String attr = NameSupport.STATUS_ATTR;
	Iterator result = nameSupport.lookupInTopology(match, attr);
	int status = -1;
	int count = 0;
	while (result.hasNext()) {
	    count++;
	    String status_string = (String) result.next();
	    if (status_string == null)
		status = UNKNOWN;
	    else if (status_string.equals(NameSupport.REGISTERED_STATUS))
		status = ACTIVE;
	    else if (status_string.equals(NameSupport.UNREGISTERED_STATUS))
		status = MOVING;
	    else
		status = UNKNOWN;
	}
	if (count == 0) {
	    return NOT_CREATED;
	} else if (count == 1) {
	    return status;
	} else {
	    // more than one match!
	    throw new RuntimeException("### More than one match for " +
				       agentAddress);
	}
    }


    public int getAgentStatus(MessageAddress agentAddress) {
	AgentStatusService.AgentState state = getAgentState(agentAddress);
	long now = System.currentTimeMillis();
	long since = now-state.timestamp;

	if (state.status == AgentStatusService.ACTIVE && since <= STALE_TIME) 
	    return ACTIVE;
	else
	    return lookupAgentStatus(agentAddress);
    }


    public int getAgentCommStatus(MessageAddress agentAddress){
	AgentStatusService.AgentState state = getAgentState(agentAddress);
	long now = System.currentTimeMillis();
	long since = now-state.timestamp;
	if (state.status == AgentStatusService.ACTIVE && since <= STALE_TIME) 
	    return ACTIVE;	
	else if (state.status == AgentStatusService.UNREACHABLE && 
		 since <= STALE_TIME)
	    return FAILING;
	else 
	    return lookupAgentStatus(agentAddress);
    }



    // The rest used to be in ResourceMonitorService


    public String lookupHostForNode(String nodeName) {
	Attributes match = 
	    new BasicAttributes(NameSupport.NODE_ATTR, nodeName);
	String attr = NameSupport.HOST_ATTR;
	Iterator result = nameSupport.lookupInTopology(match, attr);
	if (result.hasNext()) {
	    return (String) result.next();
	} else {
	    return null;
	} 
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


    public String getHostForNode(String nodeName) {
	String host = (String) hosts.get(nodeName);
	if (host != null) {
	    return host;
	} else {
	    return lookupHostForNode(nodeName);
	}
    }


}


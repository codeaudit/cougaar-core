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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;


public class StandardVariableEvaluator implements VariableEvaluator
{
    protected ServiceBroker sb;
    private AgentIdentificationService agentid_service;
    private NodeIdentificationService nodeid_service;
    private String host;

    public StandardVariableEvaluator(ServiceBroker sb) {
	this.sb = sb;
	agentid_service = (AgentIdentificationService)
	    sb.getService(this, AgentIdentificationService.class, null);
	nodeid_service = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
	try {
	    host = java.net.InetAddress.getLocalHost().getHostAddress();
	} catch (Exception ex) {
	    LoggingService lsvc = (LoggingService)
		sb.getService(this, LoggingService.class, null);
	    if (lsvc.isErrorEnabled())
		lsvc.error("Failed to get local address: " + ex);
	    host = "10.0.0.0"; // nice
	}
    }

    public String evaluateVariable(String var) {
	if (var.equals("localagent")) {
	    return "Agent(" +agentid_service.getName()+ ")";
	} else if (var.equals("localnode")) {
	    return "Node(" +nodeid_service.getMessageAddress().getAddress()+
		")";
	} else if (var.equals("localhost")) {
	    return "Host(" +host+ ")";
	} else {
	    return null;
	}
    }

}

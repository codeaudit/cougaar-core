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

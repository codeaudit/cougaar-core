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


class QosImplBase
{
    static final int STALE_TIME = 10000; // 10 seconds

    private ServiceBroker sb;
    private AgentStatusService statusService;
    protected NameSupport nameSupport;

    QosImplBase(NameSupport nameSupport, ServiceBroker sb) {
	this.sb = sb;
	this.nameSupport = nameSupport;
    }


    private void ensureService() {
	if (statusService == null) {
	    Object svc = 
		sb.getService(this, AgentStatusService.class, null);
	    if (svc == null) {
		System.err.println("### Can't find AgentStatusService");
	    } else {
		statusService = (AgentStatusService) svc;
		// System.out.println("%%% Got AgentStatusService!");
	    }
	}
    }

    AgentStatusService.AgentState getAgentState(MessageAddress agentAddress) {
	ensureService();
	return statusService.getAgentState(agentAddress);
    }

}


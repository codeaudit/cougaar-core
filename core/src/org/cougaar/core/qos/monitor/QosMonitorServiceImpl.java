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

import org.cougaar.core.mts.NameSupport;
import org.cougaar.core.society.MessageAddress;

import java.util.Iterator;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

public class QosMonitorServiceImpl implements QosMonitorService
{
    private NameSupport nameSupport;

    QosMonitorServiceImpl(NameSupport nameSupport) {
	this.nameSupport = nameSupport;
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
	return lookupAgentStatus(agentAddress);
    }
    

}


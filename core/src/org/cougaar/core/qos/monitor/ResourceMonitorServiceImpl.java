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

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.NameSupport;
import org.cougaar.core.society.MessageAddress;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import java.util.Iterator;
import java.util.Observable;

public class ResourceMonitorServiceImpl implements ResourceMonitorService
{
    protected NameSupport nameSupport;

    protected ResourceMonitorServiceImpl(NameSupport nameSupport) {
	this.nameSupport = nameSupport;
    }

    public double getJipsForAgent(MessageAddress agentAddress) {
	return 10.0;
    }

    public Observable getJipsForAgentObservable(MessageAddress agentAddress) {
	return null;
    }

    
   public String getHostForAgent(MessageAddress agentAddress) {
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
	    return host;
	} else {
	    // more than one match!
	    throw new RuntimeException("### More than one match for " +
				       agentAddress);
	}
    }

   public String getNodeForAgent(MessageAddress agentAddress) {
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
	    return node;
	} else {
	    // more than one match!
	    throw new RuntimeException("### More than one match for " +
				       agentAddress);
	}
    }

}


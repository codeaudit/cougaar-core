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

package org.cougaar.core.thread;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.plugin.ComponentPlugin;

public class AgentSensorPlugin extends ComponentPlugin
{
    public AgentSensorPlugin() {
	super();
    }

    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();
	// Watcher needs the Node's service broker.
	// 
	// NB: NodeControlService is only available on NodeAgents.
	// Loading this plugin in any other context will fail.
	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);

	if (ncs != null)
	    new LoadWatcher(ncs.getRootServiceBroker());
	else
	    System.err.println("AgentSensonPlugin can only be used in NodeAgents");
    }

    protected void setupSubscriptions() {
    }
  
    protected void execute() {
	System.out.println("Uninteresting");
    }

}

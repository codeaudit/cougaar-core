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

package org.cougaar.core.thread;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.ServletService;

/**
 * This class creates a servlet which displays the state of COUGAAR
 * ThreadService threads (Schedulables) in a way that's vaguely
 * remniscent of the unix 'top' command.
 * 
 * This is designed to be a Node-level plugin.
 */
public class TopPlugin extends ComponentPlugin
{
    public TopPlugin() {
	super();
    }



    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();

	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);

	if (ncs == null) {
	    throw new RuntimeException("Unable to obtain service");
	}

	ServiceBroker rootsb = ncs.getRootServiceBroker();

	ThreadStatusService statusService = (ThreadStatusService)
	    rootsb.getService(this, ThreadStatusService.class, null);
	
	ServletService servletService = (ServletService)
	    sb.getService(this, ServletService.class, null);

	if (servletService == null || statusService == null) {
	    throw new RuntimeException("Unable to obtain service");
	}

	TopServlet servlet = new TopServlet(statusService);

	// register the servlet
	try {
	    servletService.register(servlet.getPath(), servlet);
	} catch (Exception e) {
	    throw new RuntimeException("Unable to register servlet at path <"
				       +servlet.getPath()+ ">: " +e);
	}

    }

    protected void setupSubscriptions() {
    }
  
    protected void execute() {
	//System.out.println("Uninteresting");
    }

}

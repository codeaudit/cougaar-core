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
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.util.UnaryPredicate;

public class RootControlPlugin extends ComponentPlugin
{
    private static final int MAX_THREADS=2;

    public RootControlPlugin() {
	super();
    }


    private static class ExampleChildQualifier implements UnaryPredicate {
	private LoggingService lsvc;

	ExampleChildQualifier(LoggingService lsvc) {
	    this.lsvc = lsvc;
	}

	public boolean execute(Object x) {
	    if (! (x instanceof Scheduler)) return false;

	    Scheduler child = (Scheduler) x;
	    int lane = child.getLane();
	    Scheduler parent = 
		child.getTreeNode().getParent().getScheduler(lane);
	    float count = child.runningThreadCount();
	    float max = parent.maxRunningThreadCount();
	    // Random test - don't let any one child use more than half
	    // the slots
	    if (count/max <= .5) {
		return true;
	    } else {
		if (lsvc.isWarnEnabled()) 
		    lsvc.warn("Attempted to use too many rights Child="+child);
		return false;
	    }
	}
    }


    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();
	LoggingService lsvc = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);
	sb = ncs.getRootServiceBroker();
 	ThreadControlService tcs = (ThreadControlService)
 	    sb.getService(this, ThreadControlService.class, null);
//  	RightsSelector selector = new PercentageLoadSelector(sb);
 	//tcs.setRightsSelector(selector);
	if (tcs != null) {
	    tcs.setMaxRunningThreadCount(MAX_THREADS);
	    tcs.setChildQualifier(new ExampleChildQualifier(lsvc));
	}
	
    }

    protected void setupSubscriptions() {
    }
  
    protected void execute() {
	System.out.println("Uninteresting");
    }

}

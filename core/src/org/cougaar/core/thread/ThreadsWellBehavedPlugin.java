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

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ParameterizedComponent;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadService;

/**
 * This class marks the agent as wellbehaved. This means that all the
 * plugins in the agent will not block on IO or Network and will not
 * hog the CPU. This Plugin calls the ThreadControlService and sets
 * the default Thread Lane to be WELL_BEHAVED. After load time, the
 * plugin should do nothing. 
 * 
 * This is designed to be an Agent Plugin
 * 
 * JAZ this has not been tested with mobility, yet. Also, what about
 * when this plugin loads, maybe some threads will already be started.
 */
public class ThreadsWellBehavedPlugin
    extends ParameterizedComponent // not really a Plugin
{
    private ServiceBroker sb;
    private int defaultLane;

    public ThreadsWellBehavedPlugin() {
	super();
    }



    public void load() {
	super.load();
	long defaultLane = getParameter("defaultLane", 
					  ThreadService.WELL_BEHAVED_LANE);
	ServiceBroker sb = getServiceBroker();
	ThreadControlService tsvc = (ThreadControlService) 
	    sb.getService(this, ThreadControlService.class, null);
	LoggingService lsvc = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	tsvc.setDefaultLane((int)defaultLane);
	if (lsvc.isDebugEnabled()) 
	    lsvc.debug("Default Lane Set to " + defaultLane +
		       " got back" + tsvc.getDefaultLane());
	
	sb.releaseService(this, ThreadControlService.class, tsvc);
	sb.releaseService(this, LoggingService.class, lsvc);
    }

    public final void setBindingSite(BindingSite bs) {
	sb = bs.getServiceBroker();
    }


    public ServiceBroker getServiceBroker() {
	return sb;
    }

}

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
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadListenerService;


/**
 * The ServiceProvider for ThreadService and ThreadControlService.
 */
public final class ThreadServiceProvider implements ServiceProvider 
{
    private ThreadListenerProxy listenerProxy;
    private Scheduler scheduler;
    private ThreadServiceProxy proxy;
    private String name;

    /**
     * Create a new set of thread services. Deduce the parent by
     * asking the ServiceBroker.
     */
    public ThreadServiceProvider(ServiceBroker sb, String name) {
	this.name = name;
	ThreadService parent =(ThreadService) 
	    sb.getService(this, ThreadService.class, null);
	makeProxies(parent);
    }

    /**
     * Create a new set of thread services.  The parent is provided
     * explicitly.  No one uses this yet; it may go away.
     */
    public ThreadServiceProvider(ThreadService parent, String name) {
	this.name = name;
	makeProxies(parent);
    }

    private void makeProxies(ThreadService parent) {
	listenerProxy = new ThreadListenerProxy();
	if (Boolean.getBoolean("org.cougaar.thread.timeslice"))
	    scheduler = new SimplePropagatingScheduler(listenerProxy, name);
	else
	    scheduler = new SimpleScheduler(listenerProxy, name);

	ThreadServiceProxy parentProxy = (ThreadServiceProxy) parent;
	TreeNode node = new TreeNode(scheduler, parentProxy);
	proxy = new ThreadServiceProxy(node);
    }

    public void provideServices(ServiceBroker sb) {
	sb.addService(ThreadService.class, this);
	sb.addService(ThreadControlService.class, this);
	sb.addService(ThreadListenerService.class, this);
    }


    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == ThreadService.class) {
	    return proxy;
	} else if (serviceClass == ThreadControlService.class) {
	    // Later this will be tightly restricted
	    return scheduler;
	} else if (serviceClass == ThreadListenerService.class) {
	    return listenerProxy;
	} else {
	    return null;
	}
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }

 
}


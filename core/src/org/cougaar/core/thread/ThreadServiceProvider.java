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

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.util.GenericStateModelAdapter;


/**
 * The ServiceProvider for ThreadService and ThreadControlService.
 *
 * @property org.cougaar.thread.scheduler specifies the class of
 * scheduler to use.  The default is PropagatingScheduler.  The only
 * other reasonable choice at the moment would be
 * 'org.cougaa.core.thread.Scheduler'
 */
public final class ThreadServiceProvider 
    extends GenericStateModelAdapter
    implements ServiceProvider, Component
{
    private static final String SCHEDULER_CLASS_PROPERTY = 
	"org.cougaar.thread.scheduler";

    private ServiceBroker my_sb;
    private boolean isRoot;
    private ThreadListenerProxy listenerProxy;
    private Scheduler scheduler;
    private ThreadServiceProxy proxy;
    private String name;

    /**
     * Create a new set of thread services. Deduce the parent by
     * asking the ServiceBroker.
     */
//     public ThreadServiceProvider(ServiceBroker sb, String name) {
// 	this.name = name;
// 	ThreadService parent = (ThreadService) 
// 	    sb.getService(this, ThreadService.class, null);
// 	makeProxies(parent);
//     }


    public ThreadServiceProvider() {
    }

    public void load() {
	super.load();

	ServiceBroker sb = my_sb;
	if (isRoot) {
	    // use the root service broker
	    NodeControlService ncs = (NodeControlService)
		my_sb.getService(this, NodeControlService.class, null);
	    sb = ncs.getRootServiceBroker();
	}
	
	ThreadService parent = (ThreadService) 
	    sb.getService(this, ThreadService.class, null);
	makeProxies(parent);
	provideServices(sb);
    }

    private void setParameterFromString(String property) {
	int sepr = property.indexOf('=');
	if (sepr < 0) return;
	String key = property.substring(0, sepr);
	String value = property.substring(++sepr);
	if (key.equals("name")) {
	    name = value;
	} else if (key.equals("isRoot")) {
	    isRoot = value.equalsIgnoreCase("true");
	} // add more later
    }

    public void setParameter(Object param) {
	if (param instanceof List) {
	    Iterator itr = ((List) param).iterator();
	    while(itr.hasNext()) {
		setParameterFromString((String) itr.next());
	    }
	} else if (param instanceof String) {
	    setParameterFromString((String) param);
	}
    }

    private void makeProxies(ThreadService parent) {
	listenerProxy = new ThreadListenerProxy();

	Class[] formals = { ThreadListenerProxy.class, String.class};
	Object[] actuals = { listenerProxy, name};
	String classname = System.getProperty(SCHEDULER_CLASS_PROPERTY);
	if (classname != null) {
	    try {
		Class s_class = Class.forName(classname);
		Constructor cons = s_class.getConstructor(formals);
		scheduler = (Scheduler) cons.newInstance(actuals);
	    } catch (Exception ex) {
		ex.printStackTrace();
	    }
	}
	if (scheduler == null) {
	    scheduler = new PropagatingScheduler(listenerProxy, name);
	}

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

 
    public final void setBindingSite(BindingSite bs) {
	my_sb = bs.getServiceBroker();
    }

}


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

import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The ServiceProvider for the trivial ThreadService
 *
 */
public final class TrivialThreadServiceProvider 
    extends GenericStateModelAdapter
    implements ServiceProvider, Component
{
    private ServiceBroker my_sb;
    private TrivialThreadServiceProxy proxy;
    private ThreadStatusService statusProxy;

    public TrivialThreadServiceProvider() 
    {
    }

    public void load() 
    {
	super.load();
	if (!my_sb.hasService(ThreadService.class)) makeServices(my_sb);
    }

    void makeServices(ServiceBroker sb)
    {
	proxy = new TrivialThreadServiceProxy();
	statusProxy = new ThreadStatusService() {
		public List getStatus() {
		    List result = new ArrayList();
		    TrivialThreadPool.pool().listRunningThreads(result);
		    return result;
		}
	    };

	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);
	ServiceBroker rootsb = ncs.getRootServiceBroker();
	sb.releaseService(this, NodeControlService.class, ncs);

	rootsb.addService(ThreadService.class, this);
	rootsb.addService(ThreadStatusService.class, this);
    }


    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == ThreadService.class) {
	    return proxy;
	} else if (serviceClass == ThreadStatusService.class) {
	    return statusProxy;
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

 
    public final void setBindingSite(BindingSite bs) 
    {
	my_sb = bs.getServiceBroker();
    }

}


/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.thread;

import java.util.ArrayList;
import java.util.List;

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
public class TrivialThreadServiceProvider 
    extends GenericStateModelAdapter
    implements ServiceProvider, Component
{
    private ServiceBroker sb;
    private ThreadService proxy;
    private ThreadStatusService statusProxy;

    public TrivialThreadServiceProvider() 
    {
    }

    public void setServiceBroker(ServiceBroker sb)
    {
        this.sb = sb;
    }

    public void load() 
    {
	super.load();
	/* if (!sb.hasService(ThreadService.class)) */ makeServices(sb);
    }


    ThreadService makeThreadServiceProxy()
    {
	return new TrivialThreadServiceProxy();
    }

    ThreadStatusService makeThreadStatusService()
    {
	return new ThreadStatusService() {
		public List getStatus() {
		    List result = new ArrayList();
		    TrivialThreadPool.pool().listRunningThreads(result);
		    return result;
		}
	    };
    }

    void makeServices(ServiceBroker the_sb)
    {
	proxy = makeThreadServiceProxy();
	statusProxy = makeThreadStatusService();

	NodeControlService ncs = (NodeControlService)
	    the_sb.getService(this, NodeControlService.class, null);
	ServiceBroker rootsb = null;
	if (ncs != null) {
	    rootsb = ncs.getRootServiceBroker();
	    the_sb.releaseService(this, NodeControlService.class, ncs);
	}

	if (rootsb != null) {
	    rootsb.addService(ThreadService.class, this);
	    rootsb.addService(ThreadStatusService.class, this);
	} else {
	    the_sb.addService(ThreadService.class, this);
	    the_sb.addService(ThreadStatusService.class, this);
	}
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

}


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


import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.PropagatingServiceBroker;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.mts.NameSupport;
import org.cougaar.core.mts.NameSupportImpl;

import java.lang.reflect.Constructor;

public class QosMonitorServiceProvider 
    extends ContainerSupport
    implements ContainerAPI, ServiceProvider
{

    private QosMonitorService qms;
    private ResourceMonitorService rms;
    private ContainerAPI owner;
    private NameSupport nameSupport;
    private String id;

    public QosMonitorServiceProvider(String id, ContainerAPI owner) {
	this.id = id;
	this.owner = owner;
    }


    public void initialize() {
	// This is a problem.  We can't get at NameSupport here.
	// nameSupport = NameSupportImpl.instance();

	ServiceBroker sb = owner.getServiceBroker();
	nameSupport = 
	    (NameSupport) sb.getService(this, NameSupport.class, null);
        super.initialize();
    }

    private synchronized QosMonitorService findOrMakeQMS(ServiceBroker sb) {
	if (qms == null) qms = new QosMonitorServiceImpl(nameSupport, sb);
	return qms;
    }

    private synchronized ResourceMonitorService findOrMakeRMS(ServiceBroker sb)
    {
	if (rms != null) {
	    return rms;
	} else {
	    try {
		Class rss_class = Class.forName("org.cougaar.core.qos.quo.RSSLink");
		Class[] types = { NameSupport.class, ServiceBroker.class };
		Object[] args = { nameSupport, sb };
		Constructor cons = rss_class.getConstructor(types);
		rms = (ResourceMonitorService) cons.newInstance(args);
	    } catch (Exception ex) {
		// RSS not loaded
		LoggingService ls = (LoggingService)
		    sb.getService(this, LoggingService.class, null);
		if (ls != null)
		    ls.warn("No RSS, using default ResourceMonitorService");
		rms = new ResourceMonitorServiceImpl(nameSupport, sb);
	    }
	    return rms;
	}
    }

    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == QosMonitorService.class) {
	    return findOrMakeQMS(owner.getServiceBroker());
	} else if (serviceClass == ResourceMonitorService.class) {
	    return findOrMakeRMS(owner.getServiceBroker());
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






    // Container

    // may need to override ComponentFactory specifyComponentFactory()

    protected String specifyContainmentPoint() {
	return "qos.monitor";
    }

    public void requestStop() {}

    public final void setBindingSite(BindingSite bs) {
        super.setBindingSite(bs);
        setChildServiceBroker(new PropagatingServiceBroker(bs));
    }

    protected Class specifyChildBindingSite() {
        return null;
    }

    public ContainerAPI getContainerProxy() {
	return this;
    }

}
    

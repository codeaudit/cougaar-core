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
import org.cougaar.core.naming.NamingService;
import org.cougaar.core.mts.NameSupport;
import org.cougaar.core.mts.NameSupportImpl;

import java.lang.reflect.Constructor;

public class QosMonitorServiceProvider 
    extends ContainerSupport
    implements ContainerAPI, ServiceProvider
{

    private QosMonitorService qms;
    private ResourceMonitorService rms;
    private NameSupport nameSupport;
    private String id;

    public QosMonitorServiceProvider(String id) {
	this.id = id;
    }


    public void initialize() {
	// NB: This depends on the fact that MTS has already created
	// the NameSupport singleton!
	nameSupport = NameSupportImpl.instance();
        super.initialize();
    }

    private synchronized QosMonitorService findOrMakeQMS() {
	if (qms == null) qms = new QosMonitorServiceImpl();
	return qms;
    }

    private synchronized ResourceMonitorService findOrMakeRMS() {
	if (rms != null) {
	    return rms;
	} else {
	    try {
		Class rss_class = Class.forName("org.cougaar.core.qos.quo.RSSLink");
		Class[] types = { NameSupport.class };
		Object[] args = { nameSupport };
		Constructor cons = rss_class.getConstructor(types);
		rms = (ResourceMonitorService) cons.newInstance(args);
		System.out.println("!!!!! Made RSSLink !!!");
	    } catch (Exception ex) {
		// RSS not loaded
		System.err.println("### No RSS, using default ResourceMonitorService");
		rms = new ResourceMonitorServiceImpl();
	    }
	    return rms;
	}
    }

    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == QosMonitorService.class) {
	    return findOrMakeQMS();
	} else if (serviceClass == ResourceMonitorService.class) {
	    return findOrMakeRMS();
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
    

/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
import org.cougaar.core.society.NameSupport;
import org.cougaar.core.society.NewNameSupport;

import java.lang.reflect.Constructor;

public class QosMonitorServiceProvider 
    extends ContainerSupport
    implements ContainerAPI, ServiceProvider
{

    private QosMonitorService qms;
    private ResourceMonitorService rms;
    private NameSupport nameSupport;

    public QosMonitorServiceProvider() {
    }


    private NameSupport createNameSupport(String id) {
        ServiceBroker sb = getServiceBroker();
        if (sb == null) throw new RuntimeException("No service broker");
        return new NewNameSupport(id, (NamingService)
                                  sb.getService(this, NamingService.class, null));
    }

    public void initialize() {

	// local initialization goes here
	nameSupport = createNameSupport("QosMonitorServiceProvider");
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
    

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

package org.cougaar.core.qos.metrics;

import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.PropagatingServiceBroker;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.thread.ThreadServiceProvider;

public final class MetricsServiceProvider
    extends ContainerSupport
    implements ContainerAPI, ServiceProvider, StateObject
{
    
    private static final String RETRIEVER_IMPL_CLASS =
	"org.cougaar.core.qos.rss.RSSMetricsServiceImpl";

    private static final String UPDATER_IMPL_CLASS =
	"org.cougaar.core.qos.rss.STECMetricsUpdateServiceImpl";

    private static final String SCFAC_CLASSNAME =
	"org.cougaar.lib.mquo.SyscondFactory";


    private static long Start;
    public static long relativeTimeMillis() {
	return System.currentTimeMillis()-Start;
    }

    private MetricsService retriever;
    private MetricsUpdateService updater;

    public MetricsServiceProvider() {
	BinderFactory bf = new QosBinderFactory();
	if (!attachBinderFactory(bf)) {
	    throw new RuntimeException("Failed to load the BinderFactory in MetricsServiceProvider");
	}

    }

    public void load() {

	super.load();

	ServiceBroker sb = getServiceBroker();
	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);
	ServiceBroker rootsb = ncs.getRootServiceBroker();
	
	// Could do this as a ComponentDescription
// 	ThreadServiceProvider tsp = new ThreadServiceProvider(sb, "Metrics");
// 	tsp.provideServices(sb);

	// Could use a ComponentDescription
	ThreadServiceProvider tsp = new ThreadServiceProvider();
	tsp.setParameter("name=Metrics");
	add(tsp);


	// Later these two instances will be out child components
	Start = System.currentTimeMillis();

	try {
	    Class cl = Class.forName(UPDATER_IMPL_CLASS);
	    updater = (MetricsUpdateService) cl.newInstance();
	    add(updater);
	    rootsb.addService(MetricsUpdateService.class, this);
	} catch (ClassNotFoundException cnf) {
	    // qos jar not loaded
	} catch (Exception ex) {
	    ex.printStackTrace();
	}


	try {
	    Class cl = Class.forName(RETRIEVER_IMPL_CLASS);
	    retriever = (MetricsService) cl.newInstance();
	    add(retriever);
	    rootsb.addService(MetricsService.class, this);
	} catch (ClassNotFoundException cnf) {
	    // qos jar not loaded
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	try {
	    Class cl = Class.forName(SCFAC_CLASSNAME);
	    Object scfac =  cl.newInstance();
	    add(scfac);
	} catch (ClassNotFoundException cnf) {
	    // This means the quo jar isn't loaded
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }


    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == MetricsService.class) {
	    return retriever;
	} else if (serviceClass == MetricsUpdateService.class) {
	    return updater;
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


    // We're not using this yet but leave it in anyway.
    protected String specifyContainmentPoint() {
	return "Node.AgentManager.Agent.MetricsService";
    }

    public void requestStop() {}

    public final void setBindingSite(BindingSite bs) {
        super.setBindingSite(bs);
        setChildServiceBroker(new PropagatingServiceBroker(bs));
    }


    public ContainerAPI getContainerProxy() {
	return this;
    }


    // StateModel

    // Return a (serializable) snapshot that can be used to
    // reconstitute the state later.
    public Object getState() {
	// TBD
	return null;
    }

    // Reconstitute from the previously returned snapshot.
    public void setState(Object state) {
    }

}


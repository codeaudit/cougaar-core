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

package org.cougaar.core.qos.metrics;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.thread.ThreadServiceProvider;

public final class MetricsServiceProvider
extends ContainerSupport
implements ServiceProvider
{
    
    private static final String RETRIEVER_IMPL_CLASS =
	"org.cougaar.core.qos.rss.RSSMetricsServiceImpl";

    private static final String UPDATER_IMPL_CLASS =
	"org.cougaar.core.qos.rss.RSSMetricsUpdateServiceImpl";



    private static long Start;
    public static long relativeTimeMillis() {
	return System.currentTimeMillis()-Start;
    }

    private MetricsService retriever;
    private MetricsUpdateService updater;
    private DataFeedRegistrationService registrar;

    private void makeUpdaterService() {
	try {
	    Class cl = Class.forName(UPDATER_IMPL_CLASS);
	    updater = (MetricsUpdateService) cl.newInstance();
	    add(updater);
	} catch (ClassNotFoundException cnf) {
	    // The qos jar isn't loaded.
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	
	if (updater == null) {
	    // Make a dummy service instead.
	    updater = new NullMetricsUpdateServiceImpl() ;
	}
    }

    private void makeRetrieverService() {
	try {
	    Class cl = Class.forName(RETRIEVER_IMPL_CLASS);
	    retriever = (MetricsService) cl.newInstance();
	    add(retriever);
	} catch (ClassNotFoundException cnf) {
	    // The qos jar isn't loaded. 
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	if (retriever == null) {
	    // Make a dummy service instead.
	    retriever = new NullMetricsServiceImpl ();
	}
	// MetricService Implementation also implements Registration service
	registrar = (DataFeedRegistrationService) retriever;
    }


    // This is done before child-components are created
    public void loadHighPriorityComponents() {
        super.loadHighPriorityComponents();
	ServiceBroker sb = getServiceBroker();
	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);
	ServiceBroker rootsb = ncs.getRootServiceBroker();
	
	Start = System.currentTimeMillis();
	// DataFeeds could need a thread service
	// JAZ needs a ComponentDescription?
	ThreadServiceProvider tsp = new ThreadServiceProvider();
	tsp.setParameter("name=Metrics");
	add(tsp);

	// Childern Components need Registration Service
	// but the Registration need the MetricServiceImplementation
	// make Metric Updater Service
	makeUpdaterService();
	rootsb.addService(MetricsUpdateService.class, this);
	// make Metric Service and Feed registration service
	makeRetrieverService();
	rootsb.addService(MetricsService.class, this);
	// register registration service
	sb.addService(DataFeedRegistrationService.class, this);
    }

    // Service Provider API

    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == MetricsService.class) {
	    return retriever;
	} else if (serviceClass == MetricsUpdateService.class) {
	    return updater;
	} else if (serviceClass == DataFeedRegistrationService.class) {
	    return registrar;
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

    // Container API

    protected ComponentDescriptions findInitialComponentDescriptions() {
	ServiceBroker sb = getServiceBroker();
	ComponentInitializerService cis = (ComponentInitializerService) 
	    sb.getService(this, ComponentInitializerService.class, null);
	NodeIdentificationService nis = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
	try {
	    String cp = specifyContainmentPoint();
	    String id = nis.getMessageAddress().toString();
	    ComponentDescription[] descs = cis.getComponentDescriptions(id,cp);
 	    // Want only items _below_. Could filter (not doing so now)
	    return new ComponentDescriptions(descs);
	} catch (ComponentInitializerService.InitializerException cise) {
	    cise.printStackTrace();
	    return null;
	} finally {
	    sb.releaseService(this, ComponentInitializerService.class, cis);
	    sb.releaseService(this, NodeIdentificationService.class, nis);
	}
    }

    protected String specifyContainmentPoint() {
	return Agent.INSERTION_POINT + ".MetricsServices";
    }
}

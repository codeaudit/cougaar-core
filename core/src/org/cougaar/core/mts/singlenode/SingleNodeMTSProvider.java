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

package org.cougaar.core.mts.singlenode;
import java.util.HashMap;
import java.util.ArrayList;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.thread.ThreadServiceProvider;
import org.cougaar.core.service.ThreadService;

/**
 * Baseline implementation class of a single-node
 * MessageTransportService. Consists of of only a registry, a router,
 * and wp service loading. 
 */
public final class SingleNodeMTSProvider 
extends ContainerSupport
implements ServiceProvider
{
    private final static String NOT_A_CLIENT =
	"Requestor is not a MessageTransportClient";
    
    // MTS address
    private MessageAddress address;
    private String id;
    private final HashMap proxies = new HashMap();
    private ArrayList waitingMsgs = new ArrayList();
    
    private ServiceBroker sb;
    protected LoggingService loggingService;
    protected ThreadService threadService;
    private SingleNodeMTSProxy proxy;
    
    public SingleNodeMTSProvider() {
        super();
    }


    protected String specifyContainmentPoint() {
 	return Agent.INSERTION_POINT + ".MessageTransport";
    }

    protected ComponentDescriptions findInitialComponentDescriptions() {
	ComponentInitializerService cis = (ComponentInitializerService) 
	    sb.getService(this, ComponentInitializerService.class, null);
	try {
	    String cp = specifyContainmentPoint();
 	    // Want only items _below_. Could filter (not doing so now)
	    return new ComponentDescriptions(cis.getComponentDescriptions(id,cp));
	} catch (ComponentInitializerService.InitializerException cise) {
	    if (loggingService.isInfoEnabled()) {
		loggingService.info("\nUnable to add "+id+"'s plugins ",cise);
	    }
	    return null;
	} finally {
	    sb.releaseService(this, ComponentInitializerService.class, cis);
	}
    }


    // does all loading of services
    public void initialize() {
	super.initialize();
	sb = getServiceBroker();
	NodeIdentificationService nis = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
 	id = nis.getMessageAddress().toString();
	loggingService = 
	    (LoggingService) sb.getService(this, LoggingService.class, null);

	// Could use a ComponentDescription
	ThreadServiceProvider tsp = new ThreadServiceProvider();
	tsp.setParameter("name=SingleNodeMTS");
	add(tsp);
	
	/*( DO WE NEED THIS?
	SocketControlProvision scp = new SocketControlProvision();
	sb.addService(SocketControlProvisionService.class, scp);
	// SocketFactory has no access to services, so set it manually
	// in a static.
	SocketFactory.configureProvider(sb);
	*/
    }

    // CSMART Aspects (INTERNAL priority) will load between
    // HighPriority and ComponentPriority.  CSMART LinkProtocols and
    // LinkSelectionPolicys (COMPONENT priority) will load in the
    // super.loadComponentPriorityComponents

    public void loadComponentPriorityComponents() {
	// CSMART LinkProtocols will be loaded in the super,
        super.loadComponentPriorityComponents();

	// The rest of the services depend on aspects.

        ServiceBroker sb = getServiceBroker();

	// Make router
	SingleNodeRouterImpl router = new SingleNodeRouterImpl(sb);
	
	// Make proxy
	proxy = new SingleNodeMTSProxy(router);

	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);
	
	ServiceBroker rootsb = ncs.getRootServiceBroker();
	rootsb.addService(MessageTransportService.class, this);
    }

    // ServiceProvider
    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == MessageTransportService.class) {
	    if (requestor instanceof MessageTransportClient) {
		return proxy;
	    } else {
		throw new IllegalArgumentException(NOT_A_CLIENT);
	    }
	} else {
	    return null;
	}
    }
    
    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
	if (serviceClass == MessageTransportService.class) {
	    if (requestor instanceof MessageTransportClient) {
		MessageTransportClient  client = 
		    (MessageTransportClient) requestor;
		MessageAddress addr = client.getMessageAddress();
		MessageTransportService svc = 
		    (MessageTransportService) proxies.get(addr);
		SingleNodeMTSProxy proxy =
		    (SingleNodeMTSProxy) proxies.get(addr);
		if (svc != service) return; // ???
		proxies.remove(addr);
		proxy.release();	
	    } 
	}
    }
}

/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.Container;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;



/**
 * The underlying implementation class for the
 * MessageTransportService.  It consists almost exclusively of
 * factories, each of which is described elsewhere.  The only
 * interesting local functions are those required for ServiceBrokers,
 * and a method to create the aspects from the
 * org.cougaar.message.transport.aspects property. */

public class MessageTransportServerImpl 
  extends ContainerSupport
  implements ContainerAPI
{

    // Factories
    private MessageTransportServerServiceFactory serviceFactory;
    protected MessageTransportFactory transportFactory;
    protected SendQueueFactory sendQFactory;
    protected ReceiveQueueFactory recvQFactory;
    protected DestinationQueueFactory destQFactory;
    protected LinkSenderFactory linkSenderFactory;
    protected RouterFactory routerFactory;
    protected ReceiveLinkFactory receiveLinkFactory;


    // Singletons
    protected NameSupport nameSupport;
    protected MessageTransportRegistry registry;
    protected Router router;
    protected SendQueue sendQ;
    protected ReceiveQueue recvQ;

    private ArrayList aspects;


    private void readAspects() {
	String property = "org.cougaar.message.transport.aspects";
	String classes = System.getProperty(property);
	if (classes == null) return;

	aspects = new ArrayList();
	StringTokenizer tokenizer = new StringTokenizer(classes, ",");
	while (tokenizer.hasMoreElements()) {
	    String classname = tokenizer.nextToken();
	    try {
		Class aspectClass = Class.forName(classname);
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) aspectClass.newInstance();
		aspects.add(aspect);
	    }
	    catch (Exception ex) {
		ex.printStackTrace();
		// System.err.println(ex);
	    }
	}
    }

    public MessageTransportServerImpl(String id) {
	readAspects();

	nameSupport = new NameSupport(id);
	registry = new MessageTransportRegistry(id, this);

	//Watcher Aspect is special because the MTServicer interace
	//needs it.  So we have to make the Watcher Aspect all the
	//time.
	WatcherAspect watcherAspect =  new WatcherAspect();
	registry.setWatcherManager(watcherAspect);
	if (aspects == null) aspects = new ArrayList();
	aspects.add(watcherAspect);

	serviceFactory = new MessageTransportServerServiceFactoryImpl(aspects);
	transportFactory = 
	    new MessageTransportFactory(id, registry, nameSupport, aspects);
	receiveLinkFactory = new ReceiveLinkFactory(registry,
						    aspects);

	registry.setReceiveLinkFactory(receiveLinkFactory);
	registry.setTransportFactory(transportFactory);

	wireComponents(id);

	transportFactory.setRecvQ(recvQ);
	// force transports to be created here
	transportFactory.getTransports();

    }

    protected void wireComponents(String id) {
	recvQFactory = new ReceiveQueueFactory(registry, aspects);
	recvQ = recvQFactory.getReceiveQueue(id+"/InQ");


	linkSenderFactory =
	    new LinkSenderFactory(registry, transportFactory);
	
	destQFactory = 
	    new DestinationQueueFactory(registry, 
					transportFactory, 
					linkSenderFactory,
					aspects);
	routerFactory =
	    new RouterFactory(registry, destQFactory, aspects);

	router = routerFactory.getRouter();

	sendQFactory = new SendQueueFactory(registry, aspects);
	sendQ = sendQFactory.getSendQueue(id+"/OutQ", router);

    }






    // ServiceProvider factory



    /** 
     * This class instantiates the service proxies for the Message
     * Transport Service.  Since this factory is a subclass of
     * AspectFactory, aspects can be attached to the proxies when
     * they're instantiated.  */
    private final class MessageTransportServerServiceFactoryImpl
	extends AspectFactory
	implements MessageTransportServerServiceFactory
    {

	public MessageTransportServerServiceFactoryImpl(ArrayList aspects) {
	    super(aspects);
	}

	private boolean validateRequestor(Object requestor, 
					  Class serviceClass) 
	{
	    return requestor instanceof Communications &&
		serviceClass == MessageTransportServer.class;
	}

	public Object getService(ServiceBroker sb, 
				 Object requestor, 
				 Class serviceClass) 
	{
	    if (validateRequestor(requestor, serviceClass)) {
		Object proxy = 
		    new MessageTransportServerProxy(registry, sendQ);
		return attachAspects(proxy, ServiceProxy);
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

	// This is an odd place to put this method.  It will stay here
	// for now because Communications expects to find it here and
	// we don't want to edit that class.
	public NameServer getDefaultNameServer() {
	    return nameSupport.getNameServer();
	}

    }



    // Return the sevice-proxy factory (singleton for now)
    public MessageTransportServerServiceFactory getProvider() {
	return serviceFactory;
    }








    // Container

    // may need to override ComponentFactory specifyComponentFactory()

    protected String specifyContainmentPoint() {
	return "messagetransportservice.messagetransport";
    }

    protected ServiceBroker specifyChildServiceBroker() {
	// TO BE DONE
	return null;
    }

    public ServiceBroker getServiceBroker() {
	// TO BE DONE
	return null;
    }
    public void requestStop() {}


    public ContainerAPI getContainerProxy() {
	return this;
    }

}
    

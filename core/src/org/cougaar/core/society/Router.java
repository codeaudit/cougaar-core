package org.cougaar.core.society;

import java.util.ArrayList;
import java.util.Iterator;

class Router
{
    private MessageTransportRegistry registry;
    private MessageTransport defaultTransport;
    private MessageTransport loopback;
    private ArrayList transports; // hash transports -> binders
    private MessageTransportServerImpl.DestinationQueueFactory destQFactory;


    Router(MessageTransportRegistry registry, 
	   MessageTransportServerImpl.DestinationQueueFactory destQFactory)
    {
	this.registry = registry;
	this.destQFactory = destQFactory;
	transports = new ArrayList();
    }



    void addTransport(MessageTransport tport) {
	transports.add(tport);
    }

    void setDefaultTransport(MessageTransport tport) {
	defaultTransport = tport;
    }

    void setLoopbackTransport(MessageTransport loopback) {
	this.loopback = loopback;
    }

    MessageTransport getDefaultTransport() {
	return defaultTransport;
    }

    MessageTransport getLoopbackTransport() {
	return loopback;
    }

    Iterator getTransports() {
	return transports.iterator();
    }


    MessageTransportRegistry getRegistry() {
	return registry;
    }

    void routeMessage(Message message) {
	// find or make a DestinationQueue for this message, then add
	// the message to that queue.
	MessageAddress destination = message.getTarget();
	// Factory has a fairly efficient cache, so we do not have to
	// cache here
	DestinationQueue queue = destQFactory.getDestinationQueue(destination);
	queue.holdMessage(message);
    }

}

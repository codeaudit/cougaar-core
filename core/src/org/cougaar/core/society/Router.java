package org.cougaar.core.society;

class Router
{
    private MessageTransportRegistry registry;
    private MessageTransportServerImpl.DestinationQueueFactory destQFactory;


    Router(MessageTransportRegistry registry, 
	   MessageTransportServerImpl.DestinationQueueFactory destQFactory)
    {
	this.registry = registry;
	this.destQFactory = destQFactory;
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

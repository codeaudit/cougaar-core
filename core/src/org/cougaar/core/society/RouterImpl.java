package org.cougaar.core.society;

class RouterImpl implements Router
{
    private MessageTransportRegistry registry;
    private DestinationQueueFactory destQFactory;


    RouterImpl(MessageTransportRegistry registry, 
	   DestinationQueueFactory destQFactory)
    {
	this.registry = registry;
	this.destQFactory = destQFactory;
    }

    public void routeMessage(Message message) {
	// find or make a DestinationQueue for this message, then add
	// the message to that queue.
	MessageAddress destination = message.getTarget();
	// Factory has a fairly efficient cache, so we do not have to
	// cache here
	DestinationQueue queue = destQFactory.getDestinationQueue(destination);
	queue.holdMessage(message);
    }

}

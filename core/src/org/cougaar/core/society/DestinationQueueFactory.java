package org.cougaar.core.society;

import java.util.HashMap;

public class DestinationQueueFactory 
{
    private HashMap queues;
    private MessageTransportRegistry registry;
    private MessageTransportFactory transportFactory;
    private LinkSenderFactory linkSenderFactory;
	
    DestinationQueueFactory(MessageTransportRegistry registry,
			    MessageTransportFactory transportFactory,
			    LinkSenderFactory linkSenderFactory) 
    {
	queues = new HashMap();
	this.registry = registry;
	this.transportFactory = transportFactory;
	this.linkSenderFactory = linkSenderFactory;
    }

    DestinationQueue getDestinationQueue(MessageAddress destination) {
	    
	DestinationQueue q = (DestinationQueue) queues.get(destination);
	if (q == null) {
	    q = new DestinationQueue(destination.toString(), 
				     destination,
				     linkSenderFactory);
	    queues.put(destination, q);
	}
	return q;
    }
}


package org.cougaar.core.society;

import java.util.HashMap;

public class DestinationQueueFactory extends  AspectFactory
{
    private HashMap queues;
    private MessageTransportRegistry registry;
    private MessageTransportFactory transportFactory;
    private LinkSenderFactory linkSenderFactory;
    
    DestinationQueueFactory(MessageTransportRegistry registry,
			    MessageTransportFactory transportFactory,
			    LinkSenderFactory linkSenderFactory,
			    java.util.ArrayList aspects) 
    {
	super(aspects);
	queues = new HashMap();
	this.registry = registry;
	this.transportFactory = transportFactory;
	this.linkSenderFactory = linkSenderFactory;
    }

    DestinationQueue getDestinationQueue(MessageAddress destination) {
	    
	DestinationQueue q = (DestinationQueue) queues.get(destination);
	if (q == null) {
	    q = new DestinationQueueImpl(destination.toString(), 
					 destination,
					 linkSenderFactory);
	    q = (DestinationQueue) attachAspects(q, DestinationQueue.class);
	    queues.put(destination, q);
	}
	return q;
    }
}


package org.cougaar.core.society;

import java.util.ArrayList;
import java.util.Iterator;

public class SendQueueFactory extends AspectFactory
{
    private ArrayList queues = new ArrayList();
    private MessageTransportRegistry registry;

    SendQueueFactory(MessageTransportRegistry registry,
		     ArrayList aspects)
    {
	super(aspects);
	this.registry = registry;
    }

    SendQueue getSendQueue(String name, Router router) {
	Iterator i = queues.iterator();
	while (i.hasNext()) {
	    SendQueue candidate = (SendQueue) i.next();
	    if (candidate != null && candidate.matches(name)) return candidate;
	}
	// No match, make a new one
	SendQueue queue = new SendQueueImpl(name, router, registry);
	queue = (SendQueue) attachAspects(queue, SendQueue.class);
	queues.add(queue);
	return queue;
    }
}

package org.cougaar.core.society;

import java.util.ArrayList;
import java.util.Iterator;

public class ReceiveQueueFactory extends AspectFactory
{
    private ArrayList queues = new ArrayList();
    private MessageTransportRegistry registry;

    ReceiveQueueFactory(MessageTransportRegistry registry,
			ArrayList aspects)
    {
	super(aspects);
	this.registry = registry;
    }

    ReceiveQueue getReceiveQueue(String name) {
	Iterator i = queues.iterator();
	while (i.hasNext()) {
	    ReceiveQueue candidate = (ReceiveQueue) i.next();
	    if (candidate != null && candidate.matches(name)) return candidate;
	}
	// No match, make a new one
	ReceiveQueue queue = new ReceiveQueueImpl(name, registry);
	queue = (ReceiveQueue) attachAspects(queue, ReceiveQueue.class);
	queues.add(queue);
	return queue;
    }
}

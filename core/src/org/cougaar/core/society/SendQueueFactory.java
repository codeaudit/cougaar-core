package org.cougaar.core.society;

import java.util.ArrayList;
import java.util.Iterator;

public class SendQueueFactory 
{
    private ArrayList queues = new ArrayList();
    private MessageTransportRegistry registry;

    SendQueueFactory(MessageTransportRegistry registry) {
	this.registry = registry;
    }

    SendQueue getSendQueue(String name, Router router) {
	Iterator i = queues.iterator();
	while (i.hasNext()) {
	    SendQueue candidate = (SendQueue) i.next();
	    if (candidate != null && candidate.matches(name, router)) return candidate;
	}
	// No match, make a new one
	SendQueue queue = new SendQueue(name, router, registry);
	queues.add(queue);
	return queue;
    }
}

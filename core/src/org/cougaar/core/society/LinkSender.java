package org.cougaar.core.society;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class LinkSender implements Runnable
{
    private MessageAddress destination;
    private MessageTransportFactory transportFactory;
    private MessageTransportRegistry registry;
    private Thread thread;
    private DestinationQueue queue;
    private ArrayList destinationLinks;

    protected LinkSender(String name, 
			 MessageAddress destination, 
			 MessageTransportRegistry registry,
			 MessageTransportFactory transportFactory,
			 DestinationQueue queue) 
    {
	this.destination = destination;
	this.queue = queue;
	this.transportFactory = transportFactory;
	this.registry = registry;

	// cache DestinationLinks, per transport
	destinationLinks = new ArrayList();
	getDestinationLinks();

	thread = new Thread(this, name);
	thread.start();
    }


    private void getDestinationLinks() 
    {
	Iterator itr = transportFactory.getTransports().iterator();
	DestinationLink link;
	while (itr.hasNext()) {
	    MessageTransport tpt = (MessageTransport) itr.next();
	    Class tpt_class = tpt.getClass();
	    link = tpt.getDestinationLink(destination);
	    destinationLinks.add(link);
	}
    }

    protected DestinationLink findCheapestLink(Message message) {
	int min_cost = -1;
	DestinationLink cheapest = null;
	Iterator itr = destinationLinks.iterator();
	while (itr.hasNext()) {
	    DestinationLink link = (DestinationLink) itr.next();
	    int cost = link.cost(message);
	    if (cheapest == null || cost < min_cost) {
		cheapest = link;
		min_cost = cost;
	    }
	}
	return cheapest;
    }

    public void run() {
	Message message = null;
	while (true) {
	    synchronized (queue) {
		while (queue.isEmpty()) {
		    try { queue.wait(); } catch (InterruptedException e) {}
		}

		message = (Message) queue.next();
	    }
	    if (message != null) {
		DestinationLink link = findCheapestLink(message);
		link.forwardMessage(message);
	    }
	}
    }


}

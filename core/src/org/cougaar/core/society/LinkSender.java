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
import java.util.Map;
import java.util.Set;

/**
 * A LinkSender is essentially just a thread whose job is to pop
 * messages off a DestinationQueue and forward them on to the
 * "cheapest" transport.  There's one LinkSender per DestinationQueue,
 * and they're created by a LinkSenderFactory.  */
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


    /**
     * Here we ask each transport for DestinationLink.  The links will
     * be used later to find the cheapest transport for any given
     * message. */
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

    /**
     *  Asks each DestinationLink for the cost of sending a given
     *  message via the associated transport.  The DestinationLink
     *  with the lowest cost is returned. */
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

    /**
     * The thread body pops messages off the corresponding
     * DestinationQueue, finds the cheapest DestinationLink for that
     * message, and forwards the message to that link.  */
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

package org.cougaar.core.society;

import org.cougaar.util.CircularQueue;

class DestinationQueue extends CircularQueue
{
    private MessageAddress destination;
    private LinkSender sender;

    DestinationQueue(String name, MessageAddress destination, Router router) {
	this.destination = destination;
	sender = new LinkSender(name, destination, router, this);
    }

    void holdMessage(Message message) {
	synchronized (this) {
	    super.add(message);
	    notify();
	}
    }

    boolean matches(MessageAddress address) {
	return destination.equals(address);
    }

}

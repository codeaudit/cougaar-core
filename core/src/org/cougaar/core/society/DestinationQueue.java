package org.cougaar.core.society;

import org.cougaar.util.CircularQueue;

class DestinationQueue extends CircularQueue
{
    private MessageAddress destination;
    private LinkSender sender;

    DestinationQueue(String name, 
		     MessageAddress destination,
		     LinkSenderFactory factory)
    {
	this.destination = destination;
	sender = factory.getLinkSender(name, destination, this);
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

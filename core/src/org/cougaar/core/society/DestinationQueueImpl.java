package org.cougaar.core.society;

import org.cougaar.util.CircularQueue;

class DestinationQueueImpl extends CircularQueue implements DestinationQueue
{
    private MessageAddress destination;
    private LinkSender sender;

    DestinationQueueImpl(String name, 
		     MessageAddress destination,
		     LinkSenderFactory factory)
    {
	this.destination = destination;
	sender = factory.getLinkSender(name, destination, this);
    }

    public void holdMessage(Message message) {
	synchronized (this) {
	    super.add(message);
	    notify();
	}
    }

    public boolean matches(MessageAddress address) {
	return destination.equals(address);
    }
    

}

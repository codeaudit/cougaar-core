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

import org.cougaar.util.CircularQueue;

/**
 * The default, and for now only, implementation of DestinationQueue.
 * This is a simple passive queue for a particular destination
 * address.  It holds on to a LinkSender and notifies it when messages
 * have been added to the queue.  */
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


    /**
     * Enqueues the given message and notifies the associated
     * LinkSender. */
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

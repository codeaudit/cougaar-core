package org.cougaar.core.society;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class LinkSender implements Runnable
{
    private MessageAddress destination;
    private Router router;
    private MessageTransportRegistry registry;
    private Thread thread;
    private DestinationQueue queue;

    LinkSender(String name, 
	       MessageAddress destination, 
	       Router router, 
	       DestinationQueue queue) 
    {
	this.destination = destination;
	this.queue = queue;
	this.router = router;
	this.registry = router.getRegistry();
	thread = new Thread(this, name);
	thread.start();
    }


    private MessageTransport selectTransport(Message message) {
	String prop = "org.cougaar.message.transportClass";
	String defaultTransportClass = System.getProperty(prop);
	MessageTransportClient client = registry.findLocalClient(destination);
	if (client != null) {
	    return router.getLoopbackTransport();
	} else if (defaultTransportClass == null) {
	    return router.getDefaultTransport();
	} else {
	    return selectPreferredTransport(defaultTransportClass);
	}
    }

    private MessageTransport selectPreferredTransport(String preferredClassname) 
    {
	Class preferredClass = null;
	try {
	    preferredClass = Class.forName(preferredClassname);
	} catch (ClassNotFoundException ex) {
	    ex.printStackTrace();
	    return router.getDefaultTransport();
	}

	// Simple matcher: take the first acceptable one
	Iterator itr = router.getTransports();
	while (itr.hasNext()) {
	    MessageTransport tpt = (MessageTransport) itr.next();
	    Class tpt_class = tpt.getClass();
	    if (preferredClass.isAssignableFrom(tpt_class)) {
		return tpt;
		// return (MessageTransportServerBinder) pair.getValue();
	    }
	}
	return router.getDefaultTransport();
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
		MessageTransport transport = selectTransport(message);
		if (transport != null) {
		    transport.routeMessage(message);
		} else {
		    System.err.println("No transport for " + message);
		}
	    }
	}
    }


}

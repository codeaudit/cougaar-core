package org.cougaar.core.society;

import java.util.Enumeration;

class SendQueue extends MessageQueue
{
    private Router router;
    private MessageTransportRegistry registry;

    SendQueue(String name, Router router, MessageTransportRegistry registry) {
	super(name);
	this.router = router;
	this.registry = registry;
    }

    private void watchingOutgoing(Message message) {
	Enumeration watchers = registry.getWatchers();
	while (watchers.hasMoreElements()) {
	    ((MessageTransportWatcher) watchers.nextElement()).messageSent(message);
	}
    }

    void dispatch(Message message) {
	router.routeMessage(message);
    }


    void sendMessage(Message message) {
	// System.err.print("ZINKY");
	add(message);
	watchingOutgoing(message);
    }

    boolean matches(String name, Router router) {
	return router == this.router && name.equals(getName());
    }

}

package org.cougaar.core.society;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

class MessageTransportRegistry
{
    private String name;
    private Vector watchers;
    private HashMap myClients = new HashMap(89);
    private MessageTransportServerImpl server;
    private MessageTransportFactory transportFactory;

    MessageTransportRegistry(String name, MessageTransportServerImpl server) {
	this.name = name;
	this.server = server;
	watchers = new Vector();
    }

    void setTransportFactory(MessageTransportFactory transportFactory) {
	this.transportFactory = transportFactory;
    }

    Enumeration getWatchers() {
	return watchers.elements();
    }

    void addMessageTransportWatcher(MessageTransportWatcher watcher) {
	watchers.add(watcher);
    }


    String getIdentifier() {
	return name;
    }



    private void addLocalClient(MessageTransportClient client) {
	synchronized (myClients) {
	    try {
		myClients.put(client.getMessageAddress(), client);
	    } catch(Exception e) {}
	}
    }
    private void removeLocalClient(MessageTransportClient client) {
	synchronized (myClients) {
	    try {
		myClients.remove(client.getMessageAddress());
	    } catch (Exception e) {}
	}
    }

    MessageTransportClient findLocalClient(MessageAddress id) {
	synchronized (myClients) {
	    return (MessageTransportClient) myClients.get(id);
	}
    }

    // this is a slow implementation, as it conses a new set each time.
    // Better alternatives surely exist.
    Iterator findLocalMulticastClients(MulticastMessageAddress addr)
    {
	synchronized (myClients) {
	    return new ArrayList(myClients.values()).iterator();
	}
    }

    private void registerClientWithSociety(MessageTransportClient client) {
	// register with each component transport
	Iterator transports = transportFactory.getTransports().iterator();
	while (transports.hasNext()) {
	    MessageTransport mt = (MessageTransport) transports.next();
	    mt.registerClient(client);
	}
    }

    void registerClient(MessageTransportClient client) {
	addLocalClient(client);
	registerClientWithSociety(client);
    }


    boolean addressKnown(MessageAddress address) {
	Iterator transports = transportFactory.getTransports().iterator();
	while (transports.hasNext()) {
	    MessageTransport mt = (MessageTransport) transports.next();
	    if (mt.addressKnown(address)) return true;
	}
	return false;
    }



}

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
import java.util.HashMap;
import java.util.Iterator;

/**
 * The MessageTransportRegistry singleton is a utility instance that
 * helps certain pieces of the message transport subsystem to find one
 * another. */
class MessageTransportRegistry
{
    private String name;
    private HashMap myClients = new HashMap(89);
    private HashMap receiveLinks = new HashMap();
    private MessageTransportServerImpl server;
    private MessageTransportFactory transportFactory;
    private ReceiveLinkFactory receiveLinkFactory;
    private WatcherAspect watcherAspect;

    MessageTransportRegistry(String name, MessageTransportServerImpl server) {
	this.name = name;
	this.server = server;
    }

    void setTransportFactory(MessageTransportFactory transportFactory) {
	this.transportFactory = transportFactory;
    }

    void setReceiveLinkFactory(ReceiveLinkFactory receiveLinkFactory) {
	this.receiveLinkFactory = receiveLinkFactory;
    }


    String getIdentifier() {
	return name;
    }



    void setWatcherManager(WatcherAspect watcherAspect) {
	this.watcherAspect =watcherAspect;
    }
 

    public WatcherAspect getWatcherManager() {
	return watcherAspect;
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





    private void addLocalReceiveLink(ReceiveLink link, MessageAddress key) {
	synchronized (receiveLinks) {
	    try {
		receiveLinks.put(key, link);
	    } catch(Exception e) {}
	}
    }


    ReceiveLink findLocalReceiveLink(MessageAddress id) {
	synchronized (receiveLinks) {
	    return (ReceiveLink) receiveLinks.get(id);
	}
    }

    // this is a slow implementation, as it conses a new set each time.
    // Better alternatives surely exist.
    Iterator findLocalMulticastReceiveLinks(MulticastMessageAddress addr)
    {
	synchronized (receiveLinks) {
	    return new ArrayList(receiveLinks.values()).iterator();
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
	ReceiveLink link = receiveLinkFactory.getReceiveLink(client);
	addLocalReceiveLink(link, client.getMessageAddress());
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

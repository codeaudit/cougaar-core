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

public class MessageTransportServerProxy implements MessageTransportServer
{
    private MessageTransportServerImpl server;

    public MessageTransportServerProxy(MessageTransportServerImpl server) {
	this.server = server;
    }

    public void sendMessage(Message m) {
	server.sendMessage(m);
    }

    public void registerClient(MessageTransportClient client) {
	server.registerClient(client);
    }

    public void addMessageTransportWatcher(MessageTransportWatcher watcher) {
	server.addMessageTransportWatcher(watcher);
    }
   
    public String getIdentifier() {
	return server.getIdentifier();
    }

    public boolean addressKnown(MessageAddress a) {
	return server.addressKnown(a);
    }

}


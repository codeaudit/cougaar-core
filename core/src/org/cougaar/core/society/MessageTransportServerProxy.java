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
    private MessageTransportRegistry registry;
    private SendQueue sendQ;

    public MessageTransportServerProxy(MessageTransportRegistry registry,
				       SendQueue queue) 
    {
	this.sendQ = queue;
	this.registry = registry;
    }

    private boolean checkMessage(Message message) {
	MessageAddress target = message.getTarget();
	// message is ok as long as the target is not empty or null
	return target != null && !target.toString().equals("");
    }




    public void sendMessage(Message m) {
	if (checkMessage(m)) {
	    sendQ.sendMessage(m);
	} else {
	    System.err.println("Warning: MessageTransport.sendMessage of malformed message: "+m);
	    Thread.dumpStack();
	    return;
	}
    }

    public void registerClient(MessageTransportClient client) {
	registry.registerClient(client);
    }
    

    public void addMessageTransportWatcher(MessageTransportWatcher watcher) {
	WatcherAspect mgr = registry.getWatcherManager();
	if (mgr != null)
	    mgr.addMessageTransportWatcher(watcher);
	else
	    System.err.println("Call to addMessageTransportWatcher but no WatcherManager exists");
    }
   
    public String getIdentifier() {
	return registry.getIdentifier();
    }

    public boolean addressKnown(MessageAddress a) {
	return registry.addressKnown(a);
    }

}


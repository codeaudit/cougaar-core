package org.cougaar.core.society;

import java.util.ArrayList;
import java.util.Iterator;

public class WatcherAspect 
    implements MessageTransportAspect
{
    private ArrayList watchers;

    public WatcherAspect() {
	this.watchers = new ArrayList();
    }


    public Object getDelegate(Object delegate, Class iface) {
	if (iface == SendQueue.class) {
	    return new SendQueueDelegate((SendQueue) delegate);
	} else if (iface == ReceiveQueue.class) {
	    return new ReceiveQueueDelegate((ReceiveQueue) delegate);
	} else {
	    return null;
	}
    }


    public void addMessageTransportWatcher(MessageTransportWatcher watcher) {
	watchers.add(watcher);
    }


    private void notifyWatchersOfSend(Message message) {
	Iterator itr = watchers.iterator();
	while (itr.hasNext()) {
	    ((MessageTransportWatcher) itr.next()).messageSent(message);
	}
    }

    private void notifyWatchersOfReceive(Message m) {
	Iterator itr = watchers.iterator();
	while ( itr.hasNext() ) {
	    ((MessageTransportWatcher)itr.next()).messageReceived(m);
	}
    }


    public class SendQueueDelegate implements SendQueue
    {
	private SendQueue server;
	
	public SendQueueDelegate (SendQueue server)
	{
	    this.server = server;
	}
	
	public void sendMessage(Message message) {
	    server.sendMessage(message);
	    notifyWatchersOfSend(message);
	}
	
	public boolean matches(String name){
	    return server.matches(name);
	}
    }


    public class ReceiveQueueDelegate implements ReceiveQueue
    {
	private ReceiveQueue server;
	
	public ReceiveQueueDelegate (ReceiveQueue server)
	{
	    this.server = server;
	}
	
	public void deliverMessage(Message message) {
	    server.deliverMessage(message);
	    notifyWatchersOfReceive(message);
	}
	
	public boolean matches(String name) {
	    return server.matches(name);
	}
    }
}


    

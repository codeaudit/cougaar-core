package org.cougaar.core.society;

import java.util.Enumeration;
import java.util.Iterator;

public class ReceiveQueue extends MessageQueue
{
    private MessageTransportRegistry registry;

    ReceiveQueue(String name, MessageTransportRegistry registry) {
	super(name);
	this.registry = registry;
    }

    protected void watchingIncoming(Message m) {
	Enumeration e = registry.getWatchers();
	while ( e.hasMoreElements() ) {
	    ((MessageTransportWatcher)e.nextElement()).messageReceived(m);
	}
    }



    private void sendMessageToClient(Message message) {
	if (message == null) return;
	try {
	    MessageAddress addr = message.getTarget();
	    if (addr instanceof MulticastMessageAddress) {
		Iterator i = registry.findLocalMulticastClients((MulticastMessageAddress)addr); 
		while (i.hasNext()) {
		    MessageTransportClient client = (MessageTransportClient) i.next();
		    client.receiveMessage(message);
		}
	    } else {
		MessageTransportClient client = registry.findLocalClient(addr);
		if (client != null) {
		    client.receiveMessage(message);
		} else {
		    throw new RuntimeException("Misdelivered message "
					       + message +
					       " sent to "+this);
		}
	    }
	} catch (Exception e) {
		e.printStackTrace();
	}
    }

    void dispatch(Message message) {
	sendMessageToClient(message);
    }


    public void deliverMessage(Message message) {
	// System.err.print("ZONKY");
	add(message);
	watchingIncoming(message);
    }

    boolean matches(String name) {
	return name.equals(getName());
    }

}

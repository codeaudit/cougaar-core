package org.cougaar.core.society;

import java.util.Iterator;

public class ReceiveQueueImpl extends MessageQueue implements ReceiveQueue
{
    private MessageTransportRegistry registry;

    ReceiveQueueImpl(String name, MessageTransportRegistry registry) {
	super(name);
	this.registry = registry;
    }




    private void sendMessageToClient(Message message) {
	if (message == null) return;
	try {
	    MessageAddress addr = message.getTarget();
	    if (addr instanceof MulticastMessageAddress) {
		Iterator i = registry.findLocalMulticastReceiveLinks((MulticastMessageAddress)addr); 
		while (i.hasNext()) {
		    ReceiveLink link = (ReceiveLink) i.next();
		    link.deliverMessage(message);
		}
	    } else {
		ReceiveLink link = registry.findLocalReceiveLink(addr);
		if (link != null) {
		    link.deliverMessage(message);
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
	add(message);
    }

    public boolean matches(String name) {
	return name.equals(getName());
    }

}

package org.cougaar.core.society;

// Gives a hook to hang per Client Receive message processing
public class ReceiveLinkImpl implements ReceiveLink
{
    MessageTransportClient client;

    ReceiveLinkImpl( MessageTransportClient client) {
	this.client = client;
    }

    public void deliverMessage(Message message)
    {
	client.receiveMessage(message);
    }

}

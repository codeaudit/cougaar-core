package org.cougaar.core.society;

class LoopbackMessageTransport extends MessageTransport
{

    public void routeMessage(Message message) {
	System.out.print('!');
	getBinder().deliverMessage(message);
    }

    public void registerClient(MessageTransportClient client) {
	// Does nothing because the Database of local clients is held
	// by MessageTransportServerImpl
    }

    public boolean addressKnown(MessageAddress address) {
	// true iff the address is local
	return getBinder().isLocalAddress(address);
    }
   

}

package org.cougaar.core.society;

class LoopbackMessageTransport 
    extends MessageTransport
    implements DestinationLink
{

    public DestinationLink getDestinationLink(MessageAddress address) {
	return this;
    }

    // DestinationLink interface
    public int cost(Message msg) {
	return 
	    registry.findLocalClient(msg.getTarget()) != null ?
	    0 :
	    Integer.MAX_VALUE;
    }
	


    public void forwardMessage(Message message) {
	recvQ.deliverMessage(message);
    }

    public void registerClient(MessageTransportClient client) {
	// Does nothing because the Database of local clients is held
	// by MessageTransportServerImpl
    }

    public boolean addressKnown(MessageAddress address) {
	// true iff the address is local
	return registry.findLocalClient(address) != null;
    }
   

}

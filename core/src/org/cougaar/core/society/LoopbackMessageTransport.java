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

/**
 * This transpory handles all intra-node message traffic.  It can act
 * as its own DestinationLink, since this transport only sees traffic
 * for one destination.  The cost function is minimal (0) for local
 * traffic, and maximal (Integer.MAX_VALUE) for any other traffic. */
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

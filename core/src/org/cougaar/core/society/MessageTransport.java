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


public abstract class MessageTransport 
{
    protected ReceiveQueue recvQ;
    protected MessageTransportRegistry registry;
    protected NameSupport nameSupport;

    // abstract public void routeMessage(Message message);
    abstract DestinationLink getDestinationLink(MessageAddress destination);
    abstract public void registerClient(MessageTransportClient client);
    abstract public boolean addressKnown(MessageAddress address);


    public void setRegistry(MessageTransportRegistry registry) {
	this.registry = registry;
    }


    public void setNameSupport(NameSupport nameSupport) {
	this.nameSupport = nameSupport;
    }

    public void setRecvQ(ReceiveQueue recvQ) {
	this.recvQ = recvQ;
    }


    

}

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
 * The parent class of all MessageTransports.  Instantiable subclasses
 * are required to do two things: they must be able to say whether or
 * not they can deal with any particular addresss (addressKnown), and
 * they must be able to supply a DestinationLink instance for any
 * address they can deal with (getDestinationLin).  They will also be
 * given the opportunity to "register" clients, if they have any
 * interest in doing so (for instance, an RMI transport might use this
 * as an opportunity to publish an MTImpl for the client on a
 * nameserver). */
public abstract class MessageTransport 
{
    protected ReceiveQueue recvQ;
    protected MessageTransportRegistry registry;
    protected NameSupport nameSupport;

    // abstract public void routeMessage(Message message);
    abstract public DestinationLink getDestinationLink(MessageAddress destination);
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
